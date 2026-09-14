package com.example.data.repository

import com.example.data.db.dao.TrackDao
import com.example.data.scanner.MediaStoreScanner
import com.example.domain.model.Track
import com.example.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

class TrackRepositoryImpl(
    private val trackDao: TrackDao,
    private val scanner: MediaStoreScanner
) : TrackRepository {

    private val scanMutex = Mutex()

    override fun getAllTracks(): Flow<List<Track>> {
        return trackDao.getAllTracks().map { list ->
            list.map { Track(it.id, it.title, it.artist, it.album, it.albumArtUri, it.durationMs, it.uri) }
        }.distinctUntilChanged()
    }

    override fun getMostPlayedTracks(): Flow<List<Track>> {
        return trackDao.getMostPlayedTracks().map { list ->
            list.map { Track(it.id, it.title, it.artist, it.album, it.albumArtUri, it.durationMs, it.uri) }
        }.distinctUntilChanged()
    }

    override fun getLeastPlayedTracks(): Flow<List<Track>> {
        return trackDao.getLeastPlayedTracks().map { list ->
            list.map { Track(it.id, it.title, it.artist, it.album, it.albumArtUri, it.durationMs, it.uri) }
        }.distinctUntilChanged()
    }

    override fun getRecentTracks(): Flow<List<Track>> {
        return trackDao.getRecentTracks().map { list ->
            list.map { Track(it.id, it.title, it.artist, it.album, it.albumArtUri, it.durationMs, it.uri) }
        }.distinctUntilChanged()
    }

    override suspend fun getTrackById(id: Long): Track? {
        return trackDao.getTrackById(id)?.let {
            Track(it.id, it.title, it.artist, it.album, it.albumArtUri, it.durationMs, it.uri)
        }
    }

    override suspend fun scanDeviceForTracks() {
        if (!scanMutex.tryLock()) return // Already scanning, skip duplicate concurrent scan
        try {
            val scannedTracks = scanner.scanLocalAudio()
            if (scannedTracks.isNotEmpty()) {
                trackDao.insertTracks(scannedTracks.map {
                    com.example.data.db.entity.TrackEntity(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumArtUri = it.albumArtUri,
                        durationMs = it.durationMs,
                        uri = it.uri
                    )
                })
            }
        } finally {
            scanMutex.unlock()
        }
    }
}
