package com.example.nasonly.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Builder

@Database(
    entities = [
        VideoEntity::class,
        PlaybackHistory::class,
        PlaylistEntity::class,
        ScanProgress::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun scanProgressDao(): ScanProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueriesIfDebug()
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private fun Builder<AppDatabase>.allowMainThreadQueriesIfDebug(): Builder<AppDatabase> {
            return if (isDebug()) this.allowMainThreadQueries() else this
        }

        private fun isDebug(): Boolean {
            return try {
                Class.forName("com.example.nasonly.BuildConfig")
                    .getField("DEBUG")
                    .getBoolean(null)
            } catch (e: Exception) {
                false
            }
        }
    }
}
