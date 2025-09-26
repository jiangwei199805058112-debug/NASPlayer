package com.example.nasonly.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun VideoGestureOverlay(
    modifier: Modifier = Modifier,
    onVolumeChange: (Float) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onSeek: (Long) -> Unit = {},
    currentPosition: Long = 0L,
    duration: Long = 0L,
    content: @Composable () -> Unit
) {
    var gestureState by remember { mutableStateOf(GestureState.None) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureValue by remember { mutableStateOf(0f) }
    var gestureType by remember { mutableStateOf(GestureType.None) }
    
    // 自动隐藏手势指示器
    LaunchedEffect(showGestureIndicator) {
        if (showGestureIndicator) {
            delay(1000)
            showGestureIndicator = false
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x
                        val screenWidthPx = screenWidth.toPx()
                        
                        gestureState = when {
                            // 左侧区域：亮度控制
                            x < screenWidthPx * 0.3f -> GestureState.Brightness
                            // 右侧区域：音量控制
                            x > screenWidthPx * 0.7f -> GestureState.Volume
                            // 中间区域：进度控制
                            else -> GestureState.Seek
                        }
                        
                        gestureType = when (gestureState) {
                            GestureState.Brightness -> GestureType.Brightness
                            GestureState.Volume -> GestureType.Volume
                            GestureState.Seek -> GestureType.Seek
                            else -> GestureType.None
                        }
                        
                        showGestureIndicator = true
                    },
                    onDragEnd = {
                        gestureState = GestureState.None
                        showGestureIndicator = false
                    }
                ) { _, dragAmount ->
                    val deltaY = -dragAmount.y / 1000f // 向上为正值
                    val deltaX = dragAmount.x / 1000f // 向右为正值
                    
                    when (gestureState) {
                        GestureState.Volume -> {
                            gestureValue = (gestureValue + deltaY).coerceIn(0f, 1f)
                            onVolumeChange(gestureValue)
                        }
                        GestureState.Brightness -> {
                            gestureValue = (gestureValue + deltaY).coerceIn(0f, 1f)
                            onBrightnessChange(gestureValue)
                        }
                        GestureState.Seek -> {
                            if (duration > 0) {
                                val seekDelta = (deltaX * duration / 10f).toLong()
                                val newPosition = (currentPosition + seekDelta).coerceIn(0L, duration)
                                onSeek(newPosition)
                                gestureValue = newPosition.toFloat() / duration.toFloat()
                            }
                        }
                        else -> {}
                    }
                }
            }
    ) {
        content()
        
        // 手势指示器
        if (showGestureIndicator && gestureType != GestureType.None) {
            GestureIndicator(
                gestureType = gestureType,
                value = gestureValue,
                currentPosition = currentPosition,
                duration = duration
            )
        }
    }
}

@Composable
private fun GestureIndicator(
    gestureType: GestureType,
    value: Float,
    currentPosition: Long,
    duration: Long
) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (gestureType) {
                    GestureType.Volume -> {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "音量",
                            tint = Color.White
                        )
                        Text(
                            text = "音量",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${(value * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    GestureType.Brightness -> {
                        Icon(
                            Icons.Default.Brightness6,
                            contentDescription = "亮度",
                            tint = Color.White
                        )
                        Text(
                            text = "亮度",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${(value * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    GestureType.Seek -> {
                        Text(
                            text = "进度调整",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    GestureType.None -> {}
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private enum class GestureState {
    None,
    Volume,
    Brightness,
    Seek
}

private enum class GestureType {
    None,
    Volume,
    Brightness,
    Seek
}