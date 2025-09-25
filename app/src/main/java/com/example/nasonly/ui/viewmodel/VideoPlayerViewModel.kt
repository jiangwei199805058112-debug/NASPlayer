package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.core.player.ExoPlayerManager
import com.example.nasonly.repository.SmbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import android.net.Uri

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val exoPlayerManager: ExoPlayerManager,
    private val smbRepository: SmbRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState

    private var progressUpdateJob: Job? = null
    private var currentUri: String = ""

    companion object {
        private const val SEEK_INCREMENT_MS = 10000L // 10秒
    }

    fun initializePlayer(uri: String) {
        currentUri = uri
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isBuffering = true, error = null)
                
                // 获取SMB输入流
                val result = smbRepository.getInputStream(uri)
                result.fold(
                    onSuccess = { inputStream ->
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

    fun play() {
        try {
            exoPlayerManager.play()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            startProgressUpdates()
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
    val isFullscreen: Boolean = false
)
