package com.example.nasonly.ui.startup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.navigation.Routes
import com.example.nasonly.ui.startup.StartupViewModel
import timber.log.Timber

@Composable
fun StartupScreen(
    navController: NavController,
    viewModel: StartupViewModel = hiltViewModel(),
) {
    val startupState by viewModel.startupState.collectAsState()

    LaunchedEffect(startupState) {
        when (startupState) {
            is StartupViewModel.StartupState.AutoConnect -> {
                val host = (startupState as StartupViewModel.StartupState.AutoConnect).host
                try {
                    navController.navigate("media/${host.host}?share=${host.share}") {
                        popUpTo("startup") { inclusive = true }
                    }
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Failed to navigate to media library: ${host.host}")
                    // 回退到发现屏幕
                    try {
                        navController.navigate("nas_discovery") {
                            popUpTo("startup") { inclusive = true }
                        }
                    } catch (navException: IllegalArgumentException) {
                        Timber.e(navException, "Failed to navigate to discovery screen")
                    }
                }
            }
            StartupViewModel.StartupState.GoToDiscovery -> {
                try {
                    navController.navigate("nas_discovery") {
                        popUpTo("startup") { inclusive = true }
                    }
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Failed to navigate to discovery screen")
                }
            }
            StartupViewModel.StartupState.Checking -> {
                // 继续显示加载屏幕
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "正在检查上次连接...",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
