package com.example.nasonly.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["videoPath"])],
)
data class PlaybackHistory(
    @PrimaryKey val id: Long,
    val videoPath: String,
    val position: Long,
    val updatedAt: Long,
)
