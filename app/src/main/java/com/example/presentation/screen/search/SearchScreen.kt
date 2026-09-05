package com.example.presentation.screen.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.SonnetApplication
import com.example.domain.model.Track
import com.example.presentation.component.TrackItem
import com.example.presentation.theme.*

data class Genre(val name: String, val color: Color)

val topGenres = listOf(
    Genre("Hip-Hop", TileAmber),
    Genre("Summer", TileGarnet),
    Genre("Pop", TileDustyRose),
    Genre("Electronic", TileIndigo)
)

val browseAllGenres = listOf(
    Genre("Romantic", TileTeal),
    Genre("Bollywood", TilePlum),
    Genre("Indie", TileOlive),
    Genre("Reggae", TileSienna),
    Genre("Workout", TileGarnet),
    Genre("Sleep", TileIndigo)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(
            appContainer.getAllTracksUseCase,
            appContainer.audioPlayer
        )
    )

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var trackToAddByDialog by remember { mutableStateOf<Track?>(null) }

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

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            // Search Header & TextField
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Artists, songs or albums",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
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
                        .clip(RoundedCornerShape(8.dp)),
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

            // Body: Search Results OR Browse Genres
            if (searchQuery.isNotBlank()) {
                if (filteredTracks.isEmpty()) {
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
                                text = "Please check the spelling or try searching for another artist or song.",
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
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Your top genres",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(topGenres) { genre ->
                        GenreCard(genre)
                    }

                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Browse all",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }

                    items(browseAllGenres) { genre ->
                        GenreCard(genre)
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
fun GenreCard(genre: Genre) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(genre.color)
    ) {
        Text(
            text = genre.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 8.dp)
                .size(72.dp)
                .rotate(20f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
        }
    }
}
