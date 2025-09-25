package com.example.nasonly.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 播放列表数据访问对象
 */
@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?
    
    @Query("SELECT * FROM playlists WHERE name = :name")
    suspend fun getPlaylistByName(name: String): Playlist?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)
    
    @Query("UPDATE playlists SET item_count = (SELECT COUNT(*) FROM playlist_items WHERE playlist_id = :playlistId) WHERE id = :playlistId")
    suspend fun updatePlaylistItemCount(playlistId: Long)
    
    @Transaction
    suspend fun createPlaylistWithCount(playlist: Playlist): Long {
        val id = insertPlaylist(playlist)
        updatePlaylistItemCount(id)
        return id
    }
}
