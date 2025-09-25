package com.example.nasonly.data.smb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbDataSource @Inject constructor(
    private val smbManager: SmbManager
) {
    companion object {
        private const val TAG = "SmbDataSource"
        private const val MAX_RETRY_COUNT = 3
    }

    suspend fun getInputStream(path: String): Result<InputStream> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting input stream for path: $path")
            
            var retryCount = 0
            while (retryCount < MAX_RETRY_COUNT) {
                if (!smbManager.isConnected()) {
                    Log.d(TAG, "SMB not connected, attempting to connect...")
                    if (!smbManager.connect()) {
                        retryCount++
                        if (retryCount >= MAX_RETRY_COUNT) {
                            return@withContext Result.failure(IOException("无法连接到 SMB 服务器，已重试 $MAX_RETRY_COUNT 次"))
                        }
                        continue
                    }
                }

                val inputStream = smbManager.openInputStream(path)
                if (inputStream != null) {
                    Log.d(TAG, "Successfully opened input stream for: $path")
                    return@withContext Result.success(inputStream)
                } else {
                    retryCount++
                    Log.w(TAG, "Failed to open input stream, retry count: $retryCount")
                }
            }
            
            Result.failure(IOException("打开文件失败: $path"))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting input stream for path: $path", e)
            Result.failure(IOException("获取文件流时发生错误: ${e.message}", e))
        }
    }

    suspend fun seekStream(inputStream: InputStream, position: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Seeking stream to position: $position")
            val success = smbManager.seek(inputStream, position)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(IOException("无法定位到指定位置: $position"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking stream to position: $position", e)
            Result.failure(IOException("定位文件位置时发生错误: ${e.message}", e))
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing SMB connection...")
            if (smbManager.connect()) {
                Result.success("SMB 连接测试成功")
            } else {
                Result.failure(IOException("SMB 连接测试失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMB connection test failed", e)
            Result.failure(IOException("连接测试异常: ${e.message}", e))
        }
    }

    suspend fun listFiles(directoryPath: String): Result<List<SmbFileInfo>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listing files in directory: $directoryPath")
            
            if (!smbManager.isConnected() && !smbManager.connect()) {
                return@withContext Result.failure(IOException("无法连接到 SMB 服务器"))
            }

            // 这里应该实现真实的文件列表获取逻辑
            // 为了演示，返回一些模拟数据
            val mockFiles = listOf(
                SmbFileInfo("video1.mp4", "$directoryPath/video1.mp4", 1024000L, false),
                SmbFileInfo("video2.mkv", "$directoryPath/video2.mkv", 2048000L, false),
                SmbFileInfo("movies", "$directoryPath/movies", 0L, true)
            )
            
            Log.d(TAG, "Found ${mockFiles.size} files/folders")
            Result.success(mockFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files in directory: $directoryPath", e)
            Result.failure(IOException("获取文件列表失败: ${e.message}", e))
        }
    }

    fun close() {
        try {
            smbManager.close()
            Log.d(TAG, "SMB data source closed")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing SMB data source: ${e.message}")
        }
    }
}

data class SmbFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean
)
