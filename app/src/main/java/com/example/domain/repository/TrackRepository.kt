package com.example.domain.repository

import com.example.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getAllTracks(): Flow<List<Track>>
    fun getMostPlayedTracks(): Flow<List<Track>>
    fun getLeastPlayedTracks(): Flow<List<Track>>
    fun getRecentTracks(): Flow<List<Track>>
    suspend fun getTrackById(id: Long): Track?
    suspend fun scanDeviceForTracks()
}
