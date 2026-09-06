package com.example.presentation.screen.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.SonnetApplication
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Track
import com.example.presentation.component.AlbumArt
import com.example.presentation.component.TrackItem
import com.example.presentation.theme.*

data class AlbumDisplayItem(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artworkUri: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(
            appContainer.getAllTracksUseCase,
            appContainer.getMostPlayedTracksUseCase,
            appContainer.audioPlayer
        )
    )

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var trackToAddByDialog by remember { mutableStateOf<Track?>(null) }

    // All available albums grouped from library tracks
    val allAlbums = remember(allTracks) {
        allTracks
            .groupBy { it.album }
            .map { (albumTitle, tracks) ->
                Album(
                    title = albumTitle,
                    artist = tracks.firstOrNull()?.artist ?: "Unknown Artist",
                    artworkUri = tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                    trackCount = tracks.size,
                    tracks = tracks
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    // Top albums tracked by user play history (same logic as most popular/played tracks)
    val topAlbums = remember(allAlbums, mostPlayedTracks) {
        if (mostPlayedTracks.isNotEmpty()) {
            val mostPlayedAlbumNames = mostPlayedTracks.map { it.album }
            val playedCountMap = mostPlayedAlbumNames.groupingBy { it }.eachCount()
            val playedAlbums = allAlbums
                .filter { it.title in playedCountMap }
                .sortedByDescending { playedCountMap[it.title] ?: 0 }
            
            if (playedAlbums.size >= 5) {
                playedAlbums.take(6)
            } else {
                // If few played albums, fill with top-track-count albums
                val remaining = allAlbums
                    .filter { it !in playedAlbums }
                    .sortedByDescending { it.trackCount }
                (playedAlbums + remaining).take(6)
            }
        } else {
            allAlbums.sortedByDescending { it.trackCount }.take(6)
        }
    }

    val filteredTracks = remember(allTracks, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val matchedAlbums = remember(allAlbums, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allAlbums.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    val matchedArtists = remember(allTracks, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allTracks
                .filter { it.artist.contains(searchQuery, ignoreCase = true) }
                .groupBy { it.artist }
                .map { (artistName, tracks) ->
                    val albumsCount = tracks.map { it.album }.distinct().size
                    Artist(
                        name = artistName,
                        trackCount = tracks.size,
                        albumCount = albumsCount,
                        artworkUri = tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                        tracks = tracks
                    )
                }
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered Search Header & Centered Search TextField Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Artists, songs or albums",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BgSecondary,
                            unfocusedContainerColor = BgSecondary,
                            disabledContainerColor = BgSecondary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Body: Search Results OR Browse Genres
            if (searchQuery.isNotBlank()) {
                if (filteredTracks.isEmpty() && matchedAlbums.isEmpty() && matchedArtists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🎧",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please check the spelling or try searching for another artist, album, or song.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // Matching Artists
                        if (matchedArtists.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Artists",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(matchedArtists) { artist ->
                                        Column(
                                            modifier = Modifier
                                                .width(96.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    navController.navigate("artist/${android.net.Uri.encode(artist.name)}")
                                                }
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape)
                                                    .background(BgTertiary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (artist.artworkUri != null) {
                                                    AlbumArt(uri = artist.artworkUri, modifier = Modifier.fillMaxSize())
                                                } else {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Matching Albums
                        if (matchedAlbums.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(matchedAlbums) { album ->
                                        Column(
                                            modifier = Modifier
                                                .width(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    navController.navigate("album/${android.net.Uri.encode(album.title)}")
                                                }
                                                .padding(4.dp)
                                        ) {
                                            AlbumArt(
                                                uri = album.artworkUri,
                                                modifier = Modifier
                                                    .size(102.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = album.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = album.artist,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Matching Songs
                        if (filteredTracks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Songs",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            itemsIndexed(filteredTracks, key = { _, track -> track.id }) { index, track ->
                                Column(modifier = Modifier.animateItem()) {
                                    TrackItem(
                                        track = track,
                                        onClick = {
                                            viewModel.audioPlayer.setPlaylist(filteredTracks, index)
                                            navController.navigate("now_playing/${track.id}")
                                        },
                                        onMoreClick = {
                                            trackToAddByDialog = track
                                        }
                                    )
                                    HorizontalDivider(
                                        color = BorderSubtle,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    // Top Albums Section (Ranked by Play History - same logic as popular tracks)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Top albums",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                if (topAlbums.isNotEmpty()) {
                                    Text(
                                        text = "Most played",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AccentPrimary
                                    )
                                }
                            }

                            if (topAlbums.isEmpty()) {
                                Text(
                                    text = "Play songs to view your top albums",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(topAlbums, key = { "top_" + it.title }) { album ->
                                        TopAlbumItem(
                                            album = album,
                                            onClick = {
                                                navController.navigate("album/${android.net.Uri.encode(album.title)}")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // All Albums Section (Clean seamless list rows - no card box)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All albums",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "${allAlbums.size} albums",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    if (allAlbums.isEmpty()) {
                        item {
                            Text(
                                text = "No albums found in library",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                    } else {
                        itemsIndexed(allAlbums, key = { _, album -> "all_" + album.title }) { index, album ->
                            Column {
                                AlbumRowItem(
                                    album = album,
                                    onClick = {
                                        navController.navigate("album/${android.net.Uri.encode(album.title)}")
                                    }
                                )
                                if (index < allAlbums.size - 1) {
                                    HorizontalDivider(
                                        color = BorderSubtle,
                                        modifier = Modifier.padding(start = 80.dp, end = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    trackToAddByDialog?.let { track ->
        com.example.presentation.component.AddToPlaylistDialog(
            track = track,
            onDismiss = { trackToAddByDialog = null }
        )
    }
}

@Composable
fun TopAlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        AlbumArt(
            uri = album.artworkUri,
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary
        )
    }
}

@Composable
fun AlbumRowItem(
    album: Album,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            uri = album.artworkUri,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${album.artist} • ${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
