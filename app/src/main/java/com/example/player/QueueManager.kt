package com.example.player

import androidx.media3.common.Player
import com.example.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages playlist state, current index, shuffle, repeat, and track navigation.
 * Completely decoupled from audio playback hardware and UI.
 *
 * Not thread-safe: all calls are expected on the main thread, which is where
 * ExoPlayer delivers its listeners and where the UI drives transport controls.
 */
class QueueManager {

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    var currentTrackIndex: Int = 0
        private set

    /**
     * Index reserved by [peekNextTrack] and consumed by [getNextTrack].
     *
     * The crossfade path peeks a track to decide whether to start a
     * transition and only commits once the fade-out completes. Without a
     * reservation the two calls each pick independently, so under shuffle the
     * previewed track and the played track were different. Reserving makes the
     * pair a single decision.
     */
    private var pendingNextIndex: Int? = null

    /**
     * Indices already played in the current pass. Only consulted when shuffle
     * is on, and only to stop a `REPEAT_OFF` shuffle once the queue is
     * exhausted and to avoid immediate repeats within a repeating pass.
     */
    private val playedInShufflePass = mutableSetOf<Int>()

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0): Track? {
        _playlist.value = tracks
        currentTrackIndex = if (tracks.isNotEmpty()) startIndex.coerceIn(0, tracks.lastIndex) else 0
        resetNavigation()
        return tracks.getOrNull(currentTrackIndex)
    }

    fun setShuffleMode(enabled: Boolean) {
        if (_shuffleModeEnabled.value == enabled) return
        _shuffleModeEnabled.value = enabled
        resetNavigation()
    }

    fun toggleShuffle(): Boolean {
        setShuffleMode(!_shuffleModeEnabled.value)
        return _shuffleModeEnabled.value
    }

    fun setRepeatMode(mode: Int) {
        val sanitized = when (mode) {
            Player.REPEAT_MODE_OFF,
            Player.REPEAT_MODE_ALL,
            Player.REPEAT_MODE_ONE -> mode
            else -> Player.REPEAT_MODE_ALL
        }
        if (sanitized == _repeatMode.value) return
        _repeatMode.value = sanitized
        resetNavigation()
    }

    /** Cycles `OFF -> ALL -> ONE -> OFF` so no mode is unreachable. */
    fun toggleRepeat(): Int {
        _repeatMode.value = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        resetNavigation()
        return _repeatMode.value
    }

    fun setCurrentTrack(track: Track) {
        val idx = _playlist.value.indexOfFirst { it.id == track.id }
        if (idx != -1) {
            currentTrackIndex = idx
            playedInShufflePass += idx
            pendingNextIndex = null
        }
    }

    /** Returns the track a subsequent [getNextTrack] would return, without advancing. */
    fun peekNextTrack(currentTrack: Track?): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        val idx = resolveCurrentIndex(currentTrack)
        if (idx == -1) return null

        val next = pendingNextIndex ?: computeNextIndex(idx)?.also { pendingNextIndex = it }
        return next?.let { list.getOrNull(it) }
    }

    fun getNextTrack(currentTrack: Track?): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        val idx = resolveCurrentIndex(currentTrack)
        if (idx == -1) return null

        val next = pendingNextIndex ?: computeNextIndex(idx)
        pendingNextIndex = null

        val track = next?.let { list.getOrNull(it) } ?: return null
        currentTrackIndex = next
        playedInShufflePass += next
        return track
    }

    /**
     * Steps backwards through queue order.
     *
     * Returns `null` at the start of the queue unless repeat is `ALL`, rather
     * than silently returning the current track. Shuffle "previous" is still
     * queue-order rather than playback-history — that needs a history stack
     * and is tracked separately.
     */
    fun getPreviousTrack(currentTrack: Track?): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        val idx = resolveCurrentIndex(currentTrack)
        if (idx == -1) return null

        val prev = idx - 1
        val target = when {
            prev >= 0 -> prev
            _repeatMode.value == Player.REPEAT_MODE_ALL -> list.lastIndex
            else -> return null
        }

        val track = list.getOrNull(target) ?: return null
        currentTrackIndex = target
        playedInShufflePass += target
        return track
    }

    private fun resetNavigation() {
        pendingNextIndex = null
        playedInShufflePass.clear()
        currentTrackIndex.takeIf { it in _playlist.value.indices }?.let { playedInShufflePass += it }
    }

    /**
     * Prefers the tracked index and falls back to locating [currentTrack] by
     * id, so a stale index cannot silently navigate from the wrong position.
     */
    private fun resolveCurrentIndex(currentTrack: Track?): Int {
        val list = _playlist.value
        if (list.isEmpty()) return -1
        if (currentTrackIndex in list.indices) return currentTrackIndex
        return currentTrack?.let { track -> list.indexOfFirst { it.id == track.id } } ?: -1
    }

    private fun computeNextIndex(currentIdx: Int): Int? {
        val list = _playlist.value
        if (list.isEmpty()) return null

        if (_repeatMode.value == Player.REPEAT_MODE_ONE) return currentIdx

        return if (_shuffleModeEnabled.value) {
            nextShuffledIndex(currentIdx)
        } else {
            val next = currentIdx + 1
            when {
                next < list.size -> next
                _repeatMode.value == Player.REPEAT_MODE_ALL -> 0
                // REPEAT_OFF: the queue ends rather than wrapping.
                else -> null
            }
        }
    }

    private fun nextShuffledIndex(currentIdx: Int): Int? {
        val indices = _playlist.value.indices
        if (indices.count() <= 1) return currentIdx

        val repeats = _repeatMode.value == Player.REPEAT_MODE_ALL
        val unplayed = indices.filter { it != currentIdx && it !in playedInShufflePass }

        if (unplayed.isNotEmpty()) return unplayed.random()

        // A non-repeating shuffle has nothing left to play.
        if (!repeats) return null

        // The pass is complete; start it over, but never back onto the same
        // track twice in a row when there is any other choice.
        val withoutImmediateRepeat = indices.filter { it != currentIdx }
        return withoutImmediateRepeat.random()
    }
}
