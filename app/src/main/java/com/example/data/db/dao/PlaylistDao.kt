package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistTrackCrossRef
import com.example.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrackCrossRef(crossRef: PlaylistTrackCrossRef)

    @Query("""
        SELECT tracks.* FROM tracks 
        INNER JOIN playlist_track ON tracks.id = playlist_track.trackId 
        WHERE playlist_track.playlistId = :playlistId
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>
    
    @Query("SELECT COUNT(*) FROM playlist_track WHERE playlistId = :playlistId")
    fun getTrackCountForPlaylist(playlistId: Long): Flow<Int>
}
