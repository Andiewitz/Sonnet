package com.example.presentation.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.component.TrackItem
import com.example.domain.model.Track
import com.example.presentation.theme.*

data class Genre(val name: String, val color: Color)

val topGenres = listOf(
    Genre("Hip-Hop", TileAmber),
    Genre("Summer", TileGarnet),
    Genre("Pop", TileDustyRose),
    Genre("Electronic", TileIndigo)
)

val browseAllGenres = listOf(
    Genre("Romantic", TileTeal),
    Genre("Bollywood", TilePlum),
    Genre("Indie", TileOlive),
    Genre("Reggae", TileSienna),
    Genre("Workout", TileGarnet),
    Genre("Sleep", TileIndigo)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item(span = { GridItemSpan(2) }) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text("Artists, songs or podcasts", color = TextSecondary, style = MaterialTheme.typography.bodyLarge) 
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BgSecondary,
                        unfocusedContainerColor = BgSecondary,
                        disabledContainerColor = BgSecondary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentPrimary
                    ),
                    singleLine = true
                )
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Your top genres",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
            }

            items(topGenres) { genre ->
                GenreCard(genre)
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Browse all",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
            }

            items(browseAllGenres) { genre ->
                GenreCard(genre)
            }
        }
    }
}

@Composable
fun GenreCard(genre: Genre) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(genre.color)
    ) {
        Text(
            text = genre.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = TextOnAccent,
            modifier = Modifier.padding(16.dp)
        )
        
        // Rotated square to mimic album art
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 8.dp)
                .size(72.dp)
                .rotate(20f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = TextOnAccent.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Center).size(36.dp)
            )
        }
    }
}
