package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.repository.SmbRepository
import com.example.nasonly.ui.screens.MediaItem
import com.example.nasonly.data.db.PlaybackHistory
import com.example.nasonly.data.db.PlaybackHistoryDao
import com.example.nasonly.data.smb.SmbFileInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val smbRepository: SmbRepository,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val performanceMonitor: com.example.nasonly.core.performance.PerformanceMonitor
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState

    private var currentPath: String = ""
    private var allFiles: List<SmbFileInfo> = emptyList()
    
    companion object {
        private const val PAGE_SIZE = 50 // 每页加载文件数量
        private const val METADATA_BATCH_SIZE = 10 // 元数据批量加载大小
    }

    fun loadMediaFiles(path: String = "") {
        viewModelScope.launch {
            val operationId = performanceMonitor.startOperation("file_load")
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true, 
                    error = null,
                    files = emptyList(),
                    currentPage = 0,
                    hasMoreFiles = true
                )
                
                // 首先获取基本文件列表（不包含元数据）
                val result = smbRepository.listFiles(path)
                result.fold(
                    onSuccess = { smbFiles ->
                        allFiles = smbFiles
                        currentPath = path
                        performanceMonitor.updateLoadedFilesCount(smbFiles.size)
                        
                        // 加载第一页
                        loadMoreFiles(isInitialLoad = true)
                        performanceMonitor.endOperation(operationId, "file_load")
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "加载媒体文件失败"
                        )
                        performanceMonitor.endOperation(operationId, "file_load")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载媒体文件异常: ${e.message}"
                )
                performanceMonitor.endOperation(operationId, "file_load")
            }
        }
    }
    
    fun loadMoreFiles(isInitialLoad: Boolean = false) {
        val currentState = _uiState.value
        
        if (!isInitialLoad && (currentState.isLoadingMore || !currentState.hasMoreFiles)) {
            return
        }
        
        viewModelScope.launch {
            try {
                if (!isInitialLoad) {
                    _uiState.value = currentState.copy(isLoadingMore = true)
                }
                
                val startIndex = currentState.currentPage * PAGE_SIZE
                val endIndex = minOf(startIndex + PAGE_SIZE, allFiles.size)
                
                if (startIndex >= allFiles.size) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        hasMoreFiles = false
                    )
                    return@launch
                }
                
                val pageFiles = allFiles.subList(startIndex, endIndex)
                
                // 批量加载元数据
                val enhancedFiles = enhanceFilesWithMetadata(pageFiles)
                
                val updatedFiles = if (isInitialLoad) {
                    enhancedFiles
                } else {
                    currentState.files + enhancedFiles
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    files = updatedFiles,
                    currentPage = currentState.currentPage + 1,
                    hasMoreFiles = endIndex < allFiles.size,
                    currentPath = currentPath
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "加载更多文件失败: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun enhanceFilesWithMetadata(files: List<SmbFileInfo>): List<SmbFileInfo> {
        // 分批处理视频文件的元数据
        val videoFiles = files.filter { it.isVideoFile }
        val nonVideoFiles = files.filter { !it.isVideoFile }
        
        val enhancedVideoFiles = mutableListOf<SmbFileInfo>()
        
        // 分批处理视频文件元数据
        videoFiles.chunked(METADATA_BATCH_SIZE).forEach { batch ->
            batch.forEach { file ->
                try {
                    // 这里可以添加并行处理逻辑
                    val result = smbRepository.listFilesWithMetadata(
                        directoryPath = file.path.substringBeforeLast("/"),
                        includeMetadata = true,
                        generateThumbnails = false // 延迟生成缩略图
                    )
                    
                    result.fold(
                        onSuccess = { enhancedList ->
                            val enhanced = enhancedList.find { it.path == file.path } ?: file
                            enhancedVideoFiles.add(enhanced)
                        },
                        onFailure = {
                            enhancedVideoFiles.add(file)
                        }
                    )
                } catch (e: Exception) {
                    enhancedVideoFiles.add(file)
                }
            }
        }
        
        return (enhancedVideoFiles + nonVideoFiles).sortedBy { it.name.lowercase() }
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

    fun loadPlaybackHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val history = playbackHistoryDao.getAll()
                val historyItems = history.sortedByDescending { it.updatedAt }.map { item ->
                    HistoryItem(
                        id = item.id,
                        path = item.videoPath,
                        fileName = item.videoPath.substringAfterLast("/"),
                        position = item.position,
                        lastPlayed = formatTimestamp(item.updatedAt),
                        progressPercentage = 0f // 需要视频总时长来计算，暂时设为0
                    )
                }
                _uiState.value = _uiState.value.copy(
                    playbackHistory = historyItems,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载播放历史失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            try {
                val history = playbackHistoryDao.getAll()
                history.forEach { playbackHistoryDao.delete(it) }
                loadPlaybackHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "清除播放历史失败: ${e.message}"
                )
            }
        }
    }

    fun deleteHistoryItem(historyItem: HistoryItem) {
        viewModelScope.launch {
            try {
                val history = playbackHistoryDao.getById(historyItem.id)
                history?.let { playbackHistoryDao.delete(it) }
                loadPlaybackHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除历史记录失败: ${e.message}"
                )
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatPosition(positionMs: Long): String {
        val totalSeconds = positionMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

data class MediaLibraryUiState(
    val currentPath: String = "",
    val files: List<SmbFileInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreFiles: Boolean = true,
    val currentPage: Int = 0,
    val error: String? = null,
    val playbackHistory: List<HistoryItem> = emptyList()
)

data class HistoryItem(
    val id: Long,
    val path: String,
    val fileName: String,
    val position: Long,
    val lastPlayed: String,
    val progressPercentage: Float
)