package com.example.nasonly.repository

import android.util.Log
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbConnectionResult
import com.example.nasonly.data.smb.SmbDataSource
import com.example.nasonly.data.smb.SmbFileInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbRepository @Inject constructor(
    private val smbDataSource: SmbDataSource,
    private val smbConnectionManager: SmbConnectionManager
) {
    companion object {
        private const val TAG = "SmbRepository"
    }

    suspend fun configureConnection(
        host: String,
        share: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<String> {
        return try {
            Log.d(TAG, "Configuring SMB connection for host: $host")
            smbConnectionManager.configure(host, share, username, password, domain)
            Result.success("SMB 配置已保存")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring SMB connection", e)
            Result.failure(Exception("配置 SMB 连接失败: ${e.message}"))
        }
    }

    suspend fun testConnection(): Result<String> {
        return try {
            Log.d(TAG, "Testing SMB connection...")
            // 使用异步版本的连接验证，确保在IO线程中执行网络操作
            // 生产环境不能在主线程访问网络
            when (val result = smbConnectionManager.validateConnectionAsync()) {
                is SmbConnectionResult.Success -> {
                    Log.i(TAG, "SMB connection test successful")
                    Result.success(result.message)
                }
                is SmbConnectionResult.Error -> {
                    Log.w(TAG, "SMB connection test failed: ${result.message}")
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMB connection test exception", e)
            Result.failure(Exception("连接测试异常: ${e.message}"))
        }
    }

    suspend fun getInputStream(path: String): Result<InputStream> {
        return try {
            Log.d(TAG, "Getting input stream for path: $path")
            smbDataSource.getInputStream(path)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting input stream", e)
            Result.failure(Exception("获取文件流失败: ${e.message}"))
        }
    }

    suspend fun seekStream(inputStream: InputStream, position: Long): Result<Boolean> {
        return try {
            Log.d(TAG, "Seeking stream to position: $position")
            smbDataSource.seekStream(inputStream, position)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking stream", e)
            Result.failure(Exception("定位文件位置失败: ${e.message}"))
        }
    }

    suspend fun listFiles(directoryPath: String = ""): Result<List<SmbFileInfo>> {
        return try {
            Log.d(TAG, "Listing files in directory: $directoryPath")
            smbDataSource.listFiles(directoryPath)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            Result.failure(Exception("获取文件列表失败: ${e.message}"))
        }
    }

    suspend fun listFilesWithMetadata(
        directoryPath: String = "",
        includeMetadata: Boolean = true,
        generateThumbnails: Boolean = true
    ): Result<List<SmbFileInfo>> {
        return try {
            Log.d(TAG, "Listing files with metadata in directory: $directoryPath")
            smbDataSource.listFilesWithMetadata(directoryPath, includeMetadata, generateThumbnails)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files with metadata", e)
            Result.failure(Exception("获取增强文件列表失败: ${e.message}"))
        }
    }

    fun getConnectionStatus(): Flow<Boolean> = flow {
        try {
            emit(smbConnectionManager.isConnected())
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connection status", e)
            emit(false)
        }
    }

    suspend fun disconnect(): Result<String> {
        return try {
            Log.d(TAG, "Disconnecting SMB connection")
            smbConnectionManager.disconnect()
            smbDataSource.close()
            Result.success("已断开 SMB 连接")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting SMB", e)
            Result.failure(Exception("断开连接失败: ${e.message}"))
        }
    }

    fun close() {
        try {
            smbDataSource.close()
            Log.d(TAG, "SMB repository closed")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing SMB repository: ${e.message}")
        }
    }
}
