package com.example.presentation.screen.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Track
import com.example.domain.usecase.AddToPlaylistUseCase
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.domain.usecase.GetTracksForPlaylistUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    val playlistId: Long,
    getAllTracksUseCase: GetAllTracksUseCase,
    getTracksForPlaylistUseCase: GetTracksForPlaylistUseCase,
    private val addToPlaylistUseCase: AddToPlaylistUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {
    val tracks: StateFlow<List<Track>> = if (playlistId == -1L) {
        getAllTracksUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        getTracksForPlaylistUseCase(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val allTracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTrackToPlaylist(trackId: Long) {
        if (playlistId == -1L) return
        viewModelScope.launch {
            addToPlaylistUseCase(playlistId, trackId)
        }
    }

    companion object {
        fun provideFactory(
            playlistId: Long,
            getAllTracksUseCase: GetAllTracksUseCase,
            getTracksForPlaylistUseCase: GetTracksForPlaylistUseCase,
            addToPlaylistUseCase: AddToPlaylistUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaylistDetailViewModel(
                    playlistId,
                    getAllTracksUseCase,
                    getTracksForPlaylistUseCase,
                    addToPlaylistUseCase,
                    audioPlayer
                ) as T
            }
        }
    }
}

