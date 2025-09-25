package com.example.nasonly.data.db

import androidx.room.*

@Dao
interface ScanProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: ScanProgress): Long

    @Update
    suspend fun update(progress: ScanProgress)

    @Delete
    suspend fun delete(progress: ScanProgress)

    @Query("SELECT * FROM scan_progress WHERE id = :id")
    suspend fun getById(id: Int = 1): ScanProgress?

    @Query("SELECT * FROM scan_progress")
    suspend fun getAll(): List<ScanProgress>
}
