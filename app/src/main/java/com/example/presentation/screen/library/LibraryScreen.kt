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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Cast
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
            appContainer.audioPlayer
        )
    )

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
    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
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
                Text(
                    text = "Your Library",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            if (tracks.isEmpty()) {
                items(6) {
                    TrackItemSkeleton()
                    HorizontalDivider(
                        color = BorderSubtle,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(tracks.size) { index ->
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 50L)
                        isVisible = true
                    }
                    val track = tracks[index]
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 })
                    ) {
                        Column {
                            TrackItem(track = track, onClick = { 
                                viewModel.audioPlayer.setPlaylist(tracks, index)
                            })
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
}
