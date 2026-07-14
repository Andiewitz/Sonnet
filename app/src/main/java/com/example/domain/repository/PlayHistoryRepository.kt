package com.example.domain.repository

import com.example.domain.model.PlayHistory
import kotlinx.coroutines.flow.Flow

interface PlayHistoryRepository {
    fun getRecentHistory(): Flow<List<PlayHistory>>
    suspend fun recordPlay(trackId: Long)
}
