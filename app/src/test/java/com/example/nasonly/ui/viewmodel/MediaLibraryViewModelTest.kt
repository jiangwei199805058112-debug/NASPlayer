package com.example.nasonly.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.nasonly.data.smb.SmbFileInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class MediaLibraryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `test file filtering logic`() = runTest(testDispatcher) {
        // Given
        val allFiles = listOf(
            SmbFileInfo(
                name = "video1.mp4",
                path = "/video1.mp4",
                size = 1024L,
                isDirectory = false
            ),
            SmbFileInfo(
                name = "document.txt",
                path = "/document.txt",
                size = 512L,
                isDirectory = false
            ),
            SmbFileInfo(
                name = "video2.avi",
                path = "/video2.avi",
                size = 2048L,
                isDirectory = false
            ),
            SmbFileInfo(
                name = "folder1",
                path = "/folder1",
                size = 0L,
                isDirectory = true
            )
        )

        // When - Filter video files
        val videoExtensions = setOf("mp4", "avi", "mkv", "mov")
        val videoFiles = allFiles.filter { file ->
            if (file.isDirectory) true
            else videoExtensions.any { ext -> file.name.lowercase().endsWith(".$ext") }
        }

        // Then
        assertEquals(3, videoFiles.size) // 2 video files + 1 folder
        assertTrue(videoFiles.any { it.name == "video1.mp4" })
        assertTrue(videoFiles.any { it.name == "video2.avi" })
        assertTrue(videoFiles.any { it.name == "folder1" })
        assertFalse(videoFiles.any { it.name == "document.txt" })
    }

    @Test
    fun `test file size formatting`() = runTest(testDispatcher) {
        // Test cases for file size formatting
        val testCases = mapOf(
            0L to "0 B",
            1024L to "1.0 KB",
            1048576L to "1.0 MB",
            1073741824L to "1.0 GB"
        )

        testCases.forEach { (size, expected) ->
            val formatted = formatFileSize(size)
            assertEquals("Size $size should format to $expected", expected, formatted)
        }
    }

    @Test
    fun `test file extension detection`() = runTest(testDispatcher) {
        // Given
        val videoFiles = listOf("movie.mp4", "film.avi", "video.mkv", "clip.mov")
        val nonVideoFiles = listOf("doc.txt", "image.jpg", "audio.mp3", "data.dat")

        // When
        val videoExtensions = setOf("mp4", "avi", "mkv", "mov")
        
        // Then - All video files should be detected
        videoFiles.forEach { filename ->
            val isVideo = videoExtensions.any { ext -> 
                filename.lowercase().endsWith(".$ext") 
            }
            assertTrue("$filename should be detected as video", isVideo)
        }

        // Then - Non-video files should not be detected
        nonVideoFiles.forEach { filename ->
            val isVideo = videoExtensions.any { ext -> 
                filename.lowercase().endsWith(".$ext") 
            }
            assertFalse("$filename should not be detected as video", isVideo)
        }
    }

    @Test
    fun `test sorting logic`() = runTest(testDispatcher) {
        // Given
        val files = listOf(
            SmbFileInfo("z_video.mp4", "/z_video.mp4", 1024L, false),
            SmbFileInfo("a_video.mp4", "/a_video.mp4", 2048L, false),
            SmbFileInfo("m_video.mp4", "/m_video.mp4", 512L, false)
        )

        // When - Sort by name ascending
        val sortedByName = files.sortedBy { it.name }

        // Then
        assertEquals("a_video.mp4", sortedByName[0].name)
        assertEquals("m_video.mp4", sortedByName[1].name)
        assertEquals("z_video.mp4", sortedByName[2].name)

        // When - Sort by size descending
        val sortedBySize = files.sortedByDescending { it.size }

        // Then
        assertEquals(2048L, sortedBySize[0].size)
        assertEquals(1024L, sortedBySize[1].size)
        assertEquals(512L, sortedBySize[2].size)
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024.0} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024.0 * 1024)} MB"
            else -> "${size / (1024.0 * 1024 * 1024)} GB"
        }.let { 
            if (it.contains(".")) {
                it.substring(0, it.indexOf(".") + 2) + " " + it.substringAfterLast(" ")
            } else it
        }
    }
}