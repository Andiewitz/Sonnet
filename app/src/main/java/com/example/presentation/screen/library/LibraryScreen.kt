package com.example.presentation.screen.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SonnetApplication
import com.example.presentation.component.MiniPlayer
import com.example.presentation.component.TrackItem
import com.example.presentation.component.TrackItemSkeleton
import com.example.presentation.theme.*

@Composable
fun LibraryScreen(
    onTrackClick: (Long) -> Unit,
    navController: androidx.navigation.NavController
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.provideFactory(
            appContainer.getAllTracksUseCase,
            appContainer.getPlaylistsUseCase,
            appContainer.createPlaylistUseCase,
            appContainer.audioPlayer
        )
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }

    // Trigger scan on startup
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val coroutineScope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            coroutineScope.launch {
                appContainer.trackRepository.scanDeviceForTracks()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            appContainer.trackRepository.scanDeviceForTracks()
        } else {
            launcher.launch(permission)
        }
    }

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val allPlaylists = remember(tracks, playlists) {
        listOf(
            com.example.domain.model.Playlist(
                id = -1L,
                name = "All Songs",
                trackCount = tracks.size,
                artworkUris = tracks.mapNotNull { it.albumArtUri }.take(4)
            )
        ) + playlists
    }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
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
                        IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Playlist", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { isGridView = !isGridView }, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                            Icon(if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView, contentDescription = "Toggle View", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Your Library",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            if (isGridView) {
                val chunks = allPlaylists.chunked(2)
                items(chunks, key = { it.first().id }) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (playlist in chunk) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        navController.navigate("playlist/${playlist.id}?name=${android.net.Uri.encode(playlist.name)}")
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                com.example.presentation.component.PlaylistArtwork(
                                    artworkUris = playlist.artworkUris,
                                    isAllSongs = playlist.id == -1L,
                                    modifier = Modifier.size(100.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${playlist.trackCount} tracks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(allPlaylists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("playlist/${playlist.id}?name=${android.net.Uri.encode(playlist.name)}")
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.presentation.component.PlaylistArtwork(
                            artworkUris = playlist.artworkUris,
                            isAllSongs = playlist.id == -1L,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${playlist.trackCount} tracks",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Create Playlist", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName.trim())
                                newPlaylistName = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Create", color = AccentPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = BgSecondary
            )
        }
    }
}
