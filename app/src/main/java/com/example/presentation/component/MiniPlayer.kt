package com.example.presentation.component

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Track
import com.example.presentation.theme.*
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayer(
    track: Track, 
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "mini_player_card_scale"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = BgSecondary
        ),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Crossfade
                Crossfade(
                    targetState = track.albumArtUri,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "mini_art_crossfade"
                ) { uri ->
                    AlbumArt(
                        uri = uri,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Animated Track Info Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = track.title to track.artist,
                        transitionSpec = {
                            (fadeIn(androidx.compose.animation.core.tween(200)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(androidx.compose.animation.core.tween(200)) + slideOutVertically { -it / 2 })
                        },
                        label = "mini_player_text_anim"
                    ) { (title, artist) ->
                        Column {
                            Text(
                                text = title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // Play / Pause Button with Spring Feedback
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    val playScale by animateFloatAsState(
                        targetValue = if (isPlayPressed) 0.85f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "play_pause_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer(scaleX = playScale, scaleY = playScale)
                            .clip(CircleShape)
                            .background(AccentPrimary)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = ripple(),
                                onClick = onPlayPauseClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
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
            
            // Isolated Progress Bar - only this sub-composable collects position and recomposes
            MiniPlayerStateFlowProgressBar(
                durationMs = track.durationMs,
                positionFlow = positionFlow,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

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
    MiniPlayer(
        track = track,
        isPlaying = isPlaying,
        progressProvider = { progress },
        onPlayPauseClick = onPlayPauseClick,
        modifier = modifier
    )
}

@Composable
fun MiniPlayer(
    track: Track, 
    isPlaying: Boolean,
    progressProvider: () -> Float,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "mini_player_card_scale"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = BgSecondary
        ),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(
                    targetState = track.albumArtUri,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "mini_art_crossfade"
                ) { uri ->
                    AlbumArt(
                        uri = uri,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = track.title to track.artist,
                        transitionSpec = {
                            (fadeIn(androidx.compose.animation.core.tween(200)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(androidx.compose.animation.core.tween(200)) + slideOutVertically { -it / 2 })
                        },
                        label = "mini_player_text_anim"
                    ) { (title, artist) ->
                        Column {
                            Text(
                                text = title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    val playScale by animateFloatAsState(
                        targetValue = if (isPlayPressed) 0.85f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "play_pause_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer(scaleX = playScale, scaleY = playScale)
                            .clip(CircleShape)
                            .background(AccentPrimary)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = ripple(),
                                onClick = onPlayPauseClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
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
            
            MiniPlayerProgressBar(
                progressProvider = progressProvider,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun MiniPlayerStateFlowProgressBar(
    durationMs: Long,
    positionFlow: StateFlow<Long>,
    modifier: Modifier = Modifier
) {
    val currentPosition by positionFlow.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0) (currentPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(100, easing = androidx.compose.animation.core.LinearEasing),
        label = "mini_player_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(BgTertiary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(3.dp)
                .background(AccentPrimary)
        )
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val progress = progressProvider()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(100, easing = androidx.compose.animation.core.LinearEasing),
        label = "mini_player_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(BgTertiary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(3.dp)
                .background(AccentPrimary)
        )
    }
}
