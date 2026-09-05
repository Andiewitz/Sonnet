package com.example.presentation.screen.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AlbumDetailViewModel(
    val albumTitle: String,
    getAllTracksUseCase: GetAllTracksUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    val albumTracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .map { tracks ->
            tracks.filter { it.album.equals(albumTitle, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun provideFactory(
            albumTitle: String,
            getAllTracksUseCase: GetAllTracksUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlbumDetailViewModel(albumTitle, getAllTracksUseCase, audioPlayer) as T
            }
        }
    }
}
