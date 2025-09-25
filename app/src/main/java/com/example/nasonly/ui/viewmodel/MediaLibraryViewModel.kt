package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.repository.SmbRepository
import com.example.nasonly.ui.screens.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val smbRepository: SmbRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState

    private var currentPath: String = ""

    fun loadMediaFiles(path: String = "") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = smbRepository.listFiles(path)
                result.fold(
                    onSuccess = { smbFiles ->
                        val mediaItems = smbFiles.map { smbFile ->
                            MediaItem(
                                name = smbFile.name,
                                path = smbFile.path,
                                size = smbFile.size,
                                isDirectory = smbFile.isDirectory
                            )
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            mediaList = mediaItems,
                            currentPath = path
                        )
                        currentPath = path
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "加载媒体文件失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载媒体文件异常: ${e.message}"
                )
            }
        }
    }

    fun refreshMediaFiles() {
        loadMediaFiles(currentPath)
    }

    fun navigateToFolder(path: String) {
        loadMediaFiles(path)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class MediaLibraryUiState(
    val mediaList: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPath: String = ""
)