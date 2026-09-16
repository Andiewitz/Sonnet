package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.SonnetApplication
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "sonnet_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val appContainer = (application as SonnetApplication).container
        val rawPlayer = appContainer.audioPlayer.player
        val player = AlwaysNavigablePlayer(
            player = rawPlayer,
            onNext = {
                mainHandler.post {
                    appContainer.audioPlayer.skipToNext()
                }
            },
            onPrevious = {
                mainHandler.post {
                    appContainer.audioPlayer.skipToPrevious()
                }
            }
        )

        // PendingIntent to launch/return to the app when notification/media toaster is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomCallback())
            .build()

        // Configure notification provider to use our channel and app name
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(com.example.R.string.playback_channel_name)
            .build()
        setMediaNotificationProvider(notificationProvider)

        appContainer.audioPlayer.onActivePlayerChanged = { newPlayer ->
            val navigablePlayer = AlwaysNavigablePlayer(
                player = newPlayer,
                onNext = { appContainer.audioPlayer.skipToNext() },
                onPrevious = { appContainer.audioPlayer.skipToPrevious() }
            )
            navigablePlayer.addListener(object : Player.Listener {
                override fun onRepeatModeChanged(repeatMode: Int) {
                    mediaSession?.let { updateCustomLayout(it) }
                }
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    mediaSession?.let { updateCustomLayout(it) }
                }
            })
            mediaSession?.setPlayer(navigablePlayer)
            mediaSession?.let { updateCustomLayout(it) }
        }

        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                mediaSession?.let { updateCustomLayout(it) }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                mediaSession?.let { updateCustomLayout(it) }
            }
        })

        mediaSession?.let { updateCustomLayout(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(com.example.R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(com.example.R.string.playback_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateCustomLayout(session: MediaSession) {
        val repeatIcon = when (session.player.repeatMode) {
            Player.REPEAT_MODE_ONE -> com.example.R.drawable.ic_repeat_one
            else -> com.example.R.drawable.ic_repeat
        }
        
        val shuffleIcon = com.example.R.drawable.ic_shuffle
        
        val repeatButton = CommandButton.Builder()
            .setSessionCommand(SessionCommand("ACTION_REPEAT", Bundle.EMPTY))
            .setIconResId(repeatIcon)
            .setDisplayName(if (session.player.repeatMode == Player.REPEAT_MODE_ONE) "Loop Once" else "Loop All")
            .setEnabled(true)
            .build()

        val shuffleButton = CommandButton.Builder()
            .setSessionCommand(SessionCommand("ACTION_SHUFFLE", Bundle.EMPTY))
            .setIconResId(shuffleIcon)
            .setDisplayName("Shuffle")
            .setEnabled(true)
            .build()

        session.setCustomLayout(listOf(repeatButton, shuffleButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class CustomCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand("ACTION_REPEAT", Bundle.EMPTY))
                .add(SessionCommand("ACTION_SHUFFLE", Bundle.EMPTY))
                .build()
            val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                playerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val appContainer = (application as SonnetApplication).container
            when (customCommand.customAction) {
                "ACTION_REPEAT" -> {
                    mainHandler.post {
                        appContainer.audioPlayer.toggleRepeat()
                        updateCustomLayout(session)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "ACTION_SHUFFLE" -> {
                    mainHandler.post {
                        appContainer.audioPlayer.toggleShuffle()
                        updateCustomLayout(session)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }
}
