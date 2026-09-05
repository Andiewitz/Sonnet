package com.example.presentation.screen.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Album
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArtistDetailViewModel(
    val artistName: String,
    getAllTracksUseCase: GetAllTracksUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    val artistTracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .map { tracks ->
            tracks.filter { it.artist.equals(artistName, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistAlbums: StateFlow<List<Album>> = artistTracks
        .map { tracks ->
            tracks.groupBy { it.album }
                .map { (albumTitle, albumSongList) ->
                    Album(
                        title = albumTitle,
                        artist = artistName,
                        artworkUri = albumSongList.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                        trackCount = albumSongList.size,
                        tracks = albumSongList
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun provideFactory(
            artistName: String,
            getAllTracksUseCase: GetAllTracksUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ArtistDetailViewModel(artistName, getAllTracksUseCase, audioPlayer) as T
            }
        }
    }
}
