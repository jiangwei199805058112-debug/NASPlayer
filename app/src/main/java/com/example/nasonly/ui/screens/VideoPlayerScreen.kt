package com.example.nasonly.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nasonly.ui.viewmodel.VideoPlayerViewModel
import com.example.nasonly.ui.viewmodel.VideoPlayerUiState
import com.example.nasonly.core.ui.components.ErrorDialog
import com.example.nasonly.core.ui.components.LoadingIndicator
import com.example.nasonly.ui.utils.ImmersiveFullscreenManager
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    uri: String,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    
    // 控制UI状态
    var showControls by remember { mutableStateOf(true) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var showSeekFeedback by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf("") }
    var seekFeedbackPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(uri) {
        viewModel.initializePlayer(uri)
    }

    // 进入沉浸式全屏模式
    LaunchedEffect(Unit) {
        ImmersiveFullscreenManager.enterFullscreen(activity)
    }

    // 自动隐藏控制条 - 5秒延迟
    LaunchedEffect(showControls, uiState.isPlaying) {
        if (showControls && uiState.isPlaying) {
            delay(5000) // 5秒后自动隐藏
            showControls = false
        }
    }

    // 播放/暂停图标自动隐藏
    LaunchedEffect(showPlayPauseIcon) {
        if (showPlayPauseIcon) {
            delay(800) // 0.8秒后隐藏
            showPlayPauseIcon = false
        }
    }

    // 快进/快退反馈自动隐藏
    LaunchedEffect(showSeekFeedback) {
        if (showSeekFeedback) {
            delay(1000) // 1秒后隐藏
            showSeekFeedback = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width
                        val tapX = offset.x
                        
                        when {
                            // 左侧1/3：快退
                            tapX < screenWidth * 0.33f -> {
                                viewModel.rewind()
                                seekFeedbackText = "⏪ -10s"
                                seekFeedbackPosition = 0.2f
                                showSeekFeedback = true
                            }
                            // 右侧1/3：快进  
                            tapX > screenWidth * 0.67f -> {
                                viewModel.fastForward()
                                seekFeedbackText = "⏩ +10s"
                                seekFeedbackPosition = 0.8f
                                showSeekFeedback = true
                            }
                            // 中间1/3：显示/隐藏控制条
                            else -> {
                                showControls = !showControls
                            }
                        }
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        val tapX = offset.x
                        
                        when {
                            // 左侧1/3：快退
                            tapX < screenWidth * 0.33f -> {
                                viewModel.rewind()
                                seekFeedbackText = "⏪ -10s"
                                seekFeedbackPosition = 0.2f
                                showSeekFeedback = true
                            }
                            // 右侧1/3：快进
                            tapX > screenWidth * 0.67f -> {
                                viewModel.fastForward()
                                seekFeedbackText = "⏩ +10s"
                                seekFeedbackPosition = 0.8f
                                showSeekFeedback = true
                            }
                            // 中间1/3：播放/暂停
                            else -> {
                                if (uiState.isPlaying) {
                                    viewModel.pauseAndSaveHistory()
                                } else {
                                    viewModel.play()
                                }
                                showPlayPauseIcon = true
                            }
                        }
                    }
                )
            }
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

        // 播放/暂停反馈图标
        AnimatedVisibility(
            visible = showPlayPauseIcon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // 快进/快退反馈
        AnimatedVisibility(
            visible = showSeekFeedback,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                if (seekFeedbackPosition < 0.5f) Alignment.CenterStart else Alignment.CenterEnd
            )
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = seekFeedbackText,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // 播放控制条 - 新设计
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EnhancedVideoPlayerControls(
                uiState = uiState,
                onSeek = viewModel::seekTo,
                onSpeedChange = viewModel::setPlaybackSpeed,
                onToggleFavorite = viewModel::toggleFavorite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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

    // 处理系统返回键和清理
    DisposableEffect(Unit) {
        onDispose {
            ImmersiveFullscreenManager.exitFullscreen(activity)
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
private fun EnhancedVideoPlayerControls(
    modifier: Modifier = Modifier,
    uiState: VideoPlayerUiState,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val speedOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    var showSpeedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 进度条
            EnhancedVideoProgressBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                onSeek = onSeek
            )
            
            // 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：当前时间
                Text(
                    text = formatTime(uiState.currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // 中间：控制按钮组
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 倍速按钮
                    Box {
                        TextButton(
                            onClick = { showSpeedMenu = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "${uiState.playbackSpeed}×",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            speedOptions.forEach { speed ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "${speed}×",
                                            fontWeight = if (speed == uiState.playbackSpeed) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSpeedChange(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 收藏按钮
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (uiState.isFavorited) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // 右侧：总时长
                Text(
                    text = formatTime(uiState.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
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
    onFullscreenToggle: () -> Unit = {},
    onAddToFavorite: () -> Unit = {}
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 收藏按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ElevatedButton(
                onClick = onAddToFavorite,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("一键收藏到播放列表")
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

@Composable
private fun EnhancedVideoProgressBar(
    modifier: Modifier = Modifier,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDragging) dragPosition else progress,
        animationSpec = tween(durationMillis = 100),
        label = "progress"
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 8.dp)
        ) {
            // 背景轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .background(
                        Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // 进度轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // 拖拽滑块
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = (animatedProgress * 264).dp) // 使用计算出的像素值
                    .align(Alignment.CenterStart)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .border(
                        2.dp,
                        Color.White,
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { 
                                isDragging = true 
                            },
                            onDragEnd = { 
                                isDragging = false
                                onSeek((dragPosition * duration).toLong())
                            }
                        ) { _, dragAmount ->
                            val newPosition = (dragPosition + dragAmount.x / size.width).coerceIn(0f, 1f)
                            dragPosition = newPosition
                        }
                    }
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
