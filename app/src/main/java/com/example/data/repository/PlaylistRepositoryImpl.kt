package com.example.data.repository

import com.example.data.db.dao.PlaylistDao
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistTrackCrossRef
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {
    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsWithTracks().map { list ->
            list.map { item ->
                Playlist(
                    id = item.playlist.id,
                    name = item.playlist.name,
                    trackCount = item.tracks.size,
                    artworkUris = item.tracks.mapNotNull { it.albumArtUri }.take(4)
                )
            }
        }
    }

    override fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { list ->
            list.map { Track(it.id, it.title, it.artist, it.album, it.albumArtUri, it.durationMs, it.uri) }
        }
    }

    override suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.insertPlaylistTrackCrossRef(PlaylistTrackCrossRef(playlistId, trackId))
    }
}
