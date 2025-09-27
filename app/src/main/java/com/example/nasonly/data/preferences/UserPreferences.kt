package com.example.nasonly.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * 用户偏好设置数据存储
 */
class UserPreferences @Inject constructor(private val context: Context) {

    // 播放器设置键
    companion object {
        // 播放器设置
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")

        // 系统设置
        val CACHE_SIZE_MB = intPreferencesKey("cache_size_mb")
        val NETWORK_TIMEOUT_SECONDS = intPreferencesKey("network_timeout_seconds")
        val BUFFER_SIZE_KB = intPreferencesKey("buffer_size_kb")
        val MAX_BUFFER_MS = intPreferencesKey("max_buffer_ms")
        val MIN_BUFFER_MS = intPreferencesKey("min_buffer_ms")
        val AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
        val WIFI_ONLY_STREAMING = booleanPreferencesKey("wifi_only_streaming")

        // UI设置
        val DARK_MODE = stringPreferencesKey("dark_mode") // "auto", "light", "dark"
        val SHOW_THUMBNAILS = booleanPreferencesKey("show_thumbnails")
        val GRID_LAYOUT = booleanPreferencesKey("grid_layout")
        val SORT_ORDER = stringPreferencesKey("sort_order") // "name", "date", "size"
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")

        // NAS连接设置
        val LAST_NAS_IP = stringPreferencesKey("last_nas_ip")
        val LAST_USERNAME = stringPreferencesKey("last_username")

        // 默认值
        const val DEFAULT_PLAYBACK_SPEED = 1.0f
        const val DEFAULT_CACHE_SIZE_MB = 200
        const val DEFAULT_NETWORK_TIMEOUT_SECONDS = 30
        const val DEFAULT_BUFFER_SIZE_KB = 64
        const val DEFAULT_MAX_BUFFER_MS = 30000
        const val DEFAULT_MIN_BUFFER_MS = 2500
        const val DEFAULT_DARK_MODE = "auto"
        const val DEFAULT_SORT_ORDER = "name"
    }

    // 播放器设置
    val playbackSpeed: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[PLAYBACK_SPEED] ?: DEFAULT_PLAYBACK_SPEED }

    val autoPlayNext: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_PLAY_NEXT] ?: false }

    val resumePlayback: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[RESUME_PLAYBACK] ?: true }

    val screenOrientation: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SCREEN_ORIENTATION] ?: "auto" }

    // 系统设置
    val cacheSizeMB: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[CACHE_SIZE_MB] ?: DEFAULT_CACHE_SIZE_MB }

    val networkTimeoutSeconds: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[NETWORK_TIMEOUT_SECONDS] ?: DEFAULT_NETWORK_TIMEOUT_SECONDS }

    val bufferSizeKB: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[BUFFER_SIZE_KB] ?: DEFAULT_BUFFER_SIZE_KB }

    val maxBufferMs: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[MAX_BUFFER_MS] ?: DEFAULT_MAX_BUFFER_MS }

    val minBufferMs: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[MIN_BUFFER_MS] ?: DEFAULT_MIN_BUFFER_MS }

    val autoClearCache: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_CLEAR_CACHE] ?: false }

    val wifiOnlyStreaming: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[WIFI_ONLY_STREAMING] ?: false }

    // UI设置
    val darkMode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE] ?: DEFAULT_DARK_MODE }

    val showThumbnails: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_THUMBNAILS] ?: true }

    val gridLayout: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[GRID_LAYOUT] ?: false }

    val sortOrder: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SORT_ORDER] ?: DEFAULT_SORT_ORDER }

    val sortAscending: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SORT_ASCENDING] ?: true }

    // NAS连接设置
    val lastNasIp: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[LAST_NAS_IP] }

    val lastUsername: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[LAST_USERNAME] }

    // 设置保存方法
    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_NEXT] = enabled
        }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RESUME_PLAYBACK] = enabled
        }
    }

    suspend fun setScreenOrientation(orientation: String) {
        context.dataStore.edit { preferences ->
            preferences[SCREEN_ORIENTATION] = orientation
        }
    }

    suspend fun setCacheSizeMB(sizeMB: Int) {
        context.dataStore.edit { preferences ->
            preferences[CACHE_SIZE_MB] = sizeMB
        }
    }

    suspend fun setNetworkTimeoutSeconds(timeoutSeconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[NETWORK_TIMEOUT_SECONDS] = timeoutSeconds
        }
    }

    suspend fun setBufferSizeKB(sizeKB: Int) {
        context.dataStore.edit { preferences ->
            preferences[BUFFER_SIZE_KB] = sizeKB
        }
    }

    suspend fun setMaxBufferMs(maxMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_BUFFER_MS] = maxMs
        }
    }

    suspend fun setMinBufferMs(minMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[MIN_BUFFER_MS] = minMs
        }
    }

    suspend fun setAutoClearCache(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CLEAR_CACHE] = enabled
        }
    }

    suspend fun setWifiOnlyStreaming(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_STREAMING] = enabled
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = mode
        }
    }

    suspend fun setShowThumbnails(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_THUMBNAILS] = enabled
        }
    }

    suspend fun setGridLayout(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GRID_LAYOUT] = enabled
        }
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER] = order
        }
    }

    suspend fun setSortAscending(ascending: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ASCENDING] = ascending
        }
    }

    suspend fun setLastNasConnection(ip: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_NAS_IP] = ip
            preferences[LAST_USERNAME] = username
        }
    }
}
