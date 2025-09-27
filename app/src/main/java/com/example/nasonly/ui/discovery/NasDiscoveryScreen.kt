package com.example.nasonly.ui.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.model.NasDevice
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NasDiscoveryScreen(
    navController: NavController,
    viewModel: NasDiscoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val discoveryState by viewModel.discoveryState.collectAsState()
    var showConnectionDialog by remember { mutableStateOf<NasDevice?>(null) }

    /** 收集一次性导航事件：保证在主线程里执行 navigate */
    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { evt ->
            when (evt) {
                is NasDiscoveryViewModel.NavEvent.ToLibrary -> {
                    val host = evt.host
                    Timber.i("Navigating to media for ${host.host} / ${host.share}")
                    try {
                        navController.navigate("media/${host.host}?share=${host.share}") {
                            popUpTo("discovery") { inclusive = true }
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to navigate to media library for host: ${host.host}")
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // 标题和发现按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "NAS 设备发现",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Button(
                onClick = {
                    if (discoveryState.isDiscovering) {
                        viewModel.stopDiscovery()
                    } else {
                        viewModel.startDiscovery()
                    }
                },
                enabled = !discoveryState.isDiscovering,
            ) {
                Icon(
                    imageVector = if (discoveryState.isDiscovering) Icons.Default.Settings else Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (discoveryState.isDiscovering) "停止发现" else "开始发现")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 连接状态显示
        when (uiState) {
            is NasDiscoveryViewModel.UiState.Connected -> {
                val host = (uiState as NasDiscoveryViewModel.UiState.Connected).host
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "已连接到: ${host.host}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Share: ${host.share}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("断开")
                        }
                    }
                }
            }
            is NasDiscoveryViewModel.UiState.Error -> {
                val msg = (uiState as NasDiscoveryViewModel.UiState.Error).message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            NasDiscoveryViewModel.UiState.Connecting -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("正在连接 NAS…")
                    }
                }
            }
            NasDiscoveryViewModel.UiState.Idle -> {
                // 显示发现的设备
                if (discoveryState.isDiscovering) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在搜索网络中的NAS设备...")
                        }
                    }
                }

                if (discoveryState.devices.isNotEmpty()) {
                    Text(
                        text = "发现的设备 (${discoveryState.devices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(discoveryState.devices) { device ->
                        DeviceCard(
                            device = device,
                            isConnecting = uiState is NasDiscoveryViewModel.UiState.Connecting,
                            onConnect = { showConnectionDialog = device },
                        )
                    }
                }

                // 显示错误
                discoveryState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }

    // 连接对话框
    showConnectionDialog?.let { device ->
        ConnectionDialog(
            device = device,
            onConnect = { username, password ->
                viewModel.connectToDevice(device, username, password)
                showConnectionDialog = null
            },
            onDismiss = { showConnectionDialog = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    device: NasDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!isConnecting) onConnect() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "未知设备",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "IP: ${device.ip.hostAddress}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (device.reachable) "状态: 可达" else "状态: 不可达",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.reachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionDialog(
    device: NasDevice,
    onConnect: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("连接到 ${device.name ?: "未知设备"}")
        },
        text = {
            Column {
                Text(
                    text = "请输入SMB连接凭据",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank(),
            ) {
                Text("连接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
