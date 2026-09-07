package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isNowPlaying = currentRoute?.startsWith("now_playing") == true
                val showBottomBar = !isNowPlaying && (currentRoute == "home" || currentRoute == "library" || currentRoute == "search" || currentRoute == "settings")
                val showMiniPlayer = !isNowPlaying

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary)
                ) {
                    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            val targetRoute = targetState.destination.route ?: ""
                            val initialRoute = initialState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                    (targetRoute in listOf("home", "library", "search", "settings"))

                            if (targetRoute.startsWith("now_playing")) {
                                slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(440, easing = EmphasizedDecelerateEasing)
                                ) + fadeIn(animationSpec = tween(260))
                            } else if (initialRoute.startsWith("now_playing")) {
                                EnterTransition.None
                            } else if (isTabSwitch) {
                                fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = LinearOutSlowInEasing))
                            } else {
                                fadeIn(animationSpec = tween(260)) +
                                        slideInHorizontally(
                                            initialOffsetX = { (it * 0.25f).toInt() },
                                            animationSpec = tween(360, easing = EmphasizedDecelerateEasing)
                                        )
                            }
                        },
                        exitTransition = {
                            val targetRoute = targetState.destination.route ?: ""
                            val initialRoute = initialState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                    (targetRoute in listOf("home", "library", "search", "settings"))

                            if (targetRoute.startsWith("now_playing")) {
                                scaleOut(
                                    targetScale = 0.93f,
                                    animationSpec = tween(440, easing = EmphasizedDecelerateEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            } else if (initialRoute.startsWith("now_playing")) {
                                ExitTransition.None
                            } else if (isTabSwitch) {
                                fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(160, easing = FastOutLinearInEasing))
                            } else {
                                fadeOut(animationSpec = tween(200)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -(it * 0.15f).toInt() },
                                            animationSpec = tween(360, easing = EmphasizedDecelerateEasing)
                                        )
                            }
                        },
                        popEnterTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            val targetRoute = targetState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                    (targetRoute in listOf("home", "library", "search", "settings"))

                            if (initialRoute.startsWith("now_playing")) {
                                scaleIn(
                                    initialScale = 0.93f,
                                    animationSpec = tween(380, easing = EmphasizedDecelerateEasing)
                                ) + fadeIn(animationSpec = tween(320))
                            } else if (isTabSwitch) {
                                fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = LinearOutSlowInEasing))
                            } else {
                                fadeIn(animationSpec = tween(260)) +
                                        slideInHorizontally(
                                            initialOffsetX = { -(it * 0.15f).toInt() },
                                            animationSpec = tween(360, easing = EmphasizedDecelerateEasing)
                                        )
                            }
                        },
                        popExitTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            val targetRoute = targetState.destination.route ?: ""
                            val isTabSwitch = (initialRoute in listOf("home", "library", "search", "settings")) &&
                                    (targetRoute in listOf("home", "library", "search", "settings"))

                            if (initialRoute.startsWith("now_playing")) {
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(380, easing = EmphasizedAccelerateEasing)
                                ) + fadeOut(animationSpec = tween(240))
                            } else if (isTabSwitch) {
                                fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                                        scaleOut(targetScale = 0.98f, animationSpec = tween(160, easing = FastOutLinearInEasing))
                            } else {
                                fadeOut(animationSpec = tween(220)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { (it * 0.25f).toInt() },
                                            animationSpec = tween(360, easing = EmphasizedDecelerateEasing)
                                        )
                            }
                        }
                    ) {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("library") {
                            LibraryScreen(
                                onTrackClick = { trackId -> navController.navigate("now_playing/$trackId") },
                                navController = navController
                            )
                        }
                        composable("search") {
                            SearchScreen(navController = navController)
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
                        ) { backStackEntry ->
                            val trackId = backStackEntry.arguments?.getString("trackId")?.toLongOrNull() ?: -1L
                            NowPlayingScreen(
                                trackId = trackId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }

                    // Bottom UI Overlay (MiniPlayer + NavigationBar)
                    AnimatedVisibility(
                        visible = !isNowPlaying,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(400, easing = EmphasizedDecelerateEasing)
                        ) + fadeIn(tween(260)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(340, easing = EmphasizedAccelerateEasing)
                        ) + fadeOut(tween(200)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MainMiniPlayer(
                                audioPlayer = audioPlayer,
                                navController = navController,
                                showMiniPlayer = showMiniPlayer
                            )

                            AnimatedVisibility(
                                visible = showBottomBar,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(340, easing = EmphasizedDecelerateEasing)
                                ) + fadeIn(tween(220)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(280, easing = EmphasizedAccelerateEasing)
                                ) + fadeOut(tween(180))
                            ) {
                                com.example.presentation.component.AppBottomNavigationBar(navController = navController)
                            }
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
    navController: androidx.navigation.NavController,
    showMiniPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val currentTrack by audioPlayer.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by audioPlayer.currentPosition.collectAsStateWithLifecycle()

    val EmphasizedDecelerateEasing = remember { CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) }
    val EmphasizedAccelerateEasing = remember { CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f) }

    AnimatedVisibility(
        visible = showMiniPlayer && currentTrack != null,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(360, easing = EmphasizedDecelerateEasing)
        ) + fadeIn(tween(240)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = EmphasizedAccelerateEasing)
        ) + fadeOut(tween(180))
    ) {
        currentTrack?.let { track ->
            com.example.presentation.component.MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                onPlayPauseClick = { audioPlayer.togglePlayPause() },
                modifier = Modifier.clickable { navController.navigate("now_playing/${track.id}") }
            )
        }
    }
}
