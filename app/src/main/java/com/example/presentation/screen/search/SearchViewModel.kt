package com.example.presentation.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    getAllTracksUseCase: GetAllTracksUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    val allTracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun provideFactory(
            getAllTracksUseCase: GetAllTracksUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(getAllTracksUseCase, audioPlayer) as T
            }
        }
    }
}
