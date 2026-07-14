package com.example.player

import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.SonnetApplication
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        
        // We'll create the ExoPlayer here, or we can use the one from AppContainer.
        // Let's use the one from AppContainer so we don't break existing AudioPlayer logic
        // if AudioPlayer still directly references its own ExoPlayer.
        // Actually, it's safer to have AudioPlayer use this service, OR
        // just wrap the AppContainer's ExoPlayer.
        val appContainer = (application as SonnetApplication).container
        val player = appContainer.audioPlayer.player
        
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(CustomCallback())
            .build()

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

    private fun updateCustomLayout(session: MediaSession) {
        val repeatIcon = when (session.player.repeatMode) {
            Player.REPEAT_MODE_ONE -> com.example.R.drawable.ic_repeat_one
            else -> com.example.R.drawable.ic_repeat
        }
        
        val shuffleIcon = com.example.R.drawable.ic_shuffle
        
        val repeatButton = CommandButton.Builder()
            .setSessionCommand(SessionCommand("ACTION_REPEAT", Bundle.EMPTY))
            .setIconResId(repeatIcon)
            .setDisplayName("Repeat")
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
            // We don't release the player here because AppContainer owns it, 
            // but we do release the session.
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
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                "ACTION_REPEAT" -> {
                    val player = session.player
                    val nextMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = nextMode
                    updateCustomLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "ACTION_SHUFFLE" -> {
                    val player = session.player
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    updateCustomLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }
}
