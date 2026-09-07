package com.example.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.datastore.UserPreferencesDataStore
import com.example.domain.model.Track
import com.google.common.util.concurrent.ListenableFuture
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
    context: Context,
    val userPreferencesDataStore: UserPreferencesDataStore
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    val equalizerManager = EqualizerManager(context.applicationContext, userPreferencesDataStore, scope)

    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var isNormalizeVolume = false

    val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .build(),
            false // set handleAudioFocus to false so we can manage focus completely manually!
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
        .build()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var focusRequest: android.media.AudioFocusRequest? = null
    private var playOnFocusGain = false

    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            android.media.AudioManager.AUDIOFOCUS_LOSS -> {
                playOnFocusGain = false
                player.pause()
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                playOnFocusGain = player.isPlaying
                player.pause()
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // "when any media plays over it have it stop."
                // Pause instead of ducking!
                playOnFocusGain = player.isPlaying
                player.pause()
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                if (playOnFocusGain) {
                    player.play()
                    playOnFocusGain = false
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val request = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
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
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
    
    private var progressJob: Job? = null
    
    private var playlist: List<Track> = emptyList()
    var onTrackPlayed: ((Long) -> Unit)? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        val sid = player.audioSessionId
        if (sid != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && sid > 0) {
            equalizerManager.bindAudioSession(sid)
            bindLoudnessEnhancer(sid)
        }

        player.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                if (audioSessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    equalizerManager.bindAudioSession(audioSessionId)
                    bindLoudnessEnhancer(audioSessionId)
                }
            }
        })

        // Restore saved player preferences
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
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error loading saved preferences: ${e.message}")
            }
        }

        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                super.onTracksChanged(tracks)
                val currentSid = player.audioSessionId
                if (currentSid != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && currentSid > 0) {
                    equalizerManager.bindAudioSession(currentSid)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                val currentSid = player.audioSessionId
                if (currentSid != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && currentSid > 0) {
                    equalizerManager.bindAudioSession(currentSid)
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    if (!requestAudioFocus()) {
                        player.pause()
                    }
                } else {
                    if (!playOnFocusGain) {
                        abandonAudioFocus()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    val sessionId = player.audioSessionId
                    if (sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && sessionId > 0) {
                        equalizerManager.bindAudioSession(sessionId)
                        bindLoudnessEnhancer(sessionId)
                    }
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    val trackId = mediaItem.mediaId.toLongOrNull()
                    if (trackId != null) {
                        _currentTrack.value = playlist.find { it.id == trackId }
                        onTrackPlayed?.invoke(trackId)
                    }
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
        // Gapless playback is natively supported by ExoPlayer playlist transitions
    }

    private fun bindLoudnessEnhancer(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
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
        playlist = tracks
        player.clearMediaItems()
        val mediaItems = tracks.map { track ->
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(android.net.Uri.parse(track.albumArtUri ?: ""))
                .build()
                
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(metadata)
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
        player.seekTo(startIndex, 0L)
        player.play()
    }
    
    fun playTrack(track: Track) {
        val existingIndex = playlist.indexOfFirst { it.id == track.id }
        if (existingIndex != -1 && player.mediaItemCount == playlist.size) {
            player.seekTo(existingIndex, 0L)
            player.play()
        } else {
            setPlaylist(listOf(track), 0)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun toggleRepeat() {
        val nextMode = if (player.repeatMode == Player.REPEAT_MODE_ALL) {
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
    
    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.mediaItemCount > 0) {
            player.seekToDefaultPosition(0)
            player.play()
        }
    }
    
    fun skipToPrevious() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (player.mediaItemCount > 0) {
            player.seekToDefaultPosition(player.mediaItemCount - 1)
            player.play()
        }
    }
    
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                delay(1000L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }
    
    fun release() {
        equalizerManager.release()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        player.release()
        stopProgressTracker()
    }
}
