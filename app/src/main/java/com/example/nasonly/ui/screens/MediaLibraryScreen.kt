package com.example.nasonly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.ui.viewmodel.MediaLibraryViewModel
import com.example.nasonly.core.ui.components.LoadingIndicator
import com.example.nasonly.core.ui.components.ErrorDialog
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(
    navController: NavController,
    viewModel: MediaLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMediaFiles()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("媒体库") },
            actions = {
                IconButton(
                    onClick = { viewModel.refreshMediaFiles() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            
            uiState.mediaList.isEmpty() && !uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "未找到媒体文件",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { viewModel.refreshMediaFiles() }) {
                            Text("重新扫描")
                        }
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.mediaList) { media ->
                        MediaItemCard(
                            media = media,
                            onClick = {
                                if (media.isDirectory) {
                                    viewModel.navigateToFolder(media.path)
                                } else {
                                    val encodedUri = Uri.encode(media.path)
                                    navController.navigate("video_player?uri=$encodedUri")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 错误对话框
    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error,
            onDismiss = { viewModel.clearError() },
            onRetry = { viewModel.refreshMediaFiles() }
        )
    }
}

@Composable
private fun MediaItemCard(
    media: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (media.isDirectory) Icons.Default.Folder else Icons.Default.Movie,
                contentDescription = null,
                tint = if (media.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!media.isDirectory && media.size > 0) {
                    Text(
                        text = formatFileSize(media.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

data class MediaItem(
    val name: String,
    val path: String,
    val size: Long = 0,
    val isDirectory: Boolean = false
)