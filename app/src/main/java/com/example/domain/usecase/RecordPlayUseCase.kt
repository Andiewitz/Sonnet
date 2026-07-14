package com.example.domain.usecase

import com.example.domain.repository.PlayHistoryRepository

class RecordPlayUseCase(
    private val repository: PlayHistoryRepository
) {
    suspend operator fun invoke(trackId: Long) {
        repository.recordPlay(trackId)
    }
}
