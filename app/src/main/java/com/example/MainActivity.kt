package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.screen.home.HomeScreen
import com.example.presentation.screen.library.LibraryScreen
import com.example.presentation.screen.nowplaying.NowPlayingScreen
import com.example.presentation.screen.playlist.PlaylistDetailScreen
import com.example.presentation.screen.search.SearchScreen
import com.example.presentation.theme.AppTheme
import com.example.presentation.theme.BgPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
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
                val showBottomBar = !isNowPlaying && (currentRoute == "home" || currentRoute == "library" || currentRoute == "search")
                val showMiniPlayer = !isNowPlaying

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            val targetRoute = targetState.destination.route ?: ""
                            val initialRoute = initialState.destination.route ?: ""
                            if (targetRoute.startsWith("now_playing")) {
                                slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(250))
                            } else if (initialRoute.startsWith("now_playing")) {
                                EnterTransition.None
                            } else {
                                fadeIn(animationSpec = tween(220)) +
                                        slideInHorizontally(
                                            initialOffsetX = { 100 },
                                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                                        )
                            }
                        },
                        exitTransition = {
                            val targetRoute = targetState.destination.route ?: ""
                            val initialRoute = initialState.destination.route ?: ""
                            if (targetRoute.startsWith("now_playing")) {
                                slideOutVertically(
                                    targetOffsetY = { 60 },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(250))
                            } else if (initialRoute.startsWith("now_playing")) {
                                ExitTransition.None
                            } else {
                                fadeOut(animationSpec = tween(220)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -100 },
                                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                                        )
                            }
                        },
                        popEnterTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            if (initialRoute.startsWith("now_playing")) {
                                slideInVertically(
                                    initialOffsetY = { 60 },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(250))
                            } else {
                                fadeIn(animationSpec = tween(220)) +
                                        slideInHorizontally(
                                            initialOffsetX = { -100 },
                                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                                        )
                            }
                        },
                        popExitTransition = {
                            val initialRoute = initialState.destination.route ?: ""
                            if (initialRoute.startsWith("now_playing")) {
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(250))
                            } else {
                                fadeOut(animationSpec = tween(220)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { 100 },
                                            animationSpec = tween(220, easing = FastOutSlowInEasing)
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
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(200)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
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
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
                            ) {
                                com.example.presentation.component.AppBottomNavigationBar(navController = navController)
                            }
                        }
                    }
                }
            }
        }
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

    AnimatedVisibility(
        visible = showMiniPlayer && currentTrack != null,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
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
