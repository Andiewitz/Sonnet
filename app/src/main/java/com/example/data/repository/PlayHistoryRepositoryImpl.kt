package com.example.data.repository

import com.example.data.db.dao.PlayHistoryDao
import com.example.data.db.entity.PlayHistoryEntity
import com.example.domain.model.PlayHistory
import com.example.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayHistoryRepositoryImpl(
    private val playHistoryDao: PlayHistoryDao
) : PlayHistoryRepository {
    override fun getRecentHistory(): Flow<List<PlayHistory>> {
        return playHistoryDao.getRecentHistory().map { list ->
            list.map { PlayHistory(it.id, it.trackId, it.playedAt) }
        }
    }

    override suspend fun recordPlay(trackId: Long) {
        playHistoryDao.insertPlayHistory(PlayHistoryEntity(trackId = trackId, playedAt = System.currentTimeMillis()))
    }
}
