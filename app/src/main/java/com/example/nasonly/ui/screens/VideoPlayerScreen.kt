package com.example.nasonly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nasonly.ui.viewmodel.VideoPlayerViewModel
import com.example.nasonly.ui.viewmodel.VideoPlayerUiState
import com.example.nasonly.core.ui.components.ErrorDialog
import com.example.nasonly.core.ui.components.LoadingIndicator
import com.example.nasonly.core.ui.components.VideoGestureOverlay
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    uri: String,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    
    LaunchedEffect(uri) {
        viewModel.initializePlayer(uri)
    }

    // 自动隐藏控制条
    LaunchedEffect(showControls, uiState.isPlaying) {
        if (showControls && uiState.isPlaying) {
            delay(3000) // 3秒后自动隐藏
            showControls = false
        }
    }

    // 全屏模式处理
    LaunchedEffect(uiState.isFullscreen) {
        // 这里应该调用Activity的全屏设置
        // 目前仅作为状态处理
    }

    VideoGestureOverlay(
        modifier = Modifier.fillMaxSize(),
        onVolumeChange = { volume -> viewModel.setVolume(volume) },
        onBrightnessChange = { brightness -> viewModel.setBrightness(brightness) },
        onSeek = { position -> viewModel.seekTo(position) },
        currentPosition = uiState.currentPosition,
        duration = uiState.duration
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showControls = !showControls }
        ) {
            // 视频播放器区域
            VideoPlayerView(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState
            )

            // 缓冲指示器
            if (uiState.isBuffering) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 播放控制条
            if (showControls) {
                VideoPlayerControls(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    uiState = uiState,
                    onPlayPause = {
                        if (uiState.isPlaying) {
                            viewModel.pause()
                        } else {
                            viewModel.play()
                        }
                    },
                    onSeek = viewModel::seekTo,
                    onFastForward = { viewModel.fastForward() },
                    onRewind = { viewModel.rewind() },
                    onFullscreenToggle = { viewModel.toggleFullscreen() }
                )
            }

            // 错误对话框
            val errorMessage = uiState.error
            if (errorMessage != null) {
                ErrorDialog(
                    message = errorMessage,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }

    // 处理系统返回键
    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }
}

@Composable
private fun VideoPlayerView(
    modifier: Modifier = Modifier,
    uiState: VideoPlayerUiState
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 这里将来可以集成真实的 ExoPlayer PlayerView
        // 目前显示占位内容
        if (!uiState.isBuffering && uiState.error == null) {
            Text(
                text = "视频播放区域\n${if (uiState.isPlaying) "播放中" else "已暂停"}",
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VideoPlayerControls(
    modifier: Modifier = Modifier,
    uiState: VideoPlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
    onFullscreenToggle: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.7f)
            )
            .padding(16.dp)
    ) {
        // 进度条
        VideoProgressBar(
            currentPosition = uiState.currentPosition,
            duration = uiState.duration,
            onSeek = onSeek
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 快退按钮
            IconButton(
                onClick = onRewind,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = "快退10秒",
                    tint = Color.White
                )
            }
            
            // 播放/暂停按钮
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // 快进按钮
            IconButton(
                onClick = onFastForward,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.Forward10,
                    contentDescription = "快进10秒",
                    tint = Color.White
                )
            }
        }
        
        // 时间显示和全屏按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(uiState.currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            
            IconButton(
                onClick = onFullscreenToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (uiState.isFullscreen) "退出全屏" else "全屏",
                    tint = Color.White
                )
            }
            
            Text(
                text = formatTime(uiState.duration),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    val progress = if (duration > 0) {
        if (isDragging) dragPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Slider(
            value = progress,
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((dragPosition * duration).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
