package com.example.nasonly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 深色主题色彩配置
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6), // 紫色主色调
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color.White,
    
    secondary = Color(0xFFA78BFA),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF7C3AED),
    onSecondaryContainer = Color.White,
    
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color.Black,
    
    background = Color(0xFF0F0F23), // 深蓝黑色背景
    onBackground = Color.White,
    
    surface = Color(0xFF1E1B4B), // 深紫色表面
    onSurface = Color.White,
    surfaceVariant = Color(0xFF312E81),
    onSurfaceVariant = Color(0xFFE2E8F0),
    
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF475569),
    
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFDC2626),
    onErrorContainer = Color.White
)

// 保留浅色主题（虽然不使用）
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    
    secondary = Color(0xFFA78BFA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDD6FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color.White,
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun NASPlayerTheme(
    content: @Composable () -> Unit
) {
    // 强制使用深色主题
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}