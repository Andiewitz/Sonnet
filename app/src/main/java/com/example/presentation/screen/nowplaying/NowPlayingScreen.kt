package com.example.presentation.screen.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SonnetApplication
import com.example.presentation.component.AlbumArt
import com.example.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    trackId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as SonnetApplication).container
    val viewModel: NowPlayingViewModel = viewModel(
        factory = NowPlayingViewModel.provideFactory(appContainer.getTrackByIdUseCase, appContainer.audioPlayer)
    )
    
    LaunchedEffect(trackId) {
        if (trackId != -1L) {
            viewModel.loadTrack(trackId)
        }
    }
    
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val track by viewModel.currentTrack.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    
    var isLiked by remember(track?.id) { mutableStateOf(false) }
    
    val duration = track?.durationMs ?: 1L
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }

    // Dynamic color simulation based on track id
    val targetBgColor = remember(track?.id) {
        val hash = track?.title?.hashCode() ?: 0
        val hue = (Math.abs(hash) % 360).toFloat()
        Color.hsv(hue, 0.4f, 0.15f)
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = androidx.compose.animation.core.tween(1000),
        label = "bg_color_animation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(animatedBgColor, BgPrimary),
                    startY = 0f,
                    endY = 1000f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Minimize", tint = TextPrimary)
            }
            Text(
                text = "NOW PLAYING",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showEqualizer = true }) {
                    com.example.presentation.component.EqualizerIcon(
                        tint = AccentPrimary,
                        size = 22.dp
                    )
                }
                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextPrimary)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // Artwork Scale based on playing state (expands when playing, shrinks slightly when paused)
        val artworkScale by animateFloatAsState(
            targetValue = if (isPlaying) 1.0f else 0.90f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "artwork_scale"
        )

        // Album Art with Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer(scaleX = artworkScale, scaleY = artworkScale),
            contentAlignment = Alignment.Center
        ) {
            val glowColor = remember(track?.id) {
                val hash = track?.artist?.hashCode() ?: 0
                val hue = (Math.abs(hash) % 360).toFloat()
                Color.hsv(hue, 0.8f, 0.8f).copy(alpha = 0.18f)
            }
            val animatedGlowColor by animateColorAsState(
                targetValue = glowColor,
                animationSpec = androidx.compose.animation.core.tween(1000),
                label = "glow_color_animation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(animatedGlowColor, Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            
            Crossfade(
                targetState = track?.albumArtUri,
                animationSpec = androidx.compose.animation.core.tween(600),
                label = "album_art_crossfade"
            ) { uri ->
                AlbumArt(
                    uri = uri,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // Animated Track Info Title & Artist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = (track?.title ?: "Unknown Track") to (track?.artist ?: "Unknown Artist"),
                    transitionSpec = {
                        (fadeIn(androidx.compose.animation.core.tween(300)) + slideInVertically { it / 2 }) togetherWith
                        (fadeOut(androidx.compose.animation.core.tween(300)) + slideOutVertically { -it / 2 })
                    },
                    label = "track_info_transition"
                ) { (title, artist) ->
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.displayLarge,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.25f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "like_scale"
                )
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier.graphicsLayer(scaleX = likeScale, scaleY = likeScale)
                ) {
                    Crossfade(targetState = isLiked, label = "like_icon_crossfade") { liked ->
                        Icon(
                            imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (liked) AccentPrimary else TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { /* Share */ }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress Bar
        NowPlayingProgressBar(duration = duration, viewModel = viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls with Spring Button Feedback
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleModeEnabled) AccentPrimary else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            val prevInteraction = remember { MutableInteractionSource() }
            val isPrevPressed by prevInteraction.collectIsPressedAsState()
            val prevScale by animateFloatAsState(
                targetValue = if (isPrevPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "prev_scale"
            )
            IconButton(
                onClick = { viewModel.skipToPrevious() },
                interactionSource = prevInteraction,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer(scaleX = prevScale, scaleY = prevScale)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = TextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (isPlayPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "play_scale"
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer(scaleX = playScale, scaleY = playScale)
                    .clip(CircleShape)
                    .background(AccentPrimary)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = ripple(),
                        onClick = { viewModel.togglePlayPause() }
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
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            val nextInteraction = remember { MutableInteractionSource() }
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val nextScale by animateFloatAsState(
                targetValue = if (isNextPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "next_scale"
            )
            IconButton(
                onClick = { viewModel.skipToNext() },
                interactionSource = nextInteraction,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer(scaleX = nextScale, scaleY = nextScale)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            val isLoopOnce = repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE
            val repeatIcon = if (isLoopOnce) Icons.Filled.RepeatOne else Icons.Filled.Repeat
            val repeatDesc = if (isLoopOnce) "Loop Once" else "Loop All"
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = repeatDesc,
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(0.5f))
    }

    if (showAddToPlaylistDialog && track != null) {
        com.example.presentation.component.AddToPlaylistDialog(
            track = track!!,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showEqualizer) {
        com.example.presentation.component.EqualizerBottomSheet(
            equalizerManager = appContainer.audioPlayer.equalizerManager,
            onDismiss = { showEqualizer = false }
        )
    }
}

@Composable
fun NowPlayingProgressBar(
    duration: Long,
    viewModel: NowPlayingViewModel
) {
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val sliderPosition by remember(duration) { 
        derivedStateOf { if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f }
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = sliderPosition,
        animationSpec = androidx.compose.animation.core.tween(100, easing = androidx.compose.animation.core.LinearEasing),
        label = "progress_bar_animation"
    )

    Column {
        Slider(
            value = animatedProgress,
            onValueChange = { viewModel.seekTo((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = TextPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = BorderStrong
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currSeconds = (currentPosition / 1000) % 60
            val currMinutes = (currentPosition / 1000) / 60
            val durSeconds = (duration / 1000) % 60
            val durMinutes = (duration / 1000) / 60
            Text(String.format("%d:%02d", currMinutes, currSeconds), color = TextTertiary, style = MaterialTheme.typography.labelMedium)
            Text(String.format("%d:%02d", durMinutes, durSeconds), color = TextTertiary, style = MaterialTheme.typography.labelMedium)
        }
    }
}
