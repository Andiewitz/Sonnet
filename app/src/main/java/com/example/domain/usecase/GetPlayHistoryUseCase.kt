package com.example.domain.usecase

import com.example.domain.model.PlayHistory
import com.example.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetPlayHistoryUseCase(
    private val repository: PlayHistoryRepository
) {
    operator fun invoke(): Flow<List<PlayHistory>> {
        return repository.getRecentHistory()
    }
}
