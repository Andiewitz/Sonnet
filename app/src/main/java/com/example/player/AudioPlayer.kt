package com.example.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.example.data.datastore.UserPreferencesDataStore
import com.example.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    val equalizerManager = EqualizerManager(context.applicationContext, userPreferencesDataStore, scope)

    val queueManager = QueueManager()

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var isNormalizeVolume = false

    // Single ExoPlayer instance guarantees two songs can never play simultaneously
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            false // handle focus manually
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    val activePlayer: ExoPlayer get() = player
    var onActivePlayerChanged: ((Player) -> Unit)? = null

    val fadeController = FadeController(
        playerProvider = { player },
        scope = scope
    )

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var playOnFocusGain = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        mainHandler.post {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    playOnFocusGain = false
                    pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    playOnFocusGain = player.isPlaying
                    pause()
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (playOnFocusGain) {
                        playOnFocusGain = false
                        play()
                    }
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

    val shuffleModeEnabled: StateFlow<Boolean> = queueManager.shuffleModeEnabled
    val repeatMode: StateFlow<Int> = queueManager.repeatMode

    private val _crossfadeEnabled = MutableStateFlow(true)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(3)
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    private var progressJob: Job? = null
    var onTrackPlayed: ((Long) -> Unit)? = null

    init {
        setupPlayer()

        // Restore saved preferences
        scope.launch {
            try {
                val savedShuffle = userPreferencesDataStore.shuffleMode.first()
                queueManager.setShuffleMode(savedShuffle)
                player.shuffleModeEnabled = savedShuffle

                val savedRepeat = userPreferencesDataStore.repeatMode.first()
                queueManager.setRepeatMode(savedRepeat)
                player.repeatMode = savedRepeat

                val savedCrossfade = userPreferencesDataStore.crossfadeEnabled.first()
                _crossfadeEnabled.value = savedCrossfade

                val savedCrossfadeDuration = userPreferencesDataStore.crossfadeDurationSeconds.first()
                _crossfadeDurationSeconds.value = savedCrossfadeDuration

                val savedNormalize = userPreferencesDataStore.isNormalizeVolume.first()
                isNormalizeVolume = savedNormalize
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error loading saved preferences: ${e.message}")
            }
        }
    }

    private fun setupPlayer() {
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    equalizerManager.bindAudioSession(audioSessionId)
                    setupLoudnessEnhancer(audioSessionId)
                }
            }
        })

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (!fadeController.isTransitioning.value) {
                        skipToNext(useTransition = true)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    if (!requestAudioFocus()) {
                        player.pause()
                    }
                } else {
                    if (!playOnFocusGain && !fadeController.isTransitioning.value) {
                        abandonAudioFocus()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPlaybackService()
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("AudioPlayer", "Playback error: ${error.message}", error)
                fadeController.cancelAndRestore()
                _isPlaying.value = false
                stopProgressTracker()
            }
        })
    }

    private fun setupLoudnessEnhancer(audioSessionId: Int) {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(if (isNormalizeVolume) 800 else 0)
                enabled = isNormalizeVolume
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Could not initialize LoudnessEnhancer: ${e.message}")
        }
    }

    fun setNormalizeVolume(enabled: Boolean) {
        isNormalizeVolume = enabled
        try {
            loudnessEnhancer?.let {
                it.setTargetGain(if (enabled) 800 else 0)
                it.enabled = enabled
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error setting normalize volume: ${e.message}")
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        // Gapless playback is native to ExoPlayer pipeline
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        scope.launch {
            try {
                userPreferencesDataStore.setCrossfadeEnabled(enabled)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving crossfade: ${e.message}")
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

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        val selected = queueManager.setPlaylist(tracks, startIndex)
        if (selected != null) {
            playTrack(selected)
        }
    }

    fun playTrack(track: Track) {
        queueManager.setCurrentTrack(track)
        loadAndStart(track, applyFadeIn = _crossfadeEnabled.value)
    }

    private fun loadAndStart(track: Track, applyFadeIn: Boolean, isTransitionSwitch: Boolean = false) {
        if (!isTransitionSwitch) {
            fadeController.cancelAndRestore()
        }
        requestAudioFocus()

        try {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(buildMediaItem(track))
            player.prepare()
            player.seekTo(0L)
            if (isTransitionSwitch) {
                player.volume = 0f
            }
            player.play()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error loading media: ${e.message}", e)
        }

        _currentTrack.value = track
        _currentPosition.value = 0L
        onTrackPlayed?.invoke(track.id)
        startPlaybackService()

        if (applyFadeIn && _crossfadeEnabled.value && !isTransitionSwitch) {
            val durationMs = (_crossfadeDurationSeconds.value * 1000L).coerceIn(400L, 12000L)
            fadeController.startFadeIn(durationMs)
        }
    }

    fun play() {
        if (player.mediaItemCount == 0) {
            val track = queueManager.getNextTrack(null) ?: return
            playTrack(track)
            return
        }
        fadeController.cancelAndRestore()
        if (requestAudioFocus()) {
            player.play()
        }
    }

    fun pause() {
        fadeController.cancelAndRestore()
        player.pause()
        abandonAudioFocus()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun toggleShuffle() {
        val newShuffle = queueManager.toggleShuffle()
        player.shuffleModeEnabled = newShuffle
        scope.launch {
            try {
                userPreferencesDataStore.setShuffleMode(newShuffle)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving shuffle mode: ${e.message}")
            }
        }
    }

    fun toggleRepeat() {
        val nextMode = queueManager.toggleRepeat()
        player.repeatMode = nextMode
        scope.launch {
            try {
                userPreferencesDataStore.setRepeatMode(nextMode)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error saving repeat mode: ${e.message}")
            }
        }
    }

    fun skipToNext(useTransition: Boolean = true) {
        val nextTrack = queueManager.getNextTrack(_currentTrack.value) ?: return

        if (useTransition && _crossfadeEnabled.value && player.isPlaying) {
            val transitionMs = (_crossfadeDurationSeconds.value * 1000L).coerceIn(400L, 12000L)
            fadeController.transitionBetweenTracks(transitionMs) {
                loadAndStart(nextTrack, applyFadeIn = false, isTransitionSwitch = true)
            }
        } else {
            loadAndStart(nextTrack, applyFadeIn = useTransition && _crossfadeEnabled.value)
        }
    }

    fun skipToPrevious() {
        if (player.currentPosition > 3000L) {
            seekTo(0L)
            return
        }
        val prevTrack = queueManager.getPreviousTrack(_currentTrack.value) ?: return
        loadAndStart(prevTrack, applyFadeIn = _crossfadeEnabled.value)
    }

    fun seekTo(positionMs: Long) {
        fadeController.cancelAndRestore()
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = player.currentPosition
                val rawDur = player.duration
                val dur = if (rawDur > 0L && rawDur != C.TIME_UNSET) {
                    rawDur
                } else {
                    (_currentTrack.value?.durationMs ?: 0L).coerceAtLeast(0L)
                }
                _currentPosition.value = pos

                // Check for smooth end-of-track fade transition into next track
                if (_crossfadeEnabled.value &&
                    !fadeController.isTransitioning.value &&
                    player.isPlaying &&
                    dur > 4000L
                ) {
                    val transitionSec = _crossfadeDurationSeconds.value
                    if (transitionSec > 0) {
                        val transitionMs = (transitionSec * 1000L).coerceAtMost((dur * 0.4f).toLong())
                        val remainingMs = dur - pos
                        if (remainingMs in 1..transitionMs) {
                            val nextTrack = queueManager.peekNextTrack(_currentTrack.value)
                            if (nextTrack != null) {
                                queueManager.getNextTrack(_currentTrack.value)
                                fadeController.transitionBetweenTracks(transitionMs) {
                                    loadAndStart(nextTrack, applyFadeIn = false, isTransitionSwitch = true)
                                }
                            }
                        }
                    }
                }

                delay(100L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            // MediaSessionService manages its own foreground state automatically.
            // Using startForegroundService directly causes ForegroundServiceDidNotStartInTimeException
            // if notifications cannot be posted immediately. startService is safe and recommended.
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to start PlaybackService: ${e.message}")
        }
    }

    private fun buildMediaItem(track: Track): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)

        if (!track.albumArtUri.isNullOrBlank()) {
            try {
                metadataBuilder.setArtworkUri(android.net.Uri.parse(track.albumArtUri))
            } catch (e: Exception) {
                // Ignore invalid album art URI
            }
        }

        val parsedUri = try {
            val u = android.net.Uri.parse(track.uri)
            if (u.scheme.isNullOrBlank()) {
                android.net.Uri.fromFile(java.io.File(track.uri))
            } else {
                u
            }
        } catch (e: Exception) {
            android.net.Uri.parse(track.uri)
        }

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(parsedUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun release() {
        stopProgressTracker()
        fadeController.cancelAndRestore()
        equalizerManager.release()
        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error releasing LoudnessEnhancer: ${e.message}")
        }
        player.release()
        abandonAudioFocus()
    }
}
