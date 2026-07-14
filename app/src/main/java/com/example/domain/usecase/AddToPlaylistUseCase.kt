package com.example.domain.usecase

import com.example.domain.repository.PlaylistRepository

class AddToPlaylistUseCase(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, trackId: Long) {
        repository.addTrackToPlaylist(playlistId, trackId)
    }
}
