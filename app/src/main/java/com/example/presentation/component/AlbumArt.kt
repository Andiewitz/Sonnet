package com.example.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.presentation.theme.BgTertiary
import com.example.presentation.theme.TextTertiary

@Composable
fun AlbumArt(uri: String?, modifier: Modifier = Modifier) {
    val fallback = @Composable {
        Box(
            modifier = modifier.background(BgTertiary),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = TextTertiary)
        }
    }

    if (!uri.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = "Album Art",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { fallback() },
            error = { fallback() }
        )
    } else {
        fallback()
    }
}
