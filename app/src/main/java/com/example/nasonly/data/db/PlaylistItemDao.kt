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
 * 播放列表项数据访问对象
 */
@Dao
interface PlaylistItemDao {

    @Query("SELECT * FROM playlist_items WHERE playlist_id = :playlistId ORDER BY order_index ASC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE id = :id")
    suspend fun getPlaylistItemById(id: Long): PlaylistItem?

    @Query("SELECT * FROM playlist_items WHERE playlist_id = :playlistId AND video_path = :videoPath")
    suspend fun getPlaylistItemByPath(playlistId: Long, videoPath: String): PlaylistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItem>)

    @Update
    suspend fun updatePlaylistItem(item: PlaylistItem)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE id = :id")
    suspend fun deletePlaylistItemById(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun deleteAllPlaylistItems(playlistId: Long)

    @Query("UPDATE playlist_items SET order_index = :newIndex WHERE id = :itemId")
    suspend fun updateItemOrder(itemId: Long, newIndex: Int)

    @Query("SELECT MAX(order_index) FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Transaction
    suspend fun addItemToPlaylist(
        playlistId: Long,
        videoPath: String,
        videoName: String,
        fileSize: Long = 0,
        duration: Long = 0,
    ) {
        val maxIndex = getMaxOrderIndex(playlistId) ?: 0
        val newItem = PlaylistItem(
            playlistId = playlistId,
            videoPath = videoPath,
            videoName = videoName,
            fileSize = fileSize,
            duration = duration,
            orderIndex = maxIndex + 1,
        )
        insertPlaylistItem(newItem)
    }

    @Transaction
    suspend fun reorderPlaylistItems(playlistId: Long, itemIds: List<Long>) {
        itemIds.forEachIndexed { index, itemId ->
            updateItemOrder(itemId, index)
        }
    }
}
