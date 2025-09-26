package com.example.nasonly.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import com.example.nasonly.data.smb.SmbFileInfo

/**
 * 简单的Android集成测试
 * 测试基本的应用功能和数据结构
 */
@RunWith(AndroidJUnit4::class)
class SimpleIntegrationTest {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.nasonly", appContext.packageName)
    }

    @Test
    fun testSmbFileInfoDataClass() {
        // Test the SmbFileInfo data class
        val fileInfo = SmbFileInfo(
            name = "test_video.mp4",
            path = "/media/test_video.mp4",
            size = 1024000L,
            isDirectory = false
        )

        assertEquals("test_video.mp4", fileInfo.name)
        assertEquals("/media/test_video.mp4", fileInfo.path)
        assertEquals(1024000L, fileInfo.size)
        assertFalse(fileInfo.isDirectory)
    }

    @Test
    fun testVideoFileExtensionDetection() {
        val testFiles = listOf(
            SmbFileInfo("movie.mp4", "/movies/movie.mp4", 1024L, false),
            SmbFileInfo("video.mkv", "/videos/video.mkv", 2048L, false),
            SmbFileInfo("film.avi", "/films/film.avi", 3072L, false),
            SmbFileInfo("document.txt", "/docs/document.txt", 100L, false),
            SmbFileInfo("folder", "/folder", 0L, true)
        )

        val videoFiles = testFiles.filter { 
            !it.isDirectory && it.name.lowercase().run {
                endsWith(".mp4") || endsWith(".mkv") || endsWith(".avi") || 
                endsWith(".mov") || endsWith(".wmv") || endsWith(".flv")
            }
        }

        assertEquals(3, videoFiles.size)
        assertTrue(videoFiles.all { !it.isDirectory })
    }

    @Test
    fun testFileSizeFormatting() {
        val testCases = mapOf(
            512L to "512 B",
            1024L to "1.0 KB", 
            1536L to "1.5 KB",
            1048576L to "1.0 MB",
            1073741824L to "1.0 GB"
        )

        testCases.forEach { (size, expected) ->
            val formatted = formatFileSize(size)
            assertTrue("Size $size should contain expected format", 
                formatted.contains(expected.split(" ")[0]))
        }
    }

    @Test
    fun testMediaLibraryUiState() {
        // Test the UI state data structure
        data class MediaLibraryUiState(
            val isLoading: Boolean = false,
            val files: List<SmbFileInfo> = emptyList(),
            val error: String? = null,
            val currentPath: String = ""
        )

        // 初始状态
        var state = MediaLibraryUiState()
        assertFalse(state.isLoading)
        assertTrue(state.files.isEmpty())
        assertNull(state.error)

        // 加载状态
        state = state.copy(isLoading = true)
        assertTrue(state.isLoading)

        // 成功加载
        val files = listOf(
            SmbFileInfo("video1.mp4", "/video1.mp4", 1024L, false),
            SmbFileInfo("video2.mkv", "/video2.mkv", 2048L, false)
        )
        state = state.copy(isLoading = false, files = files)
        assertFalse(state.isLoading)
        assertEquals(2, state.files.size)

        // 错误状态
        state = state.copy(error = "连接失败")
        assertEquals("连接失败", state.error)

        // 清除错误
        state = state.copy(error = null)
        assertNull(state.error)
    }

    /**
     * 文件大小格式化函数
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.1f", size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format("%.1f", size / (1024.0 * 1024))} MB"
            else -> "${String.format("%.1f", size / (1024.0 * 1024 * 1024))} GB"
        }
    }
}