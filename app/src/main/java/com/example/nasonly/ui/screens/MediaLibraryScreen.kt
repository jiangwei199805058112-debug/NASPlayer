package com.example.nasonly.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.core.ui.components.ErrorDialog
import com.example.nasonly.core.ui.components.LoadingIndicator
import com.example.nasonly.navigation.Routes
import com.example.nasonly.ui.components.EnhancedVideoList
import com.example.nasonly.ui.components.SortOrder
import com.example.nasonly.ui.components.VideoListToolbar
import com.example.nasonly.ui.components.ViewMode
import com.example.nasonly.ui.viewmodel.HistoryItem
import timber.log.Timber
import com.example.nasonly.ui.viewmodel.MediaLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(
    navController: NavController,
    host: String = "",
    share: String = "",
    viewModel: MediaLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("媒体文件", "播放历史")

    LaunchedEffect(Unit) {
        if (host.isNotEmpty() && share.isNotEmpty()) {
            // 如果有host和share参数，从NAS加载
            viewModel.loadMediaFiles("smb://$host/$share")
        } else {
            // 默认加载本地或上次路径
            viewModel.loadMediaFiles()
        }
        viewModel.loadPlaybackHistory()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("媒体库") },
            actions = {
                IconButton(
                    onClick = {
                        navController.navigate(Routes.PLAYLIST_MANAGEMENT)
                    },
                ) {
                    Icon(Icons.Default.List, contentDescription = "媒体库")
                }
                IconButton(
                    onClick = {
                        if (selectedTabIndex == 0) {
                            viewModel.refreshMediaFiles()
                        } else {
                            viewModel.loadPlaybackHistory()
                        }
                    },
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(
                    onClick = {
                        navController.navigate(Routes.SETTINGS)
                    },
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
        )

        // 标签页
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        // 标签页内容
        when (selectedTabIndex) {
            0 -> {
                // 媒体文件标签页
                MediaFilesTab(
                    uiState = uiState,
                    onFileClick = { media ->
                        if (media.isDirectory) {
                            viewModel.navigateToFolder(media.path)
                        } else {
                            try {
                                val encodedUri = Uri.encode(media.path)
                                navController.navigate("video_player?uri=$encodedUri")
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to navigate to video player for: ${media.path}")
                            }
                        }
                    },
                    onRetry = { viewModel.refreshMediaFiles() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            1 -> {
                // 播放历史标签页
                PlaybackHistoryTab(
                    historyItems = uiState.playbackHistory,
                    isLoading = uiState.isLoading,
                    onHistoryItemClick = { historyItem ->
                        try {
                            val encodedUri = Uri.encode(historyItem.path)
                            navController.navigate("video_player?uri=$encodedUri")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to navigate to video player for history item: ${historyItem.path}")
                        }
                    },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onClearHistory = { viewModel.clearPlaybackHistory() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // 错误对话框
    val errorMessage = uiState.error
    if (errorMessage != null) {
        ErrorDialog(
            message = errorMessage,
            onDismiss = { viewModel.clearError() },
            onRetry = {
                if (selectedTabIndex == 0) {
                    viewModel.refreshMediaFiles()
                } else {
                    viewModel.loadPlaybackHistory()
                }
            },
        )
    }
}

@Composable
private fun MediaFilesTab(
    uiState: com.example.nasonly.ui.viewmodel.MediaLibraryUiState,
    onFileClick: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortOrder by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showThumbnails by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // 过滤文件
    val filteredFiles = remember(uiState.files, searchQuery) {
        if (searchQuery.isEmpty()) {
            uiState.files
        } else {
            uiState.files.filter { file ->
                file.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier) {
        // 工具栏
        if (uiState.files.isNotEmpty()) {
            VideoListToolbar(
                sortOrder = sortOrder,
                viewMode = viewMode,
                showThumbnails = showThumbnails,
                onSortOrderChange = { sortOrder = it },
                onViewModeChange = { viewMode = it },
                onShowThumbnailsChange = { showThumbnails = it },
                onSearch = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }

            filteredFiles.isEmpty() && !uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "未找到媒体文件" else "未找到匹配的文件",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (searchQuery.isEmpty()) {
                            Button(onClick = onRetry) {
                                Text("重新扫描")
                            }
                        }
                    }
                }
            }

            else -> {
                EnhancedVideoList(
                    files = filteredFiles,
                    showThumbnails = showThumbnails,
                    viewMode = viewMode,
                    sortOrder = sortOrder,
                    onFileClick = { smbFile ->
                        val media = MediaItem(
                            name = smbFile.name,
                            path = smbFile.path,
                            size = smbFile.size,
                            isDirectory = smbFile.isDirectory,
                        )
                        onFileClick(media)
                    },
                    onFileLongClick = { _ ->
                        // FIXME: Feature not implemented - show file details dialog
                    },
                    onAddToPlaylist = { _ ->
                        // FIXME: Feature not implemented - show add to playlist dialog
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PlaybackHistoryTab(
    historyItems: List<HistoryItem>,
    isLoading: Boolean,
    onHistoryItemClick: (HistoryItem) -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 顶部工具栏
        if (historyItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClearHistory) {
                    Text("清除全部")
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }

            historyItems.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无播放历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(historyItems) { historyItem ->
                        HistoryItemCard(
                            historyItem = historyItem,
                            onClick = { onHistoryItemClick(historyItem) },
                            onDelete = { onDeleteHistoryItem(historyItem) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemCard(
    media: MediaItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (media.isDirectory) Icons.Default.Folder else Icons.Default.Movie,
                contentDescription = null,
                tint = if (media.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!media.isDirectory && media.size > 0) {
                    Text(
                        text = formatFileSize(media.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    historyItem: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyItem.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "播放至 ${formatPosition(historyItem.position)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = historyItem.lastPlayed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatPosition(positionMs: Long): String {
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

data class MediaItem(
    val name: String,
    val path: String,
    val size: Long = 0,
    val isDirectory: Boolean = false,
)
