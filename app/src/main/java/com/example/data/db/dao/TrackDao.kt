package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)
    
    @Query("SELECT tracks.* FROM tracks INNER JOIN play_history ON tracks.id = play_history.trackId GROUP BY tracks.id ORDER BY COUNT(play_history.id) DESC LIMIT 20")
    fun getMostPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT tracks.* FROM tracks LEFT JOIN play_history ON tracks.id = play_history.trackId GROUP BY tracks.id ORDER BY COUNT(play_history.id) ASC LIMIT 20")
    fun getLeastPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT tracks.* FROM tracks INNER JOIN play_history ON tracks.id = play_history.trackId GROUP BY tracks.id ORDER BY MAX(play_history.playedAt) DESC LIMIT 20")
    fun getRecentTracks(): Flow<List<TrackEntity>>
}
