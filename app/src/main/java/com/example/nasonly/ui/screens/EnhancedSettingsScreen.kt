package com.example.nasonly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nasonly.ui.viewmodel.NasConfigViewModel
import com.example.nasonly.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    onNavigateBack: () -> Unit,
    nasConfigViewModel: NasConfigViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("NAS配置", "播放器设置", "系统设置", "界面设置")
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        // 标签页
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // 标签页内容
        when (selectedTab) {
            0 -> NasConfigTab(nasConfigViewModel)
            1 -> PlayerSettingsTab(settingsViewModel)
            2 -> SystemSettingsTab(settingsViewModel)
            3 -> UISettingsTab(settingsViewModel)
        }
    }
}

@Composable
fun NasConfigTab(viewModel: NasConfigViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    var host by remember { mutableStateOf(uiState.host) }
    var share by remember { mutableStateOf(uiState.share) }
    var username by remember { mutableStateOf(uiState.username) }
    var password by remember { mutableStateOf(uiState.password) }
    var domain by remember { mutableStateOf(uiState.domain) }
    var smbVersion by remember { mutableStateOf(uiState.smbVersion) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingGroup(title = "NAS 连接配置") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("主机地址") },
                        placeholder = { Text("例如: 192.168.1.100") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = share,
                        onValueChange = { share = it },
                        label = { Text("共享目录") },
                        placeholder = { Text("例如: media") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("域名 (可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = smbVersion,
                        onValueChange = { smbVersion = it },
                        label = { Text("SMB 版本 (可选)") },
                        placeholder = { Text("例如: 2.1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.testConnection(host, share, username, password, domain, smbVersion)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("测试连接")
                    }
                }
                
                Button(
                    onClick = {
                        viewModel.saveConfig(host, share, username, password, domain, smbVersion)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = host.isNotEmpty() && username.isNotEmpty() && !uiState.isLoading
                ) {
                    Text("保存配置")
                }
            }
        }
        
        item {
            val testResult = uiState.testResult
            if (testResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult.contains("成功")) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Text(
                        text = testResult,
                        modifier = Modifier.padding(16.dp),
                        color = if (testResult.contains("成功")) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSettingsTab(viewModel: SettingsViewModel) {
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val resumePlayback by viewModel.resumePlayback.collectAsState()
    val screenOrientation by viewModel.screenOrientation.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingGroup(title = "显示设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownSetting(
                        title = "屏幕方向",
                        description = getOrientationDisplayName(screenOrientation),
                        options = listOf(
                            "auto" to "自动旋转",
                            "portrait" to "竖屏锁定",
                            "landscape" to "横屏锁定"
                        ),
                        selectedValue = screenOrientation,
                        onValueChange = viewModel::setScreenOrientation
                    )
                }
            }
        }
        
        item {
            SettingGroup(title = "播放控制") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeedSetting(
                        title = "播放速度",
                        description = "${playbackSpeed}x",
                        speed = playbackSpeed,
                        onSpeedChange = viewModel::setPlaybackSpeed
                    )
                    
                    SwitchSetting(
                        title = "自动播放下一个",
                        description = "当前视频播放完毕后自动播放下一个",
                        checked = autoPlayNext,
                        onCheckedChange = viewModel::setAutoPlayNext
                    )
                    
                    SwitchSetting(
                        title = "恢复播放位置",
                        description = "从上次停止的位置继续播放",
                        checked = resumePlayback,
                        onCheckedChange = viewModel::setResumePlayback
                    )
                }
            }
        }
    }
}

@Composable
fun SystemSettingsTab(viewModel: SettingsViewModel) {
    val cacheSizeMB by viewModel.cacheSizeMB.collectAsState()
    val networkTimeoutSeconds by viewModel.networkTimeoutSeconds.collectAsState()
    val bufferSizeKB by viewModel.bufferSizeKB.collectAsState()
    val maxBufferMs by viewModel.maxBufferMs.collectAsState()
    val minBufferMs by viewModel.minBufferMs.collectAsState()
    val autoClearCache by viewModel.autoClearCache.collectAsState()
    val wifiOnlyStreaming by viewModel.wifiOnlyStreaming.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingGroup(title = "缓存设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SliderSetting(
                        title = "缓存大小",
                        description = "${cacheSizeMB}MB",
                        value = cacheSizeMB.toFloat(),
                        valueRange = 50f..1000f,
                        steps = 19,
                        onValueChange = { viewModel.setCacheSizeMB(it.roundToInt()) }
                    )
                    
                    SwitchSetting(
                        title = "自动清理缓存",
                        description = "应用退出时自动清理缓存文件",
                        checked = autoClearCache,
                        onCheckedChange = viewModel::setAutoClearCache
                    )
                }
            }
        }
        
        item {
            SettingGroup(title = "网络设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SliderSetting(
                        title = "网络超时",
                        description = "${networkTimeoutSeconds}秒",
                        value = networkTimeoutSeconds.toFloat(),
                        valueRange = 10f..120f,
                        steps = 21,
                        onValueChange = { viewModel.setNetworkTimeoutSeconds(it.roundToInt()) }
                    )
                    
                    SwitchSetting(
                        title = "仅WiFi播放",
                        description = "仅在WiFi连接时允许视频流播放",
                        checked = wifiOnlyStreaming,
                        onCheckedChange = viewModel::setWifiOnlyStreaming
                    )
                }
            }
        }
        
        item {
            SettingGroup(title = "缓冲设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SliderSetting(
                        title = "缓冲区大小",
                        description = "${bufferSizeKB}KB",
                        value = bufferSizeKB.toFloat(),
                        valueRange = 32f..256f,
                        steps = 7,
                        onValueChange = { viewModel.setBufferSizeKB(it.roundToInt()) }
                    )
                    
                    SliderSetting(
                        title = "最大缓冲时间",
                        description = "${maxBufferMs / 1000}秒",
                        value = (maxBufferMs / 1000).toFloat(),
                        valueRange = 15f..60f,
                        steps = 8,
                        onValueChange = { viewModel.setMaxBufferMs((it * 1000).roundToInt()) }
                    )
                    
                    SliderSetting(
                        title = "最小缓冲时间",
                        description = "${minBufferMs / 1000}秒",
                        value = (minBufferMs / 1000).toFloat(),
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { viewModel.setMinBufferMs((it * 1000).roundToInt()) }
                    )
                }
            }
        }
    }
}

@Composable
fun UISettingsTab(viewModel: SettingsViewModel) {
    val darkMode by viewModel.darkMode.collectAsState()
    val showThumbnails by viewModel.showThumbnails.collectAsState()
    val gridLayout by viewModel.gridLayout.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingGroup(title = "外观设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownSetting(
                        title = "主题模式",
                        description = getDarkModeDisplayName(darkMode),
                        options = listOf(
                            "auto" to "跟随系统",
                            "light" to "浅色模式",
                            "dark" to "深色模式"
                        ),
                        selectedValue = darkMode,
                        onValueChange = viewModel::setDarkMode
                    )
                }
            }
        }
        
        item {
            SettingGroup(title = "列表设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchSetting(
                        title = "显示缩略图",
                        description = "在视频列表中显示缩略图预览",
                        checked = showThumbnails,
                        onCheckedChange = viewModel::setShowThumbnails
                    )
                    
                    SwitchSetting(
                        title = "网格布局",
                        description = "使用网格布局显示视频列表",
                        checked = gridLayout,
                        onCheckedChange = viewModel::setGridLayout
                    )
                    
                    DropdownSetting(
                        title = "排序方式",
                        description = getSortOrderDisplayName(sortOrder),
                        options = listOf(
                            "name" to "按名称",
                            "date" to "按日期",
                            "size" to "按大小"
                        ),
                        selectedValue = sortOrder,
                        onValueChange = viewModel::setSortOrder
                    )
                    
                    SwitchSetting(
                        title = "升序排列",
                        description = "启用升序排列，关闭为降序",
                        checked = sortAscending,
                        onCheckedChange = viewModel::setSortAscending
                    )
                }
            }
        }
    }
}

@Composable
fun SettingGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DropdownSetting(
    title: String,
    description: String,
    options: List<Pair<String, String>>,
    @Suppress("UNUSED_PARAMETER") selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SpeedSetting(
    title: String,
    description: String,
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(speeds) { speedOption ->
                FilterChip(
                    onClick = { onSpeedChange(speedOption) },
                    label = { Text("${speedOption}x") },
                    selected = speed == speedOption
                )
            }
        }
    }
}

private fun getLanguageDisplayName(language: String): String {
    return when (language) {
        "auto" -> "自动检测"
        "zh" -> "中文"
        "en" -> "英文"
        "ja" -> "日文"
        "ko" -> "韩文"
        else -> "自动检测"
    }
}

private fun getOrientationDisplayName(orientation: String): String {
    return when (orientation) {
        "auto" -> "自动旋转"
        "portrait" -> "竖屏锁定"
        "landscape" -> "横屏锁定"
        else -> "自动旋转"
    }
}

private fun getDarkModeDisplayName(mode: String): String {
    return when (mode) {
        "auto" -> "跟随系统"
        "light" -> "浅色模式"
        "dark" -> "深色模式"
        else -> "跟随系统"
    }
}

private fun getSortOrderDisplayName(order: String): String {
    return when (order) {
        "name" -> "按名称"
        "date" -> "按日期"
        "size" -> "按大小"
        else -> "按名称"
    }
}