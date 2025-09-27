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
                navController.navigate("media/${host.host}?share=${host.share}") {
                    popUpTo("startup") { inclusive = true }
                }
            }
            StartupViewModel.StartupState.GoToDiscovery -> {
                navController.navigate(Routes.NAS_DISCOVERY) {
                    popUpTo("startup") { inclusive = true }
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
