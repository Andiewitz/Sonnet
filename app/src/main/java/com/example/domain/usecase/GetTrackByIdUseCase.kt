package com.example.domain.usecase

import com.example.domain.model.Track
import com.example.domain.repository.TrackRepository

class GetTrackByIdUseCase(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(id: Long): Track? {
        return repository.getTrackById(id) // We need to add this to TrackRepository
    }
}
