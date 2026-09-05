package com.example.presentation.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.SonnetApplication
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val coroutineScope = rememberCoroutineScope()
    
    val playlists by appContainer.getPlaylistsUseCase().collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSecondary
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
            
            HorizontalDivider(color = BorderSubtle)
            
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text(
                        text = "No custom playlists found. Create a playlist first in Your Library!",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        appContainer.addToPlaylistUseCase(playlist.id, track.id)
                                        Toast.makeText(context, "Added \"${track.title}\" to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
