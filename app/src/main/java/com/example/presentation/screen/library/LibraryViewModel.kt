package com.example.presentation.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.domain.usecase.GetPlaylistsUseCase
import com.example.domain.usecase.CreatePlaylistUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
        
    val allPlaylists: StateFlow<List<Playlist>> = combine(tracks, playlists) { trackList, playlistList ->
        listOf(
            Playlist(
                id = -1L,
                name = "All Songs",
                trackCount = trackList.size,
                artworkUris = trackList.mapNotNull { it.albumArtUri }.take(4)
            )
        ) + playlistList
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<Album>> = tracks.map { trackList ->
        trackList.groupBy { it.album }
            .map { (albumTitle, albumTracks) ->
                Album(
                    title = albumTitle,
                    artist = albumTracks.firstOrNull()?.artist ?: "Unknown Artist",
                    artworkUri = albumTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                    trackCount = albumTracks.size,
                    tracks = albumTracks
                )
            }
            .sortedBy { it.title.lowercase() }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtists: StateFlow<List<Artist>> = tracks.map { trackList ->
        trackList.groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                val albumsCount = artistTracks.map { it.album }.distinct().size
                Artist(
                    name = artistName,
                    trackCount = artistTracks.size,
                    albumCount = albumsCount,
                    artworkUri = artistTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                    tracks = artistTracks
                )
            }
            .sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default)
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
