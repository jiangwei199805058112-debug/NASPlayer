package com.example.nasonly.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 播放列表项数据实体
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["video_path"])
    ]
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    
    @ColumnInfo(name = "video_path")
    val videoPath: String,
    
    @ColumnInfo(name = "video_name")
    val videoName: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,
    
    @ColumnInfo(name = "duration")
    val duration: Long = 0,
    
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)