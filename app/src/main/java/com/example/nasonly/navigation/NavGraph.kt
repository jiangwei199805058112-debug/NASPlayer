package com.example.nasonly.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.nasonly.ui.screens.NasConfigScreen
import com.example.nasonly.ui.screens.MediaLibraryScreen
import com.example.nasonly.ui.screens.VideoPlayerScreen

@Composable
fun NavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.NAS_CONFIG) {
            NasConfigScreen(navController)
        }
        composable(Routes.MEDIA_LIBRARY) {
            MediaLibraryScreen(navController)
        }
        composable(
            route = Routes.VIDEO_PLAYER,
            arguments = listOf(
                navArgument("uri") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
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
