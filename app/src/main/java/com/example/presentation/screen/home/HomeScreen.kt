package com.example.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.SonnetApplication
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.presentation.component.AppBottomNavigationBar
import com.example.presentation.component.MiniPlayer
import com.example.presentation.component.TrackItem
import com.example.presentation.component.AlbumArt
import com.example.presentation.component.PlaylistCardSkeleton
import com.example.presentation.component.RecentTrackCardSkeleton
import com.example.presentation.component.RecommendedTrackCardSkeleton
import com.example.presentation.component.TrackItemSkeleton
import com.example.presentation.theme.*

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            appContainer.getPlaylistsUseCase,
            appContainer.getLeastPlayedTracksUseCase,
            appContainer.getRecentTracksUseCase,
            appContainer.getMostPlayedTracksUseCase,
            appContainer.audioPlayer
        )
    )

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recommendedTracks by viewModel.recommendedTracks.collectAsStateWithLifecycle()
    val recentTracks by viewModel.recentTracks.collectAsStateWithLifecycle()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SONNET",
                        color = TextPrimary,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                            Icon(Icons.Outlined.Cast, contentDescription = "Cast", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            item {
                SectionTitle("Your Top Playlists", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                if (playlists.isEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(4) {
                            PlaylistCardSkeleton()
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistCard(playlist, modifier = Modifier.animateItem())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                SectionTitle("Recent Songs", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (recentTracks.isEmpty()) {
                val mockChunks = listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6))
                items(mockChunks) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { _ ->
                            RecentTrackCardSkeleton(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                val recentChunks = recentTracks.take(6).chunked(2)
                items(recentChunks, key = { it.first().id }) { chunk ->
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { track ->
                            RecentTrackCard(
                                track = track,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.playTrack(track) }
                            )
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionTitle("Recommended", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                if (recommendedTracks.isEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(4) {
                            RecommendedTrackCardSkeleton()
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recommendedTracks.take(5), key = { it.id }) { track ->
                            RecommendedTrackCard(
                                track = track,
                                modifier = Modifier.animateItem(),
                                onClick = { viewModel.playTrack(track) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SectionTitle("Most Played", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (mostPlayedTracks.isEmpty()) {
                items(5) {
                    TrackItemSkeleton()
                }
            } else {
                items(mostPlayedTracks.take(10), key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        modifier = Modifier.animateItem(),
                        onClick = { viewModel.playTrack(track) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        modifier = modifier
    )
}

@Composable
fun PlaylistCard(playlist: Playlist, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(120.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgTertiary),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for playlist art, could be composite album art later
            Text("🎵", style = MaterialTheme.typography.displayMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun RecentTrackCard(track: Track, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BgSecondary)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            uri = track.albumArtUri,
            modifier = Modifier
                .size(48.dp)
                .background(BgTertiary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
fun RecommendedTrackCard(track: Track, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        AlbumArt(
            uri = track.albumArtUri,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(BgTertiary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
