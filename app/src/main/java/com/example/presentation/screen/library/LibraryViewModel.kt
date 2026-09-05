package com.example.presentation.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.domain.usecase.GetPlaylistsUseCase
import com.example.domain.usecase.CreatePlaylistUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.player.AudioPlayer

class LibraryViewModel(
    getAllTracksUseCase: GetAllTracksUseCase,
    getPlaylistsUseCase: GetPlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val currentTrack = audioPlayer.currentTrack
    val isPlaying = audioPlayer.isPlaying
    val currentPosition = audioPlayer.currentPosition

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            createPlaylistUseCase(name)
        }
    }

    companion object {
        fun provideFactory(
            getAllTracksUseCase: GetAllTracksUseCase,
            getPlaylistsUseCase: GetPlaylistsUseCase,
            createPlaylistUseCase: CreatePlaylistUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(getAllTracksUseCase, getPlaylistsUseCase, createPlaylistUseCase, audioPlayer) as T
            }
        }
    }
}
