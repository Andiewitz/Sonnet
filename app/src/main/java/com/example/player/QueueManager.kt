package com.example.player

import androidx.media3.common.Player
import com.example.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages playlist state, current index, shuffle, repeat, and track navigation.
 * Completely decoupled from audio playback hardware and UI.
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

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0): Track? {
        _playlist.value = tracks
        currentTrackIndex = if (tracks.isNotEmpty()) startIndex.coerceIn(0, tracks.lastIndex) else 0
        return tracks.getOrNull(currentTrackIndex)
    }

    fun setShuffleMode(enabled: Boolean) {
        _shuffleModeEnabled.value = enabled
    }

    fun toggleShuffle(): Boolean {
        val next = !_shuffleModeEnabled.value
        _shuffleModeEnabled.value = next
        return next
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = if (mode == Player.REPEAT_MODE_ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_ALL
        }
    }

    fun toggleRepeat(): Int {
        val next = if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_ALL
        }
        _repeatMode.value = next
        return next
    }

    fun setCurrentTrack(track: Track) {
        val idx = _playlist.value.indexOfFirst { it.id == track.id }
        if (idx != -1) {
            currentTrackIndex = idx
        }
    }

    fun peekNextTrack(currentTrack: Track?): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null

        if (_repeatMode.value == Player.REPEAT_MODE_ONE) {
            return currentTrack ?: list.getOrNull(currentTrackIndex)
        }

        val currentIdx = if (currentTrackIndex in list.indices) {
            currentTrackIndex
        } else {
            list.indexOfFirst { it.id == currentTrack?.id }
        }

        if (currentIdx == -1) return list.firstOrNull()

        if (_shuffleModeEnabled.value) {
            if (list.size > 1) {
                val otherIndices = list.indices.filter { it != currentIdx }
                val nextIdx = otherIndices.firstOrNull() ?: currentIdx
                return list[nextIdx]
            }
            return list[currentIdx]
        } else {
            val nextIdx = currentIdx + 1
            if (nextIdx < list.size) {
                return list[nextIdx]
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                return list.firstOrNull()
            }
        }
        return null
    }

    fun getNextTrack(currentTrack: Track?): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null

        if (_repeatMode.value == Player.REPEAT_MODE_ONE) {
            return currentTrack ?: list.getOrNull(currentTrackIndex)
        }

        val currentIdx = if (currentTrackIndex in list.indices) {
            currentTrackIndex
        } else {
            list.indexOfFirst { it.id == currentTrack?.id }
        }

        if (currentIdx == -1) return list.firstOrNull()

        if (_shuffleModeEnabled.value) {
            if (list.size > 1) {
                val otherIndices = list.indices.filter { it != currentIdx }
                val nextIdx = otherIndices.random()
                currentTrackIndex = nextIdx
                return list[nextIdx]
            }
            return list[currentIdx]
        } else {
            val nextIdx = currentIdx + 1
            if (nextIdx < list.size) {
                currentTrackIndex = nextIdx
                return list[nextIdx]
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                currentTrackIndex = 0
                return list[0]
            }
        }
        return null
    }

    fun getPreviousTrack(currentTrack: Track?): Track? {
        val list = _playlist.value
        if (list.isEmpty()) return null

        val currentIdx = if (currentTrackIndex in list.indices) {
            currentTrackIndex
        } else {
            list.indexOfFirst { it.id == currentTrack?.id }
        }

        if (currentIdx == -1) return list.firstOrNull()

        val prevIdx = currentIdx - 1
        if (prevIdx >= 0) {
            currentTrackIndex = prevIdx
            return list[prevIdx]
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            val lastIdx = list.lastIndex
            currentTrackIndex = lastIdx
            return list[lastIdx]
        }
        return list.firstOrNull()
    }
}
