package com.example.nasonly.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nasonly.ui.discovery.NasDiscoveryScreen
import com.example.nasonly.ui.screens.EnhancedSettingsScreen
import com.example.nasonly.ui.screens.MediaLibraryScreen
import com.example.nasonly.ui.screens.NasConfigScreen
import com.example.nasonly.ui.screens.PlaylistDetailScreen
import com.example.nasonly.ui.screens.PlaylistManagementScreen
import com.example.nasonly.ui.screens.VideoPlayerScreen

@Composable
fun NavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.NAS_DISCOVERY) {
            NasDiscoveryScreen(
                onDeviceConnected = { _ ->
                    // 连接成功后导航到媒体库
                    navController.navigate(Routes.MEDIA_LIBRARY)
                },
            )
        }
        composable(Routes.NAS_CONFIG) {
            NasConfigScreen(navController)
        }
        composable(Routes.SETTINGS) {
            EnhancedSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MEDIA_LIBRARY) {
            MediaLibraryScreen(navController)
        }
        composable(Routes.PLAYLIST_MANAGEMENT) {
            PlaylistManagementScreen(navController)
        }
        composable(
            route = Routes.PLAYLIST_DETAIL,
            arguments = listOf(
                navArgument("playlistId") {
                    type = NavType.LongType
                },
            ),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                playlistName = "播放列表", // 简化处理，实际应该从数据库获取
                onNavigateBack = { navController.popBackStack() },
                onPlayVideo = { videoPath ->
                    val encodedUri = java.net.URLEncoder.encode(videoPath, "UTF-8")
                    navController.navigate("video_player?uri=$encodedUri")
                },
                onAddVideos = {
                    // TODO: 实现添加视频功能
                },
            )
        }
        composable(
            route = Routes.VIDEO_PLAYER,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
            ),
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

fun NavHostController.navigateToPlaylistManagement() {
    this.navigate(Routes.PLAYLIST_MANAGEMENT)
}

fun NavHostController.navigateToPlaylistDetail(playlistId: Long) {
    this.navigate("playlist_detail/$playlistId")
}
