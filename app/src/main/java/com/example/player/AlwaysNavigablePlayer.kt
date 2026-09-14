package com.example.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

class AlwaysNavigablePlayer(
    player: Player,
    private val onNext: (() -> Unit)? = null,
    private val onPrevious: (() -> Unit)? = null
) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = true

    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() {
        seekToNextMediaItem()
    }

    override fun seekToNextMediaItem() {
        if (onNext != null) {
            onNext.invoke()
        } else if (super.hasNextMediaItem()) {
            super.seekToNextMediaItem()
        } else if (mediaItemCount > 0) {
            seekToDefaultPosition(0)
            play()
        }
    }

    override fun seekToPrevious() {
        seekToPreviousMediaItem()
    }

    override fun seekToPreviousMediaItem() {
        if (onPrevious != null) {
            onPrevious.invoke()
        } else if (currentPosition > 3000L) {
            seekTo(0L)
        } else if (super.hasPreviousMediaItem()) {
            super.seekToPreviousMediaItem()
        } else if (mediaItemCount > 0) {
            seekToDefaultPosition(mediaItemCount - 1)
            play()
        }
    }
}
