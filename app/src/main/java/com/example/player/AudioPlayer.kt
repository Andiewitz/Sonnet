package com.example.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.example.data.datastore.UserPreferencesDataStore
import com.example.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayer(
    private val context: Context,
    val userPreferencesDataStore: UserPreferencesDataStore
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    val equalizerManager = EqualizerManager(context.applicationContext, userPreferencesDataStore, scope)

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var isNormalizeVolume = false

    // Single ExoPlayer instance guarantees two songs can NEVER play simultaneously
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            false // handle focus manually
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    val activePlayer: ExoPlayer get() = player
    var onActivePlayerChanged: ((Player) -> Unit)? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var playOnFocusGain = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                playOnFocusGain = false
                player.pause()
                _isPlaying.value = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                playOnFocusGain = player.isPlaying
                player.pause()
                _isPlaying.value = false
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (playOnFocusGain) {
                    player.play()
                    playOnFocusGain = false
                    _isPlaying.value = true
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(true)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(3)
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    private var progressJob: Job? = null
    private var transitionJob: Job? = null
    private var isTransitioning = false

    private var playlist: List<Track> = emptyList()
    private var currentTrackIndex: Int = 0
    var onTrackPlayed: ((Long) -> Unit)? = null

    init {
        setupPlayer()

        // Restore saved preferences
        scope.launch {
            try {
                val savedShuffle = userPreferencesDataStore.shuffleMode.first()
                player.shuffleModeEnabled = savedShuffle
                _shuffleModeEnabled.value = savedShuffle

                val savedRepeat = userPreferencesDataStore.repeatMode.first()
                val effectiveRepeat = if (savedRepeat == Player.REPEAT_MODE_ONE) {
                    Player.REPEAT_MODE_ONE
                } else {
                    Player.REPEAT_MODE_ALL
                }
                player.repeatMode = effectiveRepeat
                _repeatMode.value = effectiveRepeat

                val savedNormalize = userPreferencesDataStore.isNormalizeVolume.first()
                setNormalizeVolume(savedNormalize)

                val savedCrossfade = userPreferencesDataStore.crossfadeEnabled.first()
                _crossfadeEnabled.value = savedCrossfade

                val savedCrossfadeDuration = userPreferencesDataStore.crossfadeDurationSeconds.first()
                _crossfadeDurationSeconds.value = savedCrossfadeDuration
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error loading saved preferences: ${e.message}")
            }
        }
    }

    private fun setupPlayer() {
        player.repeatMode = _repeatMode.value
        player.shuffleModeEnabled = _shuffleModeEnabled.value

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    equalizerManager.bindAudioSession(audioSessionId)
                    bindLoudnessEnhancer(audioSessionId)
                }
            }
        })

        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                super.onTracksChanged(tracks)
                val currentSid = player.audioSessionId
                if (currentSid != C.AUDIO_SESSION_ID_UNSET && currentSid > 0) {
                    equalizerManager.bindAudioSession(currentSid)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                val currentSid = player.audioSessionId
                if (currentSid != C.AUDIO_SESSION_ID_UNSET && currentSid > 0) {
                    equalizerManager.bindAudioSession(currentSid)
                }
                if (playbackState == Player.STATE_ENDED) {
                    if (!isTransitioning) {
                        skipToNext(useTransition = false)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    if (!requestAudioFocus()) {
                        player.pause()
                    }
                } else {
                    if (!playOnFocusGain && !isTransitioning) {
                        abandonAudioFocus()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
                        equalizerManager.bindAudioSession(sessionId)
                        bindLoudnessEnhancer(sessionId)
                    }
                    startProgressTracker()
                } else if (!isTransitioning) {
                    stopProgressTracker()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleModeEnabled
                scope.launch {
                    try {
                        userPreferencesDataStore.setShuffleMode(shuffleModeEnabled)
                    } catch (e: Exception) {
                        Log.e("AudioPlayer", "Error saving shuffle mode: ${e.message}")
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                if (repeatMode == Player.REPEAT_MODE_OFF) {
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    return
                }
                _repeatMode.value = repeatMode
                scope.launch {
                    try {
                        userPreferencesDataStore.setRepeatMode(repeatMode)
                    } catch (e: Exception) {
                        Log.e("AudioPlayer", "Error saving repeat mode: ${e.message}")
                    }
                }
            }
        })
    }

    private fun buildMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(android.net.Uri.parse(track.albumArtUri ?: ""))
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun setNormalizeVolume(enabled: Boolean) {
        isNormalizeVolume = enabled
        try {
            loudnessEnhancer?.enabled = enabled
            if (enabled) {
                loudnessEnhancer?.setTargetGain(0)
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error setting normalize volume: ${e.message}")
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        // Gapless naturally handled when transitions are disabled
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        if (!enabled) {
            cancelTransition()
            player.volume = 1.0f
        }
        scope.launch {
            try {
                userPreferencesDataStore.setCrossfadeEnabled(enabled)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving crossfade enabled: ${e.message}")
            }
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 12)
        _crossfadeDurationSeconds.value = clamped
        scope.launch {
            try {
                userPreferencesDataStore.setCrossfadeDurationSeconds(clamped)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving crossfade duration: ${e.message}")
            }
        }
    }

    private fun bindLoudnessEnhancer(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = isNormalizeVolume
                if (isNormalizeVolume) {
                    setTargetGain(0)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error binding LoudnessEnhancer: ${e.message}")
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        cancelTransition()
        playlist = tracks
        if (tracks.isEmpty()) {
            player.stop()
            player.clearMediaItems()
            _currentTrack.value = null
            _isPlaying.value = false
            _currentPosition.value = 0L
            return
        }
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
        currentTrackIndex = safeIndex
        startPlayback(tracks[safeIndex], fadeIn = false)
    }

    fun playTrack(track: Track) {
        cancelTransition()
        val index = playlist.indexOfFirst { it.id == track.id }
        if (index != -1) {
            currentTrackIndex = index
        } else {
            playlist = listOf(track)
            currentTrackIndex = 0
        }
        startPlayback(track, fadeIn = true)
    }

    private fun startPlayback(track: Track, fadeIn: Boolean) {
        cancelTransition()

        // If something was already playing, fade out swiftly (60ms) to avoid audio clipping
        if (player.isPlaying && fadeIn) {
            transitionJob?.cancel()
            transitionJob = scope.launch {
                // Quick 60ms micro-fade down
                player.volume = 0.3f
                delay(30L)
                player.volume = 0.0f
                executeLoadAndPlay(track, fadeIn = true)
            }
        } else {
            executeLoadAndPlay(track, fadeIn = fadeIn)
        }
    }

    private fun executeLoadAndPlay(track: Track, fadeIn: Boolean) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(buildMediaItem(track))
        player.prepare()
        player.seekTo(0L)
        requestAudioFocus()

        if (fadeIn && _crossfadeEnabled.value) {
            player.volume = 0.0f
            player.play()
            _currentTrack.value = track
            _currentPosition.value = 0L
            _isPlaying.value = true
            onTrackPlayed?.invoke(track.id)
            startPlaybackService()

            // Smooth fade-in curve from 0.0f to 1.0f over transition duration (or 1s for manual clicks)
            val fadeDuration = (_crossfadeDurationSeconds.value * 600L).coerceIn(400L, 2000L)
            transitionJob?.cancel()
            transitionJob = scope.launch {
                val start = System.currentTimeMillis()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - start
                    val fraction = (elapsed.toFloat() / fadeDuration).coerceIn(0f, 1f)
                    player.volume = fraction
                    if (fraction >= 1.0f) break
                    delay(25L)
                }
                player.volume = 1.0f
                isTransitioning = false
            }
        } else {
            player.volume = 1.0f
            player.play()
            _currentTrack.value = track
            _currentPosition.value = 0L
            _isPlaying.value = true
            onTrackPlayed?.invoke(track.id)
            startPlaybackService()
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error starting PlaybackService: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            // Smooth 50ms micro-fade out before pausing to eliminate speaker pop
            transitionJob?.cancel()
            transitionJob = scope.launch {
                player.volume = 0.4f
                delay(25L)
                player.volume = 0.0f
                player.pause()
                player.volume = 1.0f
                _isPlaying.value = false
            }
        } else {
            transitionJob?.cancel()
            requestAudioFocus()
            player.volume = 0.3f
            player.play()
            _isPlaying.value = true
            scope.launch {
                delay(25L)
                player.volume = 0.7f
                delay(25L)
                player.volume = 1.0f
            }
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleModeEnabled.value
        player.shuffleModeEnabled = newShuffle
        _shuffleModeEnabled.value = newShuffle
        scope.launch {
            try {
                userPreferencesDataStore.setShuffleMode(newShuffle)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving shuffle mode: ${e.message}")
            }
        }
    }

    fun toggleRepeat() {
        val nextMode = if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_ALL
        }
        player.repeatMode = nextMode
        _repeatMode.value = nextMode
        scope.launch {
            try {
                userPreferencesDataStore.setRepeatMode(nextMode)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving repeat mode: ${e.message}")
            }
        }
    }

    fun getNextTrack(): Track? {
        if (playlist.isEmpty()) return null
        if (_repeatMode.value == Player.REPEAT_MODE_ONE) {
            return _currentTrack.value
        }
        val currentIdx = if (currentTrackIndex in playlist.indices) {
            currentTrackIndex
        } else {
            playlist.indexOfFirst { it.id == _currentTrack.value?.id }
        }
        if (currentIdx == -1) return playlist.firstOrNull()

        if (_shuffleModeEnabled.value) {
            if (playlist.size > 1) {
                val otherIndices = playlist.indices.filter { it != currentIdx }
                val nextIdx = otherIndices.random()
                return playlist[nextIdx]
            }
            return playlist[currentIdx]
        } else {
            val nextIdx = currentIdx + 1
            if (nextIdx < playlist.size) {
                return playlist[nextIdx]
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                return playlist[0]
            }
        }
        return null
    }

    fun skipToNext(useTransition: Boolean = true) {
        cancelTransition()
        val next = getNextTrack()
        if (next != null) {
            val nextIdx = playlist.indexOfFirst { it.id == next.id }
            if (nextIdx != -1) currentTrackIndex = nextIdx
            startPlayback(next, fadeIn = useTransition && _crossfadeEnabled.value)
        } else if (playlist.isNotEmpty()) {
            currentTrackIndex = 0
            startPlayback(playlist[0], fadeIn = useTransition && _crossfadeEnabled.value)
        }
    }

    fun skipToPrevious() {
        cancelTransition()
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            _currentPosition.value = 0L
            return
        }
        if (playlist.isEmpty()) return
        val currentIdx = if (currentTrackIndex in playlist.indices) {
            currentTrackIndex
        } else {
            playlist.indexOfFirst { it.id == _currentTrack.value?.id }.coerceAtLeast(0)
        }
        val prevIdx = if (currentIdx > 0) currentIdx - 1 else playlist.size - 1
        currentTrackIndex = prevIdx
        startPlayback(playlist[prevIdx], fadeIn = _crossfadeEnabled.value)
    }

    fun seekTo(positionMs: Long) {
        cancelTransition()
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
        player.volume = 1.0f
    }

    private fun cancelTransition() {
        transitionJob?.cancel()
        transitionJob = null
        isTransitioning = false
    }

    // Natural sequential transition: Fade out outgoing song to silence, THEN switch to next and fade in
    private fun checkAndTriggerFadeOut(currentPos: Long, playerDuration: Long) {
        if (!_crossfadeEnabled.value) return
        if (isTransitioning) return
        if (!player.isPlaying) return
        if (playerDuration <= 6000L) return

        val transitionSec = _crossfadeDurationSeconds.value
        if (transitionSec <= 0) return

        val transitionMs = (transitionSec * 1000L).coerceAtMost((playerDuration * 0.35f).toLong())
        if (transitionMs < 600L) return

        val remainingMs = playerDuration - currentPos

        // When remaining time enters the fade-out window:
        if (remainingMs in 1..transitionMs) {
            val nextTrack = getNextTrack()
            if (nextTrack != null) {
                startSequentialTransition(nextTrack, remainingMs)
            }
        }
    }

    private fun startSequentialTransition(nextTrack: Track, fadeOutDurationMs: Long) {
        if (isTransitioning) return
        isTransitioning = true

        transitionJob?.cancel()
        transitionJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val totalMs = fadeOutDurationMs.coerceAtLeast(500L).toFloat()

            // Step 1: Smoothly fade out the CURRENT song to 0.0 volume
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed / totalMs).coerceIn(0f, 1f)
                player.volume = (1f - fraction).coerceIn(0f, 1f)

                if (fraction >= 1.0f) {
                    break
                }
                delay(25L)
            }

            // Step 2: Now that volume is completely at 0 (silence), switch to the NEXT track
            player.volume = 0.0f
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(buildMediaItem(nextTrack))
            player.prepare()
            player.seekTo(0L)
            player.play()

            // Update UI metadata and index
            _currentTrack.value = nextTrack
            _currentPosition.value = 0L
            val nextIdx = playlist.indexOfFirst { it.id == nextTrack.id }
            if (nextIdx != -1) {
                currentTrackIndex = nextIdx
            }
            onTrackPlayed?.invoke(nextTrack.id)

            // Step 3: Smoothly fade in the NEXT song from 0.0 to 1.0 volume
            val fadeInStartTime = System.currentTimeMillis()
            val fadeInTotalMs = (_crossfadeDurationSeconds.value * 800L).coerceIn(500L, 3000L).toFloat()

            while (isActive) {
                val elapsed = System.currentTimeMillis() - fadeInStartTime
                val fraction = (elapsed / fadeInTotalMs).coerceIn(0f, 1f)
                player.volume = fraction

                if (fraction >= 1.0f) {
                    break
                }
                delay(25L)
            }

            player.volume = 1.0f
            isTransitioning = false
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = player.currentPosition
                val dur = if (player.duration > 0L) player.duration else (_currentTrack.value?.durationMs ?: 0L)

                _currentPosition.value = pos
                checkAndTriggerFadeOut(pos, dur)

                delay(100L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        cancelTransition()
        progressJob?.cancel()
        player.release()
        equalizerManager.release()
        loudnessEnhancer?.release()
        abandonAudioFocus()
    }
}
