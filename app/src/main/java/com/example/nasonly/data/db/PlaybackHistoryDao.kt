package com.example.nasonly.data.db

import androidx.room.*

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistory): Long

    @Update
    suspend fun update(history: PlaybackHistory)

    @Delete
    suspend fun delete(history: PlaybackHistory)

    @Query("SELECT * FROM playback_history WHERE id = :id")
    suspend fun getById(id: Long): PlaybackHistory?

    @Query("SELECT * FROM playback_history WHERE videoPath = :videoPath")
    suspend fun getByVideoPath(videoPath: String): List<PlaybackHistory>

    @Query("SELECT * FROM playback_history")
    suspend fun getAll(): List<PlaybackHistory>
}
