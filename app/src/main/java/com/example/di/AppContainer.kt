package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.datastore.UserPreferencesDataStore
import com.example.data.db.AppDatabase
import com.example.data.repository.PlayHistoryRepositoryImpl
import com.example.data.repository.PlaylistRepositoryImpl
import com.example.data.repository.TrackRepositoryImpl
import com.example.data.scanner.MediaStoreScanner
import com.example.domain.usecase.AddToPlaylistUseCase
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.domain.usecase.GetPlayHistoryUseCase
import com.example.domain.usecase.GetPlaylistsUseCase

import com.example.player.AudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "sonnet_db").build()
    
    val audioPlayer = AudioPlayer(context)
    
    val trackDao = database.trackDao()
    val playlistDao = database.playlistDao()
    val playHistoryDao = database.playHistoryDao()
    
    val mediaStoreScanner = MediaStoreScanner(context)
    val userPreferencesDataStore = UserPreferencesDataStore(context)
    
    val trackRepository = TrackRepositoryImpl(trackDao, mediaStoreScanner)
    val playlistRepository = PlaylistRepositoryImpl(playlistDao)
    val playHistoryRepository = PlayHistoryRepositoryImpl(playHistoryDao)
    
    val getAllTracksUseCase = GetAllTracksUseCase(trackRepository)
    val getTrackByIdUseCase = com.example.domain.usecase.GetTrackByIdUseCase(trackRepository)
    val getMostPlayedTracksUseCase = com.example.domain.usecase.GetMostPlayedTracksUseCase(trackRepository)
    val getLeastPlayedTracksUseCase = com.example.domain.usecase.GetLeastPlayedTracksUseCase(trackRepository)
    val getRecentTracksUseCase = com.example.domain.usecase.GetRecentTracksUseCase(trackRepository)
    val getPlaylistsUseCase = GetPlaylistsUseCase(playlistRepository)
    val addToPlaylistUseCase = AddToPlaylistUseCase(playlistRepository)
    val getPlayHistoryUseCase = GetPlayHistoryUseCase(playHistoryRepository)
    val recordPlayUseCase = com.example.domain.usecase.RecordPlayUseCase(playHistoryRepository)

    init {
        audioPlayer.onTrackPlayed = { trackId ->
            GlobalScope.launch(Dispatchers.IO) {
                recordPlayUseCase(trackId)
            }
        }
    }
}
