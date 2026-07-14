package com.example.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.usecase.GetLeastPlayedTracksUseCase
import com.example.domain.usecase.GetMostPlayedTracksUseCase
import com.example.domain.usecase.GetPlaylistsUseCase
import com.example.domain.usecase.GetRecentTracksUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    getPlaylistsUseCase: GetPlaylistsUseCase,
    getLeastPlayedTracksUseCase: GetLeastPlayedTracksUseCase,
    getRecentTracksUseCase: GetRecentTracksUseCase,
    getMostPlayedTracksUseCase: GetMostPlayedTracksUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    fun playTrack(track: Track) {
        audioPlayer.playTrack(track)
    }

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedTracks: StateFlow<List<Track>> = getLeastPlayedTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTracks: StateFlow<List<Track>> = getRecentTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val mostPlayedTracks: StateFlow<List<Track>> = getMostPlayedTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTrack = audioPlayer.currentTrack
    val isPlaying = audioPlayer.isPlaying
    val currentPosition = audioPlayer.currentPosition

    companion object {
        fun provideFactory(
            getPlaylistsUseCase: GetPlaylistsUseCase,
            getLeastPlayedTracksUseCase: GetLeastPlayedTracksUseCase,
            getRecentTracksUseCase: GetRecentTracksUseCase,
            getMostPlayedTracksUseCase: GetMostPlayedTracksUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    getPlaylistsUseCase,
                    getLeastPlayedTracksUseCase,
                    getRecentTracksUseCase,
                    getMostPlayedTracksUseCase,
                    audioPlayer
                ) as T
            }
        }
    }
}
