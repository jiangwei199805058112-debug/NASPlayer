package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.core.player.ExoPlayerManager
import com.example.nasonly.data.smb.SmbDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val exoPlayerManager: ExoPlayerManager,
    private val smbDataSource: SmbDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState

    fun play(uri: String) {
        viewModelScope.launch {
            try {
                exoPlayerManager.prepare(android.net.Uri.parse(uri))
                exoPlayerManager.play()
                _uiState.value = _uiState.value.copy(isPlaying = true, error = null, isBuffering = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isPlaying = false)
            }
        }
    }

    fun pause() {
        exoPlayerManager.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun seekTo(positionMs: Long) {
        exoPlayerManager.seekTo(positionMs)
    }

    fun setBuffering(buffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = buffering)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        exoPlayerManager.release()
        super.onCleared()
    }
}

data class VideoPlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)
