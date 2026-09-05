package com.example.presentation.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.SonnetApplication
import com.example.domain.model.Album
import com.example.domain.model.Track
import com.example.presentation.component.AlbumArt
import com.example.presentation.component.TrackItem
import com.example.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    navController: NavController
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: ArtistDetailViewModel = viewModel(
        factory = ArtistDetailViewModel.provideFactory(
            artistName,
            appContainer.getAllTracksUseCase,
            appContainer.audioPlayer
        )
    )

    val tracks by viewModel.artistTracks.collectAsStateWithLifecycle()
    val albums by viewModel.artistAlbums.collectAsStateWithLifecycle()
    var trackToAdd by remember { mutableStateOf<Track?>(null) }

    val artistArtUri = remember(tracks) {
        tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
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
            // Artist Hero
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(BgTertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artistArtUri != null) {
                            AlbumArt(
                                uri = artistArtUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${tracks.size} tracks • ${albums.size} ${if (albums.size == 1) "album/single" else "albums/singles"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (tracks.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                            Spacer(modifier = Modifier.width(20.dp))

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
                                    contentDescription = "Play Artist",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Albums / Singles Section
            if (albums.isNotEmpty()) {
                item {
                    Text(
                        text = "Albums & Singles",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(albums, key = { it.title }) { album ->
                            ArtistAlbumCard(
                                album = album,
                                onClick = {
                                    navController.navigate("album/${android.net.Uri.encode(album.title)}")
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Popular / All Tracks Section
            item {
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
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
                            text = "No songs found for this artist.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
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
}

@Composable
fun ArtistAlbumCard(
    album: Album,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AlbumArt(
            uri = album.artworkUri,
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.trackCount} tracks",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1
        )
    }
}
