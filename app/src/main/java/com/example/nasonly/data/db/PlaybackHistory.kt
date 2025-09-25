package com.example.nasonly.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["videoPath"])]
)
data class PlaybackHistory(
    @PrimaryKey val id: Long,
    val videoPath: String,
    val position: Long,
    val updatedAt: Long
)
