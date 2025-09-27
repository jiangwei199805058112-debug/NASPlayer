package com.example.nasonly.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_progress")
data class ScanProgress(
    @PrimaryKey val id: Int = 1,
    val running: Boolean,
    val lastScanAt: Long,
)
