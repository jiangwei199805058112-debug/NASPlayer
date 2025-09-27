package com.example.nasonly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.core.ui.components.ErrorDialog
import com.example.nasonly.navigation.Routes
import com.example.nasonly.ui.viewmodel.NasConfigViewModel

data class SmbProtocol(val name: String, val value: String)
data class SharedFolder(val name: String, val path: String, var isSelected: Boolean = false)
data class DiscoveredNas(val name: String, val ip: String, val icon: ImageVector = Icons.Default.Computer)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NasConfigScreen(
    navController: NavController,
    viewModel: NasConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var host by remember { mutableStateOf(uiState.host) }
    var username by remember { mutableStateOf(uiState.username) }
    var password by remember { mutableStateOf(uiState.password) }
    var selectedSmbProtocol by remember { mutableStateOf("Auto") }
    var discoveredDevices by remember { mutableStateOf(listOf<DiscoveredNas>()) }
    var sharedFolders by remember { mutableStateOf(listOf<SharedFolder>()) }
    var showFolderSelection by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }

    val smbProtocols = listOf(
        SmbProtocol("Auto (推荐)", "Auto"),
        SmbProtocol("SMB3", "3.0"),
        SmbProtocol("SMB2", "2.1"),
    )

    LaunchedEffect(Unit) {
        // 自动发现 NAS 设备
        isDiscovering = true
        // 模拟发现设备（实际应该调用 viewModel 的发现方法）
        kotlinx.coroutines.delay(2000)
        discoveredDevices = listOf(
            DiscoveredNas("QNAP-NAS", "192.168.1.100"),
            DiscoveredNas("Synology-DS", "192.168.1.101"),
            DiscoveredNas("Windows-Share", "192.168.1.102"),
        )
        isDiscovering = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "NAS 配置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // 自动发现区域
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索设备",
                    )
                    Text(
                        text = "发现的设备",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { showDiscoveryDialog = true },
                    ) {
                        Text("查看全部")
                    }
                }

                if (discoveredDevices.isNotEmpty() && !isDiscovering) {
                    discoveredDevices.take(2).forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = device.icon,
                                contentDescription = device.name,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = device.ip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = { host = device.ip },
                            ) {
                                Text("选择")
                            }
                        }
                    }
                }
            }
        }

        // NAS 连接配置
        OutlinedTextField(
            value = host,
            onValueChange = {
                host = it
                showFolderSelection = false
                sharedFolders = emptyList()
            },
            label = { Text("NAS 地址") },
            placeholder = { Text("例如: 192.168.1.100") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = "NAS地址",
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "用户名",
                    )
                },
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "密码",
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }

        // SMB 协议选择
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedSmbProtocol,
                onValueChange = { },
                readOnly = true,
                label = { Text("SMB 协议") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "SMB协议",
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                smbProtocols.forEach { protocol ->
                    DropdownMenuItem(
                        text = { Text(protocol.name) },
                        onClick = {
                            selectedSmbProtocol = protocol.name
                            expanded = false
                        },
                    )
                }
            }
        }

        // 共享文件夹选择（仅在连接成功后显示）
        if (showFolderSelection && sharedFolders.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "选择共享文件夹",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    sharedFolders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = folder.isSelected,
                                    onClick = {
                                        sharedFolders = sharedFolders.map {
                                            if (it.name == folder.name) {
                                                it.copy(isSelected = !it.isSelected)
                                            } else {
                                                it
                                            }
                                        }
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = folder.isSelected,
                                onCheckedChange = { isChecked ->
                                    sharedFolders = sharedFolders.map {
                                        if (it.name == folder.name) {
                                            it.copy(isSelected = isChecked)
                                        } else {
                                            it
                                        }
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "文件夹",
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = folder.name)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    // 测试连接，成功后显示共享文件夹
                    if (host.isNotEmpty() && username.isNotEmpty()) {
                        // 模拟连接测试和获取共享文件夹
                        viewModel.testConnection(host, "", username, password, "", selectedSmbProtocol)
                        // 模拟获取到的共享文件夹
                        sharedFolders = listOf(
                            SharedFolder("Video", "/Video"),
                            SharedFolder("Media", "/Media"),
                            SharedFolder("Movies", "/Movies"),
                            SharedFolder("Photos", "/Photos"),
                        )
                        showFolderSelection = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = host.isNotEmpty() && username.isNotEmpty() && !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "测试连接",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("测试连接")
                }
            }

            Button(
                onClick = {
                    val selectedFolders = sharedFolders.filter { it.isSelected }
                    val shareString = selectedFolders.joinToString(",") { it.path }
                    viewModel.saveConfig(host, shareString, username, password, "", selectedSmbProtocol)
                    navController.navigate(Routes.MEDIA_LIBRARY) {
                        popUpTo(Routes.NAS_CONFIG) { inclusive = true }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = host.isNotEmpty() && username.isNotEmpty() &&
                    (!showFolderSelection || sharedFolders.any { it.isSelected }) &&
                    !uiState.isLoading,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "保存配置",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存并继续")
            }
        }

        // 连接测试结果
        val testResult = uiState.testResult
        if (testResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (testResult.contains("成功")) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (testResult.contains("成功")) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (testResult.contains("成功")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = testResult,
                        color = if (testResult.contains("成功")) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }
        }
    }

    // 发现设备对话框
    if (showDiscoveryDialog) {
        AlertDialog(
            onDismissRequest = { showDiscoveryDialog = false },
            title = { Text("发现的 NAS 设备") },
            text = {
                LazyColumn {
                    items(discoveredDevices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = device.icon,
                                contentDescription = device.name,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = device.ip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = {
                                    host = device.ip
                                    showDiscoveryDialog = false
                                },
                            ) {
                                Text("选择")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDiscoveryDialog = false },
                ) {
                    Text("关闭")
                }
            },
        )
    }

    // 错误对话框
    val errorMessage = uiState.error
    if (errorMessage != null) {
        ErrorDialog(
            message = errorMessage,
            onDismiss = { viewModel.clearError() },
            onRetry = {
                viewModel.testConnection(host, "", username, password, "", selectedSmbProtocol)
            },
        )
    }
}
