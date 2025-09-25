package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.db.Playlist
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.db.PlaylistItem
import com.example.nasonly.data.db.PlaylistItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistWithItems(
    val playlist: Playlist,
    val items: List<PlaylistItem>
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao
) : ViewModel() {
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    
    private val _currentPlaylistItems = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val currentPlaylistItems: StateFlow<List<PlaylistItem>> = _currentPlaylistItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadPlaylists()
    }
    
    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                playlistDao.getAllPlaylists().collect { playlistsList ->
                    _playlists.value = playlistsList
                }
            } catch (e: Exception) {
                _error.value = "加载播放列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val playlist = Playlist(
                    name = name,
                    description = description
                )
                playlistDao.createPlaylistWithCount(playlist)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "创建播放列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updatedPlaylist = playlist.copy(updatedAt = System.currentTimeMillis())
                playlistDao.updatePlaylist(updatedPlaylist)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "更新播放列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                playlistDao.deletePlaylist(playlist)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "删除播放列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadPlaylistItems(playlistId: Long) {
        viewModelScope.launch {
            try {
                playlistItemDao.getPlaylistItems(playlistId).collect { items ->
                    _currentPlaylistItems.value = items
                }
            } catch (e: Exception) {
                _error.value = "加载播放列表项失败: ${e.message}"
            }
        }
    }
    
    fun addItemToPlaylist(playlistId: Long, videoPath: String, videoName: String, fileSize: Long = 0, duration: Long = 0) {
        viewModelScope.launch {
            try {
                playlistItemDao.addItemToPlaylist(playlistId, videoPath, videoName, fileSize, duration)
                playlistDao.updatePlaylistItemCount(playlistId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "添加到播放列表失败: ${e.message}"
            }
        }
    }
    
    fun removeItemFromPlaylist(item: PlaylistItem) {
        viewModelScope.launch {
            try {
                playlistItemDao.deletePlaylistItem(item)
                playlistDao.updatePlaylistItemCount(item.playlistId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "从播放列表移除失败: ${e.message}"
            }
        }
    }
    
    fun reorderPlaylistItems(playlistId: Long, itemIds: List<Long>) {
        viewModelScope.launch {
            try {
                playlistItemDao.reorderPlaylistItems(playlistId, itemIds)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "重新排序失败: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}