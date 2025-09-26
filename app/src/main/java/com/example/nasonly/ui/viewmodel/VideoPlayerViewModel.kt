package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.core.player.ExoPlayerManager
import com.example.nasonly.repository.SmbRepository
import com.example.nasonly.data.db.PlaybackHistory
import com.example.nasonly.data.db.PlaybackHistoryDao
import com.example.nasonly.data.db.Playlist
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.db.PlaylistItem
import com.example.nasonly.data.db.PlaylistItemDao
import com.example.nasonly.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import android.net.Uri

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") private val savedStateHandle: SavedStateHandle,
    private val exoPlayerManager: ExoPlayerManager,
    private val smbRepository: SmbRepository,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val playlistItemDao: PlaylistItemDao,
    private val playlistDao: PlaylistDao,
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState

    private var progressUpdateJob: Job? = null
    private var currentUri: String = ""
    private var currentPlaylistId: Long = -1
    private var currentPlaylist: List<PlaylistItem> = emptyList()
    private var currentIndex: Int = -1

    companion object {
        private const val SEEK_INCREMENT_MS = 10000L // 10秒
    }

    fun initializePlayer(uri: String) {
        currentUri = uri
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isBuffering = true, error = null)
                
                // 检查收藏状态
                checkFavoriteStatus()
                
                // 获取SMB输入流
                val result = smbRepository.getInputStream(uri)
                result.fold(
                    onSuccess = { _ ->
                        // 准备播放器
                        exoPlayerManager.prepare(Uri.parse(uri))
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            duration = 300000L // 模拟5分钟视频，实际应从ExoPlayer获取
                        )
                        startProgressUpdates()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            error = "加载视频失败: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    error = "播放器初始化失败: ${e.message}"
                )
            }
        }
    }

    private suspend fun checkFavoriteStatus() {
        try {
            val favoritePlaylist = playlistDao.getPlaylistByName("我的收藏")
            if (favoritePlaylist != null) {
                val existingItem = playlistItemDao.getPlaylistItemByPath(favoritePlaylist.id, currentUri)
                _uiState.value = _uiState.value.copy(isFavorited = existingItem != null)
            } else {
                _uiState.value = _uiState.value.copy(isFavorited = false)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isFavorited = false)
        }
    }

    fun play() {
        try {
            exoPlayerManager.play()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            startProgressUpdates()
            // 开始播放时保存历史记录
            savePlaybackHistory()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "播放失败: ${e.message}")
        }
    }

    fun pause() {
        try {
            exoPlayerManager.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
            stopProgressUpdates()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "暂停失败: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            exoPlayerManager.seekTo(positionMs)
            _uiState.value = _uiState.value.copy(currentPosition = positionMs)
            // 跳转时也保存历史记录
            savePlaybackHistory()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "跳转失败: ${e.message}")
        }
    }

    fun fastForward() {
        val newPosition = (_uiState.value.currentPosition + SEEK_INCREMENT_MS)
            .coerceAtMost(_uiState.value.duration)
        seekTo(newPosition)
    }

    fun rewind() {
        val newPosition = (_uiState.value.currentPosition - SEEK_INCREMENT_MS)
            .coerceAtLeast(0L)
        seekTo(newPosition)
    }

    fun retry(uri: String) {
        clearError()
        initializePlayer(uri)
    }

    fun setVolume(volume: Float) {
        try {
            // 这里应该调用系统音量管理器或ExoPlayer的音量设置
            // 目前仅记录到状态中
            _uiState.value = _uiState.value.copy(volume = volume)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "音量调节失败: ${e.message}")
        }
    }

    fun setBrightness(brightness: Float) {
        try {
            // 这里应该调用Activity的窗口亮度设置
            // 目前仅记录到状态中
            _uiState.value = _uiState.value.copy(brightness = brightness)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "亮度调节失败: ${e.message}")
        }
    }

    fun toggleFullscreen() {
        try {
            val currentFullscreen = _uiState.value.isFullscreen
            _uiState.value = _uiState.value.copy(isFullscreen = !currentFullscreen)
            // 这里应该调用Activity的全屏切换方法
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "全屏切换失败: ${e.message}")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun release() {
        // 在释放前保存最后一次播放历史
        savePlaybackHistory()
        stopProgressUpdates()
        try {
            exoPlayerManager.release()
            smbRepository.close()
        } catch (e: Exception) {
            // 忽略释放时的错误
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = viewModelScope.launch {
            var progressCounter = 0
            while (_uiState.value.isPlaying) {
                try {
                    // 从 ExoPlayer 获取当前播放位置
                    val player = exoPlayerManager.getPlayer()
                    val currentPosition = player?.currentPosition ?: _uiState.value.currentPosition
                    val duration = player?.duration?.takeIf { it > 0 } ?: _uiState.value.duration
                    
                    _uiState.value = _uiState.value.copy(
                        currentPosition = currentPosition,
                        duration = duration
                    )
                    
                    // 每10秒保存一次播放历史（避免频繁数据库写入）
                    progressCounter++
                    if (progressCounter >= 10) {
                        savePlaybackHistory()
                        progressCounter = 0
                    }
                    
                    delay(1000) // 每秒更新一次
                } catch (e: Exception) {
                    // 忽略更新错误
                    break
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun savePlaybackHistory() {
        if (currentUri.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val currentState = _uiState.value
                    val history = PlaybackHistory(
                        id = System.currentTimeMillis(), // 使用时间戳作为ID
                        videoPath = currentUri,
                        position = currentState.currentPosition,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // 检查是否已存在相同路径的记录
                    val existingHistory = playbackHistoryDao.getByVideoPath(currentUri)
                    if (existingHistory.isNotEmpty()) {
                        // 更新现有记录
                        val existing = existingHistory.first()
                        val updated = existing.copy(
                            position = currentState.currentPosition,
                            updatedAt = System.currentTimeMillis()
                        )
                        playbackHistoryDao.update(updated)
                    } else {
                        // 插入新记录
                        playbackHistoryDao.insert(history)
                    }
                } catch (e: Exception) {
                    // 静默处理历史记录保存失败，不影响播放体验
                    android.util.Log.w("VideoPlayerViewModel", "Failed to save playback history: ${e.message}")
                }
            }
        }
    }

    fun pauseAndSaveHistory() {
        pause()
        savePlaybackHistory()
    }
    
    // ==================== 播放列表相关方法 ====================
    
    /**
     * 初始化播放列表播放
     */
    fun initializePlaylistPlayer(playlistId: Long, startIndex: Int = 0) {
        currentPlaylistId = playlistId
        viewModelScope.launch {
            try {
                // 加载播放列表项
                playlistItemDao.getPlaylistItems(playlistId).collect { items ->
                    currentPlaylist = items
                    currentIndex = startIndex.coerceIn(0, items.size - 1)
                    
                    _uiState.value = _uiState.value.copy(
                        currentPlaylist = items,
                        currentIndex = currentIndex,
                        isPlaylistMode = true,
                        canPlayPrevious = currentIndex > 0,
                        canPlayNext = currentIndex < items.size - 1
                    )
                    
                    // 加载用户偏好设置
                    val autoPlay = userPreferences.autoPlayNext.first()
                    val speed = userPreferences.playbackSpeed.first()
                    
                    _uiState.value = _uiState.value.copy(
                        autoPlayNext = autoPlay,
                        playbackSpeed = speed
                    )
                    
                    // 播放当前索引的视频
                    if (items.isNotEmpty() && currentIndex >= 0) {
                        val currentItem = items[currentIndex]
                        initializePlayer(currentItem.videoPath)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载播放列表失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 播放下一个视频
     */
    fun playNext() {
        if (currentIndex < currentPlaylist.size - 1) {
            currentIndex++
            updateCurrentPlaylistItem()
        }
    }
    
    /**
     * 播放上一个视频
     */
    fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            updateCurrentPlaylistItem()
        }
    }
    
    /**
     * 跳转到指定索引的视频
     */
    fun playItemAt(index: Int) {
        if (index in 0 until currentPlaylist.size) {
            currentIndex = index
            updateCurrentPlaylistItem()
        }
    }
    
    /**
     * 当前视频播放完毕时的处理
     */
    fun onVideoCompleted() {
        val currentState = _uiState.value
        if (currentState.isPlaylistMode && currentState.autoPlayNext && currentState.canPlayNext) {
            playNext()
        } else if (currentState.isPlaylistMode) {
            // 播放列表播放完毕
            pause()
            _uiState.value = _uiState.value.copy(
                currentPosition = 0L
            )
        }
    }
    
    /**
     * 设置播放速度
     */
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                exoPlayerManager.setPlaybackSpeed(speed)
                userPreferences.setPlaybackSpeed(speed)
                _uiState.value = _uiState.value.copy(playbackSpeed = speed)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "设置播放速度失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 切换自动播放下一个
     */
    fun toggleAutoPlayNext() {
        val newValue = !_uiState.value.autoPlayNext
        viewModelScope.launch {
            userPreferences.setAutoPlayNext(newValue)
            _uiState.value = _uiState.value.copy(autoPlayNext = newValue)
        }
    }
    
    /**
     * 获取当前播放的项目信息
     */
    fun getCurrentPlaylistItem(): PlaylistItem? {
        return if (currentIndex in 0 until currentPlaylist.size) {
            currentPlaylist[currentIndex]
        } else null
    }
    
    /**
     * 退出播放列表模式
     */
    fun exitPlaylistMode() {
        currentPlaylistId = -1
        currentPlaylist = emptyList()
        currentIndex = -1
        _uiState.value = _uiState.value.copy(
            isPlaylistMode = false,
            currentPlaylist = emptyList(),
            currentIndex = -1,
            canPlayPrevious = false,
            canPlayNext = false
        )
    }
    
    private fun updateCurrentPlaylistItem() {
        if (currentIndex in 0 until currentPlaylist.size) {
            val currentItem = currentPlaylist[currentIndex]
            
            // 更新UI状态
            _uiState.value = _uiState.value.copy(
                currentIndex = currentIndex,
                canPlayPrevious = currentIndex > 0,
                canPlayNext = currentIndex < currentPlaylist.size - 1
            )
            
            // 初始化新的视频播放
            initializePlayer(currentItem.videoPath)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                if (currentUri.isNotEmpty()) {
                    val favoritePlaylistName = "我的收藏"
                    var favoritePlaylist = playlistDao.getPlaylistByName(favoritePlaylistName)
                    
                    val playlistId = if (favoritePlaylist == null) {
                        // 如果不存在收藏播放列表，创建一个新的
                        val newPlaylist = Playlist(
                            name = favoritePlaylistName,
                            description = "自动创建的收藏播放列表",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        playlistDao.insertPlaylist(newPlaylist)
                    } else {
                        favoritePlaylist.id
                    }
                    
                    // 检查视频是否已在收藏列表中
                    val existingItem = playlistItemDao.getPlaylistItemByPath(playlistId, currentUri)
                    
                    if (existingItem == null) {
                        // 添加到收藏
                        val maxIndex = playlistItemDao.getMaxOrderIndex(playlistId) ?: 0
                        val videoItem = PlaylistItem(
                            playlistId = playlistId,
                            videoPath = currentUri,
                            videoName = extractVideoName(currentUri),
                            fileSize = 0,
                            duration = _uiState.value.duration,
                            orderIndex = maxIndex + 1,
                            addedAt = System.currentTimeMillis()
                        )
                        playlistItemDao.insertPlaylistItem(videoItem)
                        playlistDao.updatePlaylistItemCount(playlistId)
                        
                        _uiState.value = _uiState.value.copy(isFavorited = true)
                    } else {
                        // 从收藏中移除
                        playlistItemDao.deletePlaylistItem(existingItem)
                        playlistDao.updatePlaylistItemCount(playlistId)
                        
                        _uiState.value = _uiState.value.copy(isFavorited = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "切换收藏状态失败: ${e.message}"
                )
            }
        }
    }

    fun addToFavoritePlaylist() {
        viewModelScope.launch {
            try {
                if (currentUri.isNotEmpty()) {
                    // 获取或创建"我的收藏"播放列表
                    val favoritePlaylistName = "我的收藏"
                    var favoritePlaylist = playlistDao.getPlaylistByName(favoritePlaylistName)
                    
                    val playlistId = if (favoritePlaylist == null) {
                        // 如果不存在收藏播放列表，创建一个新的
                        val newPlaylist = Playlist(
                            name = favoritePlaylistName,
                            description = "自动创建的收藏播放列表",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        playlistDao.insertPlaylist(newPlaylist)
                    } else {
                        favoritePlaylist.id
                    }
                    
                    // 检查视频是否已在收藏列表中
                    val existingItem = playlistItemDao.getPlaylistItemByPath(playlistId, currentUri)
                    
                    if (existingItem == null) {
                        // 添加当前视频到播放列表
                        val maxIndex = playlistItemDao.getMaxOrderIndex(playlistId) ?: 0
                        val videoItem = PlaylistItem(
                            playlistId = playlistId,
                            videoPath = currentUri,
                            videoName = extractVideoName(currentUri),
                            fileSize = 0, // 可以从SMB获取实际文件大小
                            duration = _uiState.value.duration,
                            orderIndex = maxIndex + 1,
                            addedAt = System.currentTimeMillis()
                        )
                        playlistItemDao.insertPlaylistItem(videoItem)
                        
                        // 更新播放列表项目数量
                        playlistDao.updatePlaylistItemCount(playlistId)
                        
                        // 显示成功消息（这里可以通过UI状态反馈）
                        // _uiState.value = _uiState.value.copy(successMessage = "已添加到收藏列表")
                    } else {
                        // 视频已存在于收藏列表中
                        // _uiState.value = _uiState.value.copy(successMessage = "该视频已在收藏列表中")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "添加到收藏列表失败: ${e.message}"
                )
            }
        }
    }
    
    private fun extractVideoName(uri: String): String {
        return try {
            val decodedUri = Uri.decode(uri)
            decodedUri.substringAfterLast("/").substringBeforeLast(".")
        } catch (e: Exception) {
            "未知视频"
        }
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}

data class VideoPlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 0.5f,
    val brightness: Float = 0.5f,
    val isFullscreen: Boolean = false,
    // 播放列表相关
    val currentPlaylist: List<PlaylistItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaylistMode: Boolean = false,
    val autoPlayNext: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val canPlayPrevious: Boolean = false,
    val canPlayNext: Boolean = false,
    val isFavorited: Boolean = false
)
