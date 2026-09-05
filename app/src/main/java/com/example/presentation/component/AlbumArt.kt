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
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.presentation.theme.BgTertiary
import com.example.presentation.theme.TextTertiary

@Composable
fun AlbumArt(uri: String?, modifier: Modifier = Modifier) {
    var isError by remember(uri) { mutableStateOf(false) }
    var isLoading by remember(uri) { mutableStateOf(!uri.isNullOrBlank()) }

    Box(
        modifier = modifier.background(BgTertiary),
        contentAlignment = Alignment.Center
    ) {
        if (uri.isNullOrBlank() || isError || isLoading) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = TextTertiary
            )
        }
        
        if (!uri.isNullOrBlank()) {
            val context = LocalContext.current
            val imageRequest = remember(uri) {
                ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(150)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .precision(Precision.INEXACT) // Allows downscaling efficiently without exact pixel overhead
                    .scale(Scale.FILL)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
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

