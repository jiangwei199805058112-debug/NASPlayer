package com.example.nasonly.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PlaybackHistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var playbackHistoryDao: PlaybackHistoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        playbackHistoryDao = database.playbackHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetPlaybackHistory() = runTest {
        // Given
        val playbackHistory = PlaybackHistory(
            id = 1L,
            videoPath = "/test/video1.mp4",
            position = 30000L,
            updatedAt = System.currentTimeMillis()
        )

        // When
        playbackHistoryDao.insert(playbackHistory)
        val retrievedHistory = playbackHistoryDao.getByVideoPath("/test/video1.mp4")

        // Then
        assertEquals(1, retrievedHistory.size)
        assertEquals(playbackHistory.videoPath, retrievedHistory[0].videoPath)
        assertEquals(playbackHistory.position, retrievedHistory[0].position)
    }

    @Test
    fun updatePlaybackHistory() = runTest {
        // Given
        val originalHistory = PlaybackHistory(
            id = 1L,
            videoPath = "/test/video2.mp4",
            position = 15000L,
            updatedAt = System.currentTimeMillis()
        )
        playbackHistoryDao.insert(originalHistory)

        // When
        val updatedHistory = originalHistory.copy(
            position = 45000L,
            updatedAt = System.currentTimeMillis()
        )
        playbackHistoryDao.update(updatedHistory)
        val retrievedHistory = playbackHistoryDao.getByVideoPath("/test/video2.mp4")

        // Then
        assertEquals(1, retrievedHistory.size)
        assertEquals(45000L, retrievedHistory[0].position)
    }

    @Test
    fun getAllPlaybackHistory() = runTest {
        // Given
        val history1 = PlaybackHistory(1L, "/video1.mp4", 10000L, System.currentTimeMillis())
        val history2 = PlaybackHistory(2L, "/video2.mp4", 20000L, System.currentTimeMillis())
        val history3 = PlaybackHistory(3L, "/video3.mp4", 30000L, System.currentTimeMillis())

        // When
        playbackHistoryDao.insert(history1)
        playbackHistoryDao.insert(history2)
        playbackHistoryDao.insert(history3)
        val allHistory = playbackHistoryDao.getAll()

        // Then
        assertEquals(3, allHistory.size)
        assertTrue(allHistory.any { it.videoPath == "/video1.mp4" })
        assertTrue(allHistory.any { it.videoPath == "/video2.mp4" })
        assertTrue(allHistory.any { it.videoPath == "/video3.mp4" })
    }

    @Test
    fun deletePlaybackHistory() = runTest {
        // Given
        val history = PlaybackHistory(1L, "/delete_test.mp4", 5000L, System.currentTimeMillis())
        playbackHistoryDao.insert(history)

        // Verify it exists
        var allHistory = playbackHistoryDao.getAll()
        assertEquals(1, allHistory.size)

        // When
        playbackHistoryDao.delete(history)
        allHistory = playbackHistoryDao.getAll()

        // Then
        assertEquals(0, allHistory.size)
    }

    @Test
    fun deleteAllPlaybackHistory() = runTest {
        // Given - Insert multiple records
        val history1 = PlaybackHistory(1L, "/video1.mp4", 10000L, System.currentTimeMillis())
        val history2 = PlaybackHistory(2L, "/video2.mp4", 20000L, System.currentTimeMillis())
        playbackHistoryDao.insert(history1)
        playbackHistoryDao.insert(history2)

        // Verify they exist
        var allHistory = playbackHistoryDao.getAll()
        assertEquals(2, allHistory.size)

        // When
        playbackHistoryDao.deleteAll()
        allHistory = playbackHistoryDao.getAll()

        // Then
        assertEquals(0, allHistory.size)
    }

    @Test
    fun getByVideoPathReturnsEmptyForNonExistent() = runTest {
        // When
        val result = playbackHistoryDao.getByVideoPath("/nonexistent.mp4")

        // Then
        assertTrue(result.isEmpty())
    }
}