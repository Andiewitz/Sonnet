package com.example.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.presentation.theme.BgTertiary
import com.example.presentation.theme.TextTertiary

@Composable
fun AlbumArt(uri: String?, modifier: Modifier = Modifier) {
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.background(BgTertiary),
        contentAlignment = Alignment.Center
    ) {
        if (uri.isNullOrBlank() || isError || isLoading) {
            Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = TextTertiary)
        }
        
        if (!uri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Art",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    isError = state is AsyncImagePainter.State.Error
                    isLoading = state is AsyncImagePainter.State.Loading
                }
            )
        }
    }
}
