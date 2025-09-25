package com.example.nasonly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.ui.viewmodel.NasConfigViewModel
import com.example.nasonly.navigation.Routes
import com.example.nasonly.core.ui.components.LoadingIndicator
import com.example.nasonly.core.ui.components.ErrorDialog

@Composable
fun NasConfigScreen(
    navController: NavController,
    viewModel: NasConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var host by remember { mutableStateOf(uiState.host) }
    var share by remember { mutableStateOf(uiState.share) }
    var username by remember { mutableStateOf(uiState.username) }
    var password by remember { mutableStateOf(uiState.password) }
    var domain by remember { mutableStateOf(uiState.domain) }
    var smbVersion by remember { mutableStateOf(uiState.smbVersion) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "NAS 配置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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

        Spacer(modifier = Modifier.height(16.dp))

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
                    navController.navigate(Routes.MEDIA_LIBRARY) {
                        popUpTo(Routes.NAS_CONFIG) { inclusive = true }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = host.isNotEmpty() && username.isNotEmpty() && !uiState.isLoading
            ) {
                Text("保存并继续")
            }
        }

        val testResult = uiState.testResult
        if (testResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = testResult,
                    modifier = Modifier.padding(16.dp),
                    color = if (testResult.contains("成功")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
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
                viewModel.testConnection(host, share, username, password, domain, smbVersion)
            }
        )
    }
}