package com.example.nasonly.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.graphics.drawable.ColorDrawable
import com.example.nasonly.R
import com.example.nasonly.data.smb.SmbFileInfo

enum class SortOrder {
    NAME_ASC, NAME_DESC,
    DATE_ASC, DATE_DESC,
    SIZE_ASC, SIZE_DESC,
    DURATION_ASC, DURATION_DESC
}

enum class ViewMode {
    LIST, GRID
}

@Composable
fun EnhancedVideoList(
    files: List<SmbFileInfo>,
    showThumbnails: Boolean = true,
    viewMode: ViewMode = ViewMode.LIST,
    sortOrder: SortOrder = SortOrder.NAME_ASC,
    onFileClick: (SmbFileInfo) -> Unit,
    onFileLongClick: (SmbFileInfo) -> Unit = {},
    onAddToPlaylist: (SmbFileInfo) -> Unit = {},
    onLoadMore: () -> Unit = {},
    hasMoreData: Boolean = false,
    isLoadingMore: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 优化排序计算，使用derivedStateOf避免不必要的重计算
    val sortedFiles by remember {
        derivedStateOf {
            when (sortOrder) {
                SortOrder.NAME_ASC -> files.sortedBy { it.name.lowercase() }
                SortOrder.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
                SortOrder.DATE_ASC -> files.sortedBy { it.lastModified }
                SortOrder.DATE_DESC -> files.sortedByDescending { it.lastModified }
                SortOrder.SIZE_ASC -> files.sortedBy { it.size }
                SortOrder.SIZE_DESC -> files.sortedByDescending { it.size }
                SortOrder.DURATION_ASC -> files.sortedBy { it.duration }
                SortOrder.DURATION_DESC -> files.sortedByDescending { it.duration }
            }
        }
    }
    
    // 滚动状态
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    
    // 检测滚动到底部
    LaunchedEffect(listState, gridState, hasMoreData) {
        if (!hasMoreData || isLoadingMore) return@LaunchedEffect
        
        snapshotFlow {
            when (viewMode) {
                ViewMode.LIST -> {
                    val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisibleIndex >= sortedFiles.size - 3 // 提前3个item开始加载
                }
                ViewMode.GRID -> {
                    val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisibleIndex >= sortedFiles.size - 6 // 提前6个item开始加载（2行）
                }
            }
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                onLoadMore()
            }
        }
    }
    
    when (viewMode) {
        ViewMode.LIST -> {
            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sortedFiles,
                    key = { file -> file.path } // 使用stable key提高性能
                ) { file ->
                    EnhancedVideoListItem(
                        file = file,
                        showThumbnail = showThumbnails,
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        onAddToPlaylist = { onAddToPlaylist(file) }
                    )
                }
                
                // 加载更多指示器
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
        ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                state = gridState,
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sortedFiles,
                    key = { file -> file.path } // 使用stable key提高性能
                ) { file ->
                    EnhancedVideoGridItem(
                        file = file,
                        showThumbnail = showThumbnails,
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        onAddToPlaylist = { onAddToPlaylist(file) }
                    )
                }
                
                // 加载更多指示器
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedVideoListItem(
    file: SmbFileInfo,
    showThumbnail: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 缩略图或文件图标
            if (showThumbnail && file.isVideoFile) {
                Box(
                    modifier = Modifier
                        .size(80.dp, 60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.thumbnailPath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file.thumbnailPath)
                                .crossfade(true)
                                .memoryCacheKey(file.path) // 使用文件路径作为缓存键
                                .diskCacheKey(file.path)
                                .placeholder(ColorDrawable(androidx.compose.ui.graphics.Color.Gray.toArgb()))
                                .error(ColorDrawable(androidx.compose.ui.graphics.Color.Red.toArgb()))
                                .build(),
                            contentDescription = "视频缩略图",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = "视频文件",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 时长标签
                    if (file.displayDuration.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = file.displayDuration,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (file.isDirectory) Icons.Default.Folder else Icons.Default.VideoFile,
                        contentDescription = if (file.isDirectory) "文件夹" else "视频文件",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // 文件信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (file.isVideoFile) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            if (file.resolution.isNotEmpty()) {
                                VideoInfoChip(
                                    text = file.resolution,
                                    icon = Icons.Default.AspectRatio
                                )
                            }
                        }
                        
                        item {
                            VideoInfoChip(
                                text = file.displaySize,
                                icon = Icons.Default.Storage
                            )
                        }
                        
                        item {
                            if (file.displayBitrate.isNotEmpty()) {
                                VideoInfoChip(
                                    text = file.displayBitrate,
                                    icon = Icons.Default.Speed
                                )
                            }
                        }
                        
                        item {
                            if (file.codecName != null) {
                                VideoInfoChip(
                                    text = file.codecName,
                                    icon = Icons.Default.Code
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (file.isDirectory) "文件夹" else file.displaySize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 菜单按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (file.isVideoFile) {
                        DropdownMenuItem(
                            text = { Text("添加到播放列表") },
                            onClick = {
                                onAddToPlaylist()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                            }
                        )
                    }
                    
                    DropdownMenuItem(
                        text = { Text("详细信息") },
                        onClick = {
                            onLongClick()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedVideoGridItem(
    file: SmbFileInfo,
    showThumbnail: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // 缩略图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (showThumbnail && file.isVideoFile && file.thumbnailPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(file.thumbnailPath)
                            .crossfade(true)
                            .memoryCacheKey(file.path) // 使用文件路径作为缓存键
                            .diskCacheKey(file.path)  
                            .placeholder(ColorDrawable(androidx.compose.ui.graphics.Color.Gray.toArgb()))
                            .error(ColorDrawable(androidx.compose.ui.graphics.Color.Red.toArgb()))
                            .build(),
                        contentDescription = "视频缩略图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (file.isDirectory) Icons.Default.Folder else Icons.Default.VideoFile,
                        contentDescription = if (file.isDirectory) "文件夹" else "视频文件",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 时长标签
                if (file.displayDuration.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.displayDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
                
                // 菜单按钮
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多选项",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (file.isVideoFile) {
                            DropdownMenuItem(
                                text = { Text("添加到播放列表") },
                                onClick = {
                                    onAddToPlaylist()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                                }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("详细信息") },
                            onClick = {
                                onLongClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )
                    }
                }
            }
            
            // 文件信息区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (file.isVideoFile) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (file.resolution.isNotEmpty()) {
                            Text(
                                text = file.resolution,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = file.displaySize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (file.isDirectory) "文件夹" else file.displaySize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VideoInfoChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun VideoListToolbar(
    sortOrder: SortOrder,
    viewMode: ViewMode,
    showThumbnails: Boolean,
    onSortOrderChange: (SortOrder) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onShowThumbnailsChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        onSearch(it)
                    },
                    placeholder = { Text("搜索视频...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false
                            searchQuery = ""
                            onSearch("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                        }
                    }
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 搜索按钮
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    
                    // 排序按钮
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "排序")
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("按名称 A-Z") },
                                onClick = {
                                    onSortOrderChange(SortOrder.NAME_ASC)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("按名称 Z-A") },
                                onClick = {
                                    onSortOrderChange(SortOrder.NAME_DESC)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("按大小 小-大") },
                                onClick = {
                                    onSortOrderChange(SortOrder.SIZE_ASC)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("按大小 大-小") },
                                onClick = {
                                    onSortOrderChange(SortOrder.SIZE_DESC)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("按时长 短-长") },
                                onClick = {
                                    onSortOrderChange(SortOrder.DURATION_ASC)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("按时长 长-短") },
                                onClick = {
                                    onSortOrderChange(SortOrder.DURATION_DESC)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                            )
                        }
                    }
                    
                    // 视图模式切换
                    IconButton(
                        onClick = { 
                            onViewModeChange(
                                if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            )
                        }
                    ) {
                        Icon(
                            if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "切换视图模式"
                        )
                    }
                    
                    // 缩略图切换
                    IconButton(
                        onClick = { onShowThumbnailsChange(!showThumbnails) }
                    ) {
                        Icon(
                            if (showThumbnails) Icons.Default.Image else Icons.Default.ImageNotSupported,
                            contentDescription = "切换缩略图显示",
                            tint = if (showThumbnails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}