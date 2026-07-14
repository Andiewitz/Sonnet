package com.example.presentation.screen.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.domain.usecase.GetTracksForPlaylistUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class PlaylistDetailViewModel(
    val playlistId: Long,
    getAllTracksUseCase: GetAllTracksUseCase,
    getTracksForPlaylistUseCase: GetTracksForPlaylistUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {
    val tracks: StateFlow<List<Track>> = if (playlistId == -1L) {
        getAllTracksUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        getTracksForPlaylistUseCase(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    companion object {
        fun provideFactory(
            playlistId: Long,
            getAllTracksUseCase: GetAllTracksUseCase,
            getTracksForPlaylistUseCase: GetTracksForPlaylistUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaylistDetailViewModel(playlistId, getAllTracksUseCase, getTracksForPlaylistUseCase, audioPlayer) as T
            }
        }
    }
}
