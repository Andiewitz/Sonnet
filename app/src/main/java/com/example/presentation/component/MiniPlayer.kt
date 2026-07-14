package com.example.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.domain.model.Track
import com.example.presentation.theme.*

import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.togetherWith

@Composable
fun MiniPlayer(
    track: Track, 
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = track.durationMs
    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = BgSecondary
        ),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                AlbumArt(
                    uri = track.albumArtUri,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    Text(
                        text = track.artist,
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
                
                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isPressed) 0.85f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
                        label = "play_pause_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clip(CircleShape)
                            .background(AccentPrimary)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(),
                                onClick = onPlayPauseClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn() togetherWith 
                                androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                            },
                            label = "play_pause_icon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Progress Bar
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = progress,
                animationSpec = androidx.compose.animation.core.tween(100, easing = androidx.compose.animation.core.LinearEasing),
                label = "mini_player_progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(BgTertiary)
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(3.dp)
                        .background(AccentPrimary)
                )
            }
        }
    }
}
