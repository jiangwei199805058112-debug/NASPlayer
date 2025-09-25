package com.example.nasonly.data.db

import androidx.room.*

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE name = :name")
    suspend fun getByName(name: String): PlaylistEntity?

    @Query("SELECT * FROM playlist")
    suspend fun getAll(): List<PlaylistEntity>
}
