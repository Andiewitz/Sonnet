package com.example.presentation.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Track
import com.example.domain.usecase.GetAllTracksUseCase
import com.example.domain.usecase.GetMostPlayedTracksUseCase
import com.example.player.AudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class SearchViewModel(
    getAllTracksUseCase: GetAllTracksUseCase,
    getMostPlayedTracksUseCase: GetMostPlayedTracksUseCase,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    val allTracks: StateFlow<List<Track>> = getAllTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedTracks: StateFlow<List<Track>> = getMostPlayedTracksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<Album>> = allTracks.map { tracks ->
        tracks.groupBy { it.album }
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

    val topAlbums: StateFlow<List<Album>> = combine(allAlbums, mostPlayedTracks) { albums, playedTracks ->
        if (playedTracks.isNotEmpty()) {
            val mostPlayedAlbumNames = playedTracks.map { it.album }
            val playedCountMap = mostPlayedAlbumNames.groupingBy { it }.eachCount()
            val playedAlbums = albums
                .filter { it.title in playedCountMap }
                .sortedByDescending { playedCountMap[it.title] ?: 0 }
            
            if (playedAlbums.size >= 5) {
                playedAlbums.take(6)
            } else {
                val remaining = albums
                    .filter { it !in playedAlbums }
                    .sortedByDescending { it.trackCount }
                (playedAlbums + remaining).take(6)
            }
        } else {
            albums.sortedByDescending { it.trackCount }.take(6)
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<Triple<List<Track>, List<Album>, List<Artist>>> = 
        combine(_searchQuery, allTracks, allAlbums) { query, tracks, albums ->
            if (query.isBlank()) {
                Triple(emptyList(), emptyList(), emptyList())
            } else {
                val matchedT = tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
                val matchedA = albums.filter { it.title.contains(query, ignoreCase = true) }
                val matchedArt = tracks
                    .filter { it.artist.contains(query, ignoreCase = true) }
                    .groupBy { it.artist }
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
                Triple(matchedT, matchedA, matchedArt)
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), emptyList(), emptyList()))

    companion object {
        fun provideFactory(
            getAllTracksUseCase: GetAllTracksUseCase,
            getMostPlayedTracksUseCase: GetMostPlayedTracksUseCase,
            audioPlayer: AudioPlayer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(getAllTracksUseCase, getMostPlayedTracksUseCase, audioPlayer) as T
            }
        }
    }
}
