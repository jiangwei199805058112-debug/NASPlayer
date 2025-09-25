package com.example.nasonly.data.db

import androidx.room.*

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Update
    suspend fun update(video: VideoEntity)

    @Delete
    suspend fun delete(video: VideoEntity)

    @Query("SELECT * FROM video WHERE id = :id")
    suspend fun getById(id: Long): VideoEntity?

    @Query("SELECT * FROM video WHERE path = :path")
    suspend fun getByPath(path: String): VideoEntity?

    @Query("SELECT * FROM video")
    suspend fun getAll(): List<VideoEntity>
}
