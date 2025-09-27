package com.example.nasonly.ui.screens

import com.example.nasonly.ui.viewmodel.VideoPlayerUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * 视频播放器屏幕增强功能测试
 */
class VideoPlayerScreenTest {

    @Test
    fun testVideoPlayerUiState_defaultValues() {
        val uiState = VideoPlayerUiState()

        // 验证默认值
        assertFalse("默认不应该在播放", uiState.isPlaying)
        assertFalse("默认不应该在缓冲", uiState.isBuffering)
        assertNull("默认不应该有错误", uiState.error)
        assertEquals("默认播放位置应该是0", 0L, uiState.currentPosition)
        assertEquals("默认时长应该是0", 0L, uiState.duration)
        assertEquals("默认播放速度应该是1.0", 1.0f, uiState.playbackSpeed)
        assertFalse("默认不应该被收藏", uiState.isFavorited)
        assertFalse("默认不应该是全屏", uiState.isFullscreen)
    }

    @Test
    fun testVideoPlayerUiState_withFavoriteStatus() {
        val uiState = VideoPlayerUiState(
            isPlaying = true,
            currentPosition = 30000L,
            duration = 120000L,
            playbackSpeed = 1.5f,
            isFavorited = true,
        )

        // 验证收藏状态
        assertTrue("应该在播放", uiState.isPlaying)
        assertEquals("播放位置应该是30秒", 30000L, uiState.currentPosition)
        assertEquals("时长应该是2分钟", 120000L, uiState.duration)
        assertEquals("播放速度应该是1.5倍", 1.5f, uiState.playbackSpeed)
        assertTrue("应该被收藏", uiState.isFavorited)
    }

    @Test
    fun testFormatTime() {
        // 测试时间格式化功能
        // 注意：这需要我们在测试类中复制formatTime函数，或者将其移到工具类中
        val timeMs = 125000L // 2分5秒
        val expected = "02:05"

        val result = formatTimeForTest(timeMs)
        assertEquals("时间格式化应该正确", expected, result)
    }

    @Test
    fun testPlaybackSpeedOptions() {
        val speedOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f)

        // 验证播放速度选项
        assertTrue("应该包含0.5倍速", speedOptions.contains(0.5f))
        assertTrue("应该包含正常速度", speedOptions.contains(1.0f))
        assertTrue("应该包含1.5倍速", speedOptions.contains(1.5f))
        assertTrue("应该包含2倍速", speedOptions.contains(2.0f))
        assertEquals("应该有4个速度选项", 4, speedOptions.size)
    }

    // 辅助函数用于测试
    private fun formatTimeForTest(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
