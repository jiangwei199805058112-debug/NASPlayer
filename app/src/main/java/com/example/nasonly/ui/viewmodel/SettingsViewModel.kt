package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    // 播放器设置
    private val _playbackSpeed = MutableStateFlow(UserPreferences.DEFAULT_PLAYBACK_SPEED)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    private val _autoPlayNext = MutableStateFlow(false)
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()
    
    private val _resumePlayback = MutableStateFlow(true)
    val resumePlayback: StateFlow<Boolean> = _resumePlayback.asStateFlow()
    
    private val _screenOrientation = MutableStateFlow("auto")
    val screenOrientation: StateFlow<String> = _screenOrientation.asStateFlow()
    
    // 系统设置
    private val _cacheSizeMB = MutableStateFlow(UserPreferences.DEFAULT_CACHE_SIZE_MB)
    val cacheSizeMB: StateFlow<Int> = _cacheSizeMB.asStateFlow()
    
    private val _networkTimeoutSeconds = MutableStateFlow(UserPreferences.DEFAULT_NETWORK_TIMEOUT_SECONDS)
    val networkTimeoutSeconds: StateFlow<Int> = _networkTimeoutSeconds.asStateFlow()
    
    private val _bufferSizeKB = MutableStateFlow(UserPreferences.DEFAULT_BUFFER_SIZE_KB)
    val bufferSizeKB: StateFlow<Int> = _bufferSizeKB.asStateFlow()
    
    private val _maxBufferMs = MutableStateFlow(UserPreferences.DEFAULT_MAX_BUFFER_MS)
    val maxBufferMs: StateFlow<Int> = _maxBufferMs.asStateFlow()
    
    private val _minBufferMs = MutableStateFlow(UserPreferences.DEFAULT_MIN_BUFFER_MS)
    val minBufferMs: StateFlow<Int> = _minBufferMs.asStateFlow()
    
    private val _autoClearCache = MutableStateFlow(false)
    val autoClearCache: StateFlow<Boolean> = _autoClearCache.asStateFlow()
    
    private val _wifiOnlyStreaming = MutableStateFlow(false)
    val wifiOnlyStreaming: StateFlow<Boolean> = _wifiOnlyStreaming.asStateFlow()
    
    // UI设置
    private val _darkMode = MutableStateFlow(UserPreferences.DEFAULT_DARK_MODE)
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()
    
    private val _showThumbnails = MutableStateFlow(true)
    val showThumbnails: StateFlow<Boolean> = _showThumbnails.asStateFlow()
    
    private val _gridLayout = MutableStateFlow(false)
    val gridLayout: StateFlow<Boolean> = _gridLayout.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(UserPreferences.DEFAULT_SORT_ORDER)
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()
    
    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // 播放器设置
                userPreferences.playbackSpeed.collect { _playbackSpeed.value = it }
            } catch (e: Exception) {
                _error.value = "加载设置失败: ${e.message}"
            }
        }
        
        viewModelScope.launch {
            userPreferences.autoPlayNext.collect { _autoPlayNext.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.resumePlayback.collect { _resumePlayback.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.screenOrientation.collect { _screenOrientation.value = it }
        }
        
        // 系统设置
        viewModelScope.launch {
            userPreferences.cacheSizeMB.collect { _cacheSizeMB.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.networkTimeoutSeconds.collect { _networkTimeoutSeconds.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.bufferSizeKB.collect { _bufferSizeKB.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.maxBufferMs.collect { _maxBufferMs.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.minBufferMs.collect { _minBufferMs.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.autoClearCache.collect { _autoClearCache.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.wifiOnlyStreaming.collect { _wifiOnlyStreaming.value = it }
        }
        
        // UI设置
        viewModelScope.launch {
            userPreferences.darkMode.collect { _darkMode.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.showThumbnails.collect { _showThumbnails.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.gridLayout.collect { _gridLayout.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.sortOrder.collect { _sortOrder.value = it }
        }
        
        viewModelScope.launch {
            userPreferences.sortAscending.collect { _sortAscending.value = it }
        }
    }
    
    // 播放器设置更新方法
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                userPreferences.setPlaybackSpeed(speed)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setAutoPlayNext(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setResumePlayback(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setScreenOrientation(orientation: String) {
        viewModelScope.launch {
            try {
                userPreferences.setScreenOrientation(orientation)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    // 系统设置更新方法
    fun setCacheSizeMB(sizeMB: Int) {
        viewModelScope.launch {
            try {
                userPreferences.setCacheSizeMB(sizeMB)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setNetworkTimeoutSeconds(timeoutSeconds: Int) {
        viewModelScope.launch {
            try {
                userPreferences.setNetworkTimeoutSeconds(timeoutSeconds)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setBufferSizeKB(sizeKB: Int) {
        viewModelScope.launch {
            try {
                userPreferences.setBufferSizeKB(sizeKB)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setMaxBufferMs(maxMs: Int) {
        viewModelScope.launch {
            try {
                userPreferences.setMaxBufferMs(maxMs)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setMinBufferMs(minMs: Int) {
        viewModelScope.launch {
            try {
                userPreferences.setMinBufferMs(minMs)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setAutoClearCache(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setAutoClearCache(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setWifiOnlyStreaming(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setWifiOnlyStreaming(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    // UI设置更新方法
    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            try {
                userPreferences.setDarkMode(mode)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setShowThumbnails(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setShowThumbnails(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setGridLayout(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setGridLayout(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setSortOrder(order: String) {
        viewModelScope.launch {
            try {
                userPreferences.setSortOrder(order)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun setSortAscending(ascending: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setSortAscending(ascending)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}