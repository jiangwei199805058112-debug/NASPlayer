package com.example.nasonly

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.nasonly.navigation.NavGraph
import com.example.nasonly.ui.theme.NASPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Notification permission granted
        } else {
            // Notification permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在调试模式下放开StrictMode网络限制，方便开发调试
        // 生产环境不能在主线程访问网络，必须使用协程的IO线程
        try {
            // 检测是否为调试模式
            val isDebugMode = (0 != (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE))
            if (isDebugMode) {
                val policy = StrictMode.ThreadPolicy.Builder()
                    .permitNetwork() // 调试模式临时允许主线程网络访问
                    .build()
                StrictMode.setThreadPolicy(policy)
            }
        } catch (e: Exception) {
            // 忽略StrictMode配置错误
        }

        // Request notification permission for Android 13+
        requestNotificationPermission()

        setContent {
            NASPlayerTheme {
                Surface {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
