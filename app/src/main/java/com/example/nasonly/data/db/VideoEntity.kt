package com.example.nasonly.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "video",
    indices = [Index(value = ["path"], unique = true)]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val name: String,
    val duration: Long,
    val size: Long,
    val lastModified: Long
)
