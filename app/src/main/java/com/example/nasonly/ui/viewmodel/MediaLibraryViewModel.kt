package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.db.PlaybackHistoryDao
import com.example.nasonly.data.remote.smb.RemoteVideo
import com.example.nasonly.data.smb.SmbCredentials
import com.example.nasonly.data.remote.smb.SmbScanner
import com.example.nasonly.data.smb.SmbFile
import com.example.nasonly.data.smb.SmbFileWithMetadata
import com.example.nasonly.repository.SmbRepository
import com.example.nasonly.ui.screens.MediaItem
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
    private val smbScanner: SmbScanner,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState

        private var currentShare: String = ""
        private var currentPath: String = ""

    fun loadMediaFiles(path: String = "") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = smbRepository.listFilesWithMetadata(
                    currentShare,
                    path,
                    includeMetadata = true,
                    generateThumbnails = true,
                )
                result.fold(
                    onSuccess = { smbFiles ->
                        @Suppress("UNUSED_VARIABLE")
                        val mediaItems = smbFiles.map { smbFileWithMetadata ->
                            MediaItem(
                                name = smbFileWithMetadata.file.name,
                                path = smbFileWithMetadata.file.fullPath,
                                size = smbFileWithMetadata.file.size,
                                isDirectory = smbFileWithMetadata.file.isDirectory,
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            files = smbFiles,
                            currentPath = path,
                        )
                        currentPath = path
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "加载媒体文件失败",
                        )
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载媒体文件异常: ${e.message}",
                )
            }
        }
    }
    
        /**
         * 加载顶层共享列表或指定共享下的文件
         * @param host NAS主机名
         * @param creds SMB凭据
         * @param shareName 共享名（为空则枚举共享列表）
         * @param relativePath 共享内路径
         */
        fun loadMedia(host: String, creds: SmbCredentials, shareName: String = "", relativePath: String = "") {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                try {
                    if (shareName.isEmpty()) {
                        // 枚举顶层共享
                        val result = smbRepository.getShares(host, creds)
                        result.fold(
                            onSuccess = { shares ->
                                val items = shares.map { share ->
                                    SmbFileWithMetadata(
                                        file = SmbFile(
                                            name = share.name,
                                            isDirectory = true,
                                            size = 0L,
                                            lastModified = Date(),
                                            fullPath = share.name
                                        ),
                                        metadata = null
                                    )
                                }
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    files = items,
                                    currentPath = "",
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = error.message ?: "加载共享列表失败",
                                )
                            },
                        )
                    } else {
                        // 枚举共享内文件
                        val result = smbRepository.listFilesWithMetadata(
                            shareName,
                            relativePath,
                            includeMetadata = true,
                            generateThumbnails = true,
                        )
                        result.fold(
                            onSuccess = { smbFiles ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    files = smbFiles,
                                    currentPath = relativePath,
                                )
                                currentShare = shareName
                                currentPath = relativePath
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = error.message ?: "加载媒体文件失败",
                                )
                            },
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "加载媒体文件异常: ${e.message}",
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

    fun scanExample() {
        viewModelScope.launch {
            val list = smbScanner.scanVideos(
                rootPath = "\\\\Mycloudpr2100\\备份文件\\20250606美术馆",
                creds = com.example.nasonly.data.remote.smb.SmbCredentials(username = "guest", password = "")
            ) { found ->
                // stream results to UI if needed
                // Log.d("SMB", "Found: ${found.name}")
            }
            // Update UI state with found videos
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                files = list.map { remoteVideo ->
                    SmbFileWithMetadata(
                        file = SmbFile(
                            name = remoteVideo.name,
                            isDirectory = false,
                            size = remoteVideo.size,
                            lastModified = Date(),
                            fullPath = remoteVideo.smbUrl
                        ),
                        metadata = null
                    )
                }
            )
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
                    error = "清除播放历史失败: ${e.message}",
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
                    error = "删除历史记录失败: ${e.message}",
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

    fun loadPlaybackHistory() {
        viewModelScope.launch {
            try {
                val historyList = playbackHistoryDao.getAll()
                val historyItems = historyList.map { history ->
                    val fileName = history.videoPath.substringAfterLast('/')
                    val formattedTime = formatTimestamp(history.updatedAt)
                    // Note: progressPercentage calculation requires video duration metadata
                    // For now, using a placeholder calculation
                    val progressPercentage = if (history.position > 0) 0.5f else 0f // Placeholder

                    HistoryItem(
                        id = history.id,
                        path = history.videoPath,
                        fileName = fileName,
                        position = history.position,
                        lastPlayed = formattedTime,
                        progressPercentage = progressPercentage,
                    )
                }
                _uiState.value = _uiState.value.copy(playbackHistory = historyItems)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载播放历史失败: ${e.message}",
                )
            }
        }
    }
}

data class MediaLibraryUiState(
    val currentPath: String = "",
    val files: List<SmbFileWithMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val playbackHistory: List<HistoryItem> = emptyList(),
)

data class HistoryItem(
    val id: Long,
    val path: String,
    val fileName: String,
    val position: Long,
    val lastPlayed: String,
    val progressPercentage: Float,
)
