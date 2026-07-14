package com.example.presentation.screen.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Track
import com.example.domain.usecase.GetTrackByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.player.AudioPlayer

class NowPlayingViewModel(
    private val getTrackByIdUseCase: GetTrackByIdUseCase,
    private val audioPlayer: AudioPlayer
) : ViewModel() {
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val currentTrack: StateFlow<Track?> = audioPlayer.currentTrack
    val currentPosition: StateFlow<Long> = audioPlayer.currentPosition
    val shuffleModeEnabled: StateFlow<Boolean> = audioPlayer.shuffleModeEnabled
    val repeatMode: StateFlow<Int> = audioPlayer.repeatMode

    fun togglePlayPause() {
        audioPlayer.togglePlayPause()
    }

    fun toggleShuffle() {
        audioPlayer.toggleShuffle()
    }

    fun toggleRepeat() {
        audioPlayer.toggleRepeat()
    }
    
    fun skipToNext() {
        audioPlayer.skipToNext()
    }
    
    fun skipToPrevious() {
        audioPlayer.skipToPrevious()
    }
    
    fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
    }
    
    fun loadTrack(id: Long) {
        viewModelScope.launch {
            val track = getTrackByIdUseCase(id)
            if (track != null && audioPlayer.currentTrack.value?.id != track.id) {
                audioPlayer.playTrack(track)
            }
        }
    }

    companion object {
        fun provideFactory(getTrackByIdUseCase: GetTrackByIdUseCase, audioPlayer: AudioPlayer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NowPlayingViewModel(getTrackByIdUseCase, audioPlayer) as T
            }
        }
    }
}
