package com.example.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.presentation.theme.AccentPrimary
import com.example.presentation.theme.BgTertiary
import com.example.presentation.theme.TextTertiary

@Composable
fun PlaylistArtwork(
    artworkUris: List<String?>,
    modifier: Modifier = Modifier,
    isAllSongs: Boolean = false
) {
    val validUris = artworkUris.filter { !it.isNullOrBlank() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAllSongs) AccentPrimary else BgTertiary),
        contentAlignment = Alignment.Center
    ) {
        if (isAllSongs && validUris.size < 4) {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        } else if (validUris.size >= 4) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AlbumArt(uri = validUris[0], modifier = Modifier.weight(1f).fillMaxHeight())
                    AlbumArt(uri = validUris[1], modifier = Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AlbumArt(uri = validUris[2], modifier = Modifier.weight(1f).fillMaxHeight())
                    AlbumArt(uri = validUris[3], modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        } else if (validUris.isNotEmpty()) {
            AlbumArt(
                uri = validUris.first(),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
