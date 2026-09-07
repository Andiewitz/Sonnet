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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SonnetApplication
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.presentation.component.AlbumArt
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
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Playlists") } // "Playlists", "Albums", "Artists"

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

    val allAlbums = remember(tracks) {
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
    }

    val allArtists = remember(tracks) {
        tracks.groupBy { it.artist }
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
    }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
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
                        if (selectedCategory == "Playlists") {
                            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Playlist", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        android.widget.Toast.makeText(context, "Scanning for songs...", android.widget.Toast.LENGTH_SHORT).show()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            appContainer.trackRepository.scanDeviceForTracks()
                                        }
                                        android.widget.Toast.makeText(context, "Library updated", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Scan failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Rescan Media", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { isGridView = !isGridView }, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
                            Icon(if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView, contentDescription = "Toggle View", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { navController.navigate("settings") }, modifier = Modifier.size(36.dp).background(BgTertiary, CircleShape)) {
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Category Filter Pills: Playlists, Albums, Artists
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Playlists", "Albums", "Artists").forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BgSecondary,
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = TextOnAccent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BorderSubtle,
                                selectedBorderColor = AccentPrimary
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSecondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LibraryMusic,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Songs Found",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Songs are automatically detected, or you can tap below to scan your device storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            android.widget.Toast.makeText(context, "Scanning for songs...", android.widget.Toast.LENGTH_SHORT).show()
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                appContainer.trackRepository.scanDeviceForTracks()
                                            }
                                            android.widget.Toast.makeText(context, "Library updated", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Scan failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Device for Songs")
                            }
                        }
                    }
                }
            }

            when (selectedCategory) {
                "Playlists" -> {
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
                                            modifier = Modifier.size(110.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                "Albums" -> {
                    if (allAlbums.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No albums found", color = TextSecondary)
                            }
                        }
                    } else if (isGridView) {
                        val chunks = allAlbums.chunked(2)
                        items(chunks, key = { it.first().title }) { chunk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for (album in chunk) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navController.navigate("album/${android.net.Uri.encode(album.title)}")
                                            }
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AlbumArt(
                                            uri = album.artworkUri,
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${album.artist} • ${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(allAlbums, key = { it.title }) { album ->
                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("album/${android.net.Uri.encode(album.title)}")
                                    }
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArt(
                                    uri = album.artworkUri,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${album.artist} • ${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                "Artists" -> {
                    if (allArtists.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No artists found", color = TextSecondary)
                            }
                        }
                    } else if (isGridView) {
                        val chunks = allArtists.chunked(2)
                        items(chunks, key = { it.first().name }) { chunk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for (artist in chunk) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navController.navigate("artist/${android.net.Uri.encode(artist.name)}")
                                            }
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(CircleShape)
                                                .background(BgTertiary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (artist.artworkUri != null) {
                                                AlbumArt(
                                                    uri = artist.artworkUri,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(54.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${artist.trackCount} tracks • ${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(allArtists, key = { it.name }) { artist ->
                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("artist/${android.net.Uri.encode(artist.name)}")
                                    }
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(BgTertiary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artist.artworkUri != null) {
                                        AlbumArt(
                                            uri = artist.artworkUri,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${artist.trackCount} tracks • ${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
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
