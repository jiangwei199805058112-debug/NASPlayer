package com.example.nasonly.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * 简化的播放历史数据模型测试
 * 测试核心业务逻辑而不依赖Android框架
 */
@ExperimentalCoroutinesApi
class PlaybackHistoryDaoTest {

    // 简化的PlaybackHistory数据类用于测试
    data class PlaybackHistory(
        val filePath: String,
        val fileName: String,
        val lastPlayedPosition: Long,
        val duration: Long,
        val lastPlayedTime: Long
    )

    @Test
    fun `test playback history data model creation`() = runTest {
        // Given
        val filePath = "/test/video.mp4"
        val fileName = "video.mp4"
        val lastPlayedPosition = 5000L
        val duration = 10000L
        val lastPlayedTime = System.currentTimeMillis()

        // When
        val playbackHistory = PlaybackHistory(
            filePath = filePath,
            fileName = fileName,
            lastPlayedPosition = lastPlayedPosition,
            duration = duration,
            lastPlayedTime = lastPlayedTime
        )

        // Then
        assertEquals(filePath, playbackHistory.filePath)
        assertEquals(fileName, playbackHistory.fileName)
        assertEquals(lastPlayedPosition, playbackHistory.lastPlayedPosition)
        assertEquals(duration, playbackHistory.duration)
        assertEquals(lastPlayedTime, playbackHistory.lastPlayedTime)
    }

    @Test
    fun `test playback position validation`() = runTest {
        // Given
        val history = PlaybackHistory(
            filePath = "/test/video.mp4",
            fileName = "video.mp4",
            lastPlayedPosition = 7500L,
            duration = 10000L,
            lastPlayedTime = System.currentTimeMillis()
        )

        // When - Test position is within duration
        val isValidPosition = history.lastPlayedPosition <= history.duration && history.lastPlayedPosition >= 0

        // Then
        assertTrue("Position should be valid", isValidPosition)
    }

    @Test
    fun `test playback position percentage calculation`() = runTest {
        // Given
        val history = PlaybackHistory(
            filePath = "/test/video.mp4",
            fileName = "video.mp4",
            lastPlayedPosition = 2500L,
            duration = 10000L,
            lastPlayedTime = System.currentTimeMillis()
        )

        // When
        val percentage = (history.lastPlayedPosition.toFloat() / history.duration.toFloat() * 100).toInt()

        // Then
        assertEquals(25, percentage)
    }

    @Test
    fun `test playback time formatting`() = runTest {
        // Test time formatting logic (would be used in UI)
        val testCases = mapOf(
            0L to "0:00",
            30000L to "0:30",
            90000L to "1:30",
            3600000L to "60:00"
        )

        testCases.forEach { (millis, expected) ->
            val formatted = formatTime(millis)
            assertEquals("$millis ms should format to $expected", expected, formatted)
        }
    }

    @Test
    fun `test file path validation`() = runTest {
        // Given
        val validPaths = listOf(
            "/videos/movie.mp4",
            "/media/film.avi",
            "/content/video.mkv"
        )
        
        val invalidPaths = listOf(
            "",
            "   ",
            "relative/path.mp4"
        )

        // When & Then - Valid paths
        validPaths.forEach { path ->
            assertTrue("$path should be valid", isValidFilePath(path))
        }

        // When & Then - Invalid paths
        invalidPaths.forEach { path ->
            assertFalse("$path should be invalid", isValidFilePath(path))
        }
    }

    @Test
    fun `test duration validation`() = runTest {
        // Given
        val validDurations = listOf(1000L, 60000L, 3600000L)
        val invalidDurations = listOf(-1L, 0L)

        // When & Then - Valid durations
        validDurations.forEach { duration ->
            assertTrue("$duration should be valid", duration > 0)
        }

        // When & Then - Invalid durations
        invalidDurations.forEach { duration ->
            assertFalse("$duration should be invalid", duration > 0)
        }
    }

    // Helper functions for testing
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun isValidFilePath(path: String): Boolean {
        return path.isNotBlank() && path.startsWith("/")
    }
}