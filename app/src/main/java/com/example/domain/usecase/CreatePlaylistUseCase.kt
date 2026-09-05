package com.example.domain.usecase

import com.example.domain.repository.PlaylistRepository

class CreatePlaylistUseCase(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(name: String) {
        repository.createPlaylist(name)
    }
}
