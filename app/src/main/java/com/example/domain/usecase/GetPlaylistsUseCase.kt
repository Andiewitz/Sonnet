package com.example.domain.usecase

import com.example.domain.model.Playlist
import com.example.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class GetPlaylistsUseCase(
    private val repository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return repository.getAllPlaylists()
    }
}
