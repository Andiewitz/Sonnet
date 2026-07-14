package com.example.domain.usecase

import com.example.domain.model.Track
import com.example.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class GetTracksForPlaylistUseCase(
    private val repository: PlaylistRepository
) {
    operator fun invoke(playlistId: Long): Flow<List<Track>> {
        return repository.getTracksForPlaylist(playlistId)
    }
}
