package com.example.presentation.screen.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.SonnetApplication
import com.example.domain.model.Track
import com.example.presentation.component.AlbumArt
import com.example.presentation.component.PlaylistArtwork
import com.example.presentation.component.TrackItem
import com.example.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    navController: NavController
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: PlaylistDetailViewModel = viewModel(
        factory = PlaylistDetailViewModel.provideFactory(
            playlistId,
            appContainer.getAllTracksUseCase,
            appContainer.getTracksForPlaylistUseCase,
            appContainer.addToPlaylistUseCase,
            appContainer.audioPlayer
        )
    )

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    var trackToAdd by remember { mutableStateOf<Track?>(null) }
    var showAddSongsSheet by rememberSaveable { mutableStateOf(false) }

    val artworkUris = remember(tracks) {
        tracks.mapNotNull { it.albumArtUri }.take(4)
    }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(BgSecondary.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Spotify Hero Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistArtwork(
                        artworkUris = artworkUris,
                        isAllSongs = (playlistId == -1L),
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${tracks.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tracks.isNotEmpty()) {
                            // Shuffle Play button
                            IconButton(
                                onClick = {
                                    val shuffled = tracks.shuffled()
                                    viewModel.audioPlayer.setPlaylist(shuffled, 0)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(BgTertiary, CircleShape)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = TextPrimary)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Main Play FAB Button
                            FloatingActionButton(
                                onClick = {
                                    viewModel.audioPlayer.setPlaylist(tracks, 0)
                                },
                                containerColor = AccentPrimary,
                                contentColor = Color.Black,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play Playlist",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            if (playlistId != -1L) {
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        // Single Add Songs Button
                        if (playlistId != -1L) {
                            OutlinedButton(
                                onClick = { showAddSongsSheet = true },
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Songs", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This playlist has no songs yet.\nTap 'Add Songs' to add music!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    Column(modifier = Modifier.animateItem()) {
                        TrackItem(
                            track = track,
                            onClick = {
                                viewModel.audioPlayer.setPlaylist(tracks, index)
                            },
                            onMoreClick = {
                                trackToAdd = track
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

    trackToAdd?.let { track ->
        com.example.presentation.component.AddToPlaylistDialog(
            track = track,
            onDismiss = { trackToAdd = null }
        )
    }

    if (showAddSongsSheet) {
        AddSongsToPlaylistSheet(
            playlistName = playlistName,
            existingTrackIds = tracks.map { it.id }.toSet(),
            allTracks = allTracks,
            onAddTrack = { trackId -> viewModel.addTrackToPlaylist(trackId) },
            onDismiss = { showAddSongsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistSheet(
    playlistName: String,
    existingTrackIds: Set<Long>,
    allTracks: List<Track>,
    onAddTrack: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTracks = remember(allTracks, searchQuery) {
        if (searchQuery.isBlank()) {
            allTracks
        } else {
            allTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSecondary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add Songs to $playlistName",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs or artists...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No songs found", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(filteredTracks, key = { it.id }) { track ->
                        val isAlreadyAdded = existingTrackIds.contains(track.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isAlreadyAdded) {
                                        onAddTrack(track.id)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArt(
                                uri = track.albumArtUri,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (!isAlreadyAdded) {
                                        onAddTrack(track.id)
                                    }
                                }
                            ) {
                                if (isAlreadyAdded) {
                                    Icon(Icons.Default.Check, contentDescription = "Added", tint = AccentPrimary)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = TextPrimary)
                                }
                            }
                        }
                        HorizontalDivider(
                            color = BorderSubtle,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
