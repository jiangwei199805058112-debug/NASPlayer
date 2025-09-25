package com.example.nasonly.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nasonly.ui.screens.VideoPlayerScreen
// 请根据实际路径补充其它 Screen 的 import
// import com.example.nasonly.ui.screens.NasConfigScreen
// import com.example.nasonly.ui.screens.MediaLibraryScreen
// import com.example.nasonly.ui.screens.PlaylistScreen

@Composable
fun NavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.NAS_CONFIG) {
            // NasConfigScreen()
        }
        composable(Routes.MEDIA_LIBRARY) {
            // MediaLibraryScreen()
        }
        composable(Routes.PLAYLIST) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
            // PlaylistScreen(playlistId = playlistId)
        }
        composable(Routes.VIDEO_PLAYER) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            VideoPlayerScreen(uri = Uri.decode(uri))
        }
    }
}

fun NavHostController.navigateToVideoPlayer(smbPath: String) {
    this.navigate("video_player?uri=${Uri.encode(smbPath)}")
}

fun NavHostController.goBack() {
    this.popBackStack()
}
