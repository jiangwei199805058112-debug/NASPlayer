package com.example.nasonly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nasonly.ui.viewmodel.VideoPlayerViewModel

@Composable
fun VideoPlayerScreen(
    uri: String,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uri) {
        viewModel.play(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 视频播放器区域（可扩展为 ExoPlayer Compose）
        Spacer(modifier = Modifier.height(200.dp))

        if (uiState.isBuffering) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (uiState.isPlaying) viewModel.pause() else viewModel.play(uri)
            }) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                )
            }
            // 进度条与拖动（可扩展）
            LinearProgressIndicator(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        }

        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("播放错误") },
                text = { Text(uiState.error ?: "") },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) { Text("确定") }
                }
            )
        }
    }
}
