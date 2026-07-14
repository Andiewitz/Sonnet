package com.example.domain.repository

import com.example.domain.model.Playlist
import com.example.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>
    suspend fun createPlaylist(name: String)
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
}
