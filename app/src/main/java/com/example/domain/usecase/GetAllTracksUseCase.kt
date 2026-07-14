package com.example.domain.usecase

import com.example.domain.model.Track
import com.example.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow

class GetAllTracksUseCase(
    private val repository: TrackRepository
) {
    operator fun invoke(): Flow<List<Track>> {
        return repository.getAllTracks()
    }
}
