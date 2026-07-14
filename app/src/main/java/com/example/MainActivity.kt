package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.screen.library.LibraryScreen
import com.example.presentation.screen.nowplaying.NowPlayingScreen
import com.example.presentation.screen.home.HomeScreen
import com.example.presentation.theme.AppTheme

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
        val showBottomBar = currentRoute in listOf("home", "library", "search")
        val showMiniPlayer = currentRoute in listOf("home", "search")

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = com.example.presentation.theme.BgPrimary,
          bottomBar = {
              if (showBottomBar) {
                  com.example.presentation.component.AppBottomNavigationBar(navController = navController)
              }
          }
        ) { innerPadding ->
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
          ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { 300 },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -300 },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -300 },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { 300 },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
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
              composable(
                  route = "now_playing/{trackId}",
                  enterTransition = {
                      androidx.compose.animation.slideInVertically(
                          initialOffsetY = { it },
                          animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                      )
                  },
                  exitTransition = {
                      androidx.compose.animation.slideOutVertically(
                          targetOffsetY = { it },
                          animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                      )
                  }
              ) { backStackEntry ->
                val trackId = backStackEntry.arguments?.getString("trackId")?.toLongOrNull() ?: -1L
                NowPlayingScreen(
                  trackId = trackId,
                  onBackClick = { navController.popBackStack() }
                )
              }
              composable("search") {
                com.example.presentation.screen.search.SearchScreen(navController = navController)
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
                  com.example.presentation.screen.playlist.PlaylistDetailScreen(
                      playlistId = playlistId,
                      playlistName = name,
                      navController = navController
                  )
              }
            }
            
            MainMiniPlayer(
                audioPlayer = audioPlayer,
                navController = navController,
                showMiniPlayer = showMiniPlayer,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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

    androidx.compose.animation.AnimatedVisibility(
        visible = showMiniPlayer && currentTrack != null,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
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
