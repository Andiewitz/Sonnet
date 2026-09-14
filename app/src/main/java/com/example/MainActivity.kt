package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.screen.album.AlbumDetailScreen
import com.example.presentation.screen.artist.ArtistDetailScreen
import com.example.presentation.screen.home.HomeScreen
import com.example.presentation.screen.library.LibraryScreen
import com.example.presentation.screen.nowplaying.NowPlayingScreen
import com.example.presentation.screen.playlist.PlaylistDetailScreen
import com.example.presentation.screen.search.SearchScreen
import com.example.presentation.screen.settings.SettingsScreen
import com.example.presentation.theme.AppTheme
import com.example.presentation.theme.BgPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (permissions[audioPermission] == true) {
            triggerMediaScan()
        }
    }

    private fun triggerMediaScan() {
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val appContainer = (applicationContext as SonnetApplication).container
                    appContainer.trackRepository.scanDeviceForTracks()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error auto-scanning songs: ${e.message}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val neededPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(audioPermission)
        }

        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        } else {
            triggerMediaScan()
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                val navController = rememberNavController()

                val appContainer = (applicationContext as SonnetApplication).container
                val audioPlayer = appContainer.audioPlayer

                val currentTrack by audioPlayer.currentTrack.collectAsStateWithLifecycle()
                var isNowPlayingOpen by rememberSaveable { mutableStateOf(false) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isNowPlaying = isNowPlayingOpen
                val showBottomBar = !isNowPlaying && (currentRoute == "home" || currentRoute == "library" || currentRoute == "search" || currentRoute == "settings")

                BackHandler(enabled = isNowPlayingOpen) {
                    isNowPlayingOpen = false
                }

                val SnappyDecelerateEasing = remember { CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f) }
                val SnappyAccelerateEasing = remember { CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f) }

                val backgroundScale by animateFloatAsState(
                    targetValue = if (isNowPlayingOpen) 0.96f else 1.0f,
                    animationSpec = tween(
                        durationMillis = if (isNowPlayingOpen) 250 else 220,
                        easing = if (isNowPlayingOpen) SnappyDecelerateEasing else SnappyAccelerateEasing
                    ),
                    label = "background_scale"
                )
                val scrimAlpha by animateFloatAsState(
                    targetValue = if (isNowPlayingOpen) 0.45f else 0.0f,
                    animationSpec = tween(
                        durationMillis = if (isNowPlayingOpen) 250 else 220,
                        easing = if (isNowPlayingOpen) SnappyDecelerateEasing else SnappyAccelerateEasing
                    ),
                    label = "scrim_alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary)
                ) {
                    // Main Navigation Content (subtly scales back with 0-cost GPU matrix transform)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = backgroundScale
                                scaleY = backgroundScale
                            }
                    ) {
                        NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            val targetRoute = targetState.destination.route ?: ""
                            val initialRoute = initialState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                    (targetRoute in listOf("home", "library", "search", "settings"))

                            if (isTabSwitch) {
                                fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = LinearOutSlowInEasing))
                            } else {
                                fadeIn(animationSpec = tween(220)) +
                                        slideInHorizontally(
                                            initialOffsetX = { (it * 0.25f).toInt() },
                                            animationSpec = tween(260, easing = SnappyDecelerateEasing)
                                        )
                            }
                        },
                        exitTransition = {
                            val targetRoute = targetState.destination.route ?: ""
                            val initialRoute = initialState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                     (targetRoute in listOf("home", "library", "search", "settings"))

                            if (isTabSwitch) {
                                fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(160, easing = FastOutLinearInEasing))
                            } else {
                                fadeOut(animationSpec = tween(180)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -(it * 0.15f).toInt() },
                                            animationSpec = tween(260, easing = SnappyDecelerateEasing)
                                        )
                            }
                        },
                        popEnterTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            val targetRoute = targetState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                     (targetRoute in listOf("home", "library", "search", "settings"))

                            if (isTabSwitch) {
                                fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = LinearOutSlowInEasing))
                            } else {
                                fadeIn(animationSpec = tween(220)) +
                                        slideInHorizontally(
                                            initialOffsetX = { -(it * 0.15f).toInt() },
                                            animationSpec = tween(260, easing = SnappyDecelerateEasing)
                                        )
                            }
                        },
                        popExitTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            val targetRoute = targetState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                     (targetRoute in listOf("home", "library", "search", "settings"))

                            if (isTabSwitch) {
                                fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(160, easing = FastOutLinearInEasing))
                            } else {
                                fadeOut(animationSpec = tween(180)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { (it * 0.25f).toInt() },
                                            animationSpec = tween(260, easing = SnappyDecelerateEasing)
                                        )
                            }
                        }
                    ) {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("library") {
                            LibraryScreen(
                                onTrackClick = { isNowPlayingOpen = true },
                                navController = navController
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                navController = navController,
                                onTrackClick = { isNowPlayingOpen = true }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }
                        composable(
                            route = "playlist/{playlistId}?name={name}",
                            arguments = listOf(
                                androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.LongType },
                                androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType; defaultValue = "Playlist" }
                            )
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: -1L
                            val name = backStackEntry.arguments?.getString("name") ?: "Playlist"
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistName = name,
                                navController = navController
                            )
                        }
                        composable(
                            route = "album/{albumTitle}",
                            arguments = listOf(
                                androidx.navigation.navArgument("albumTitle") { type = androidx.navigation.NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val albumTitle = backStackEntry.arguments?.getString("albumTitle") ?: ""
                            AlbumDetailScreen(
                                albumTitle = albumTitle,
                                navController = navController
                            )
                        }
                        composable(
                            route = "artist/{artistName}",
                            arguments = listOf(
                                androidx.navigation.navArgument("artistName") { type = androidx.navigation.NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                            ArtistDetailScreen(
                                artistName = artistName,
                                navController = navController
                            )
                        }
                        composable(
                            route = "now_playing/{trackId}"
                        ) {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                                isNowPlayingOpen = true
                            }
                        }
                    }
                    }

                    // Scrim overlay over recessed background
                    if (scrimAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = scrimAlpha }
                                .background(Color.Black)
                        )
                    }

                    // Bottom UI Overlay (MiniPlayer + NavigationBar)
                    AnimatedVisibility(
                        visible = !isNowPlaying,
                        enter = fadeIn(tween(160, delayMillis = 40)),
                        exit = fadeOut(tween(100)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MainMiniPlayer(
                                audioPlayer = audioPlayer,
                                onOpenNowPlaying = { isNowPlayingOpen = true }
                            )

                            AnimatedVisibility(
                                visible = showBottomBar,
                                enter = expandVertically(
                                    animationSpec = tween(220, easing = SnappyDecelerateEasing)
                                ) + fadeIn(tween(160)),
                                exit = shrinkVertically(
                                    animationSpec = tween(180, easing = SnappyAccelerateEasing)
                                ) + fadeOut(tween(120))
                            ) {
                                com.example.presentation.component.AppBottomNavigationBar(navController = navController)
                            }
                        }
                    }

                    // Fullscreen Now Playing Overlay with drop-down exit animation
                    AnimatedVisibility(
                        visible = isNowPlayingOpen && currentTrack != null,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(250, easing = SnappyDecelerateEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(220, easing = SnappyAccelerateEasing)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        currentTrack?.let { track ->
                            NowPlayingScreen(
                                trackId = track.id,
                                onBackClick = { isNowPlayingOpen = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        triggerMediaScan()
    }
}

@Composable
fun MainMiniPlayer(
    audioPlayer: com.example.player.AudioPlayer,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by audioPlayer.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by audioPlayer.isPlaying.collectAsStateWithLifecycle()

    val EmphasizedDecelerateEasing = remember { CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) }
    val EmphasizedAccelerateEasing = remember { CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f) }

    AnimatedVisibility(
        visible = currentTrack != null,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        enter = expandVertically(
            animationSpec = tween(280, easing = EmphasizedDecelerateEasing)
        ) + fadeIn(tween(200)),
        exit = shrinkVertically(
            animationSpec = tween(240, easing = EmphasizedAccelerateEasing)
        ) + fadeOut(tween(160))
    ) {
        currentTrack?.let { track ->
            com.example.presentation.component.MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                positionFlow = audioPlayer.currentPosition,
                onPlayPauseClick = { audioPlayer.togglePlayPause() },
                modifier = Modifier.clickable { onOpenNowPlaying() }
            )
        }
    }
}
