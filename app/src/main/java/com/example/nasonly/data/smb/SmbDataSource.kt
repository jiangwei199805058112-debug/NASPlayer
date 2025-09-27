package com.example.nasonly.data.smb

import android.util.Log
import com.example.nasonly.data.media.VideoMetadataExtractor
import com.example.nasonly.data.smb.SmbFileWithMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbDataSource @Inject constructor(
    private val smbManager: SmbManager,
    private val smbConnectionManager: SmbConnectionManager,
    private val videoMetadataExtractor: VideoMetadataExtractor,
) {
    /**
     * 枚举顶层共享列表
     */
    suspend fun listShares(host: String, creds: SmbCredentials): Result<List<SmbShareInfo>> = withContext(Dispatchers.IO) {
        try {
            if (smbManager is SmbConnectionManager) {
                val shares = smbManager.listShares(host, creds)
                Result.success(shares)
            } else {
                Result.failure(IOException("SMB manager is not SmbConnectionManager"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing shares", e)
            Result.failure(e)
        }
    }
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

    suspend fun listFiles(shareName: String, relativePath: String = ""): Result<List<SmbFileInfo>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listing files in share: $shareName, path: $relativePath")
            if (!smbManager.isConnected()) {
                Log.d(TAG, "SMB not connected, attempting to connect...")
                if (!smbManager.connect()) {
                    return@withContext Result.failure(IOException("无法连接到 SMB 服务器"))
                }
            }
            val files = if (smbManager is SmbConnectionManager) {
                smbManager.listDirectory(shareName, relativePath)
            } else {
                Log.w(TAG, "SMB manager is not SmbConnectionManager, cannot list directory")
                emptyList()
            }
            Log.d(TAG, "Found ${files.size} files/folders")
            Result.success(files)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            Result.failure(e)
        }
    }

    suspend fun listFilesWithMetadata(
        shareName: String,
        relativePath: String = "",
        includeMetadata: Boolean = true,
        generateThumbnails: Boolean = true,
    ): Result<List<SmbFileWithMetadata>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listing files with metadata in share: $shareName, path: $relativePath")
            // 直接使用SmbConnectionManager列出文件
            val basicFiles = smbConnectionManager.listDirectory(shareName, relativePath)
            val enhancedFiles = mutableListOf<SmbFileWithMetadata>()
            for (fileInfo in basicFiles) {
                val isVideoFile = !fileInfo.isDirectory && fileInfo.name.lowercase().let { name ->
                    name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
                    name.endsWith(".mov") || name.endsWith(".wmv") || name.endsWith(".flv") ||
                    name.endsWith(".webm") || name.endsWith(".m4v") || name.endsWith(".3gp")
                }
                val metadata = if (isVideoFile && includeMetadata) {
                    try {
                        videoMetadataExtractor.extractMetadata(fileInfo.path)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to extract metadata for ${fileInfo.path}", e)
                        null
                    }
                } else null

                val smbFile = SmbFile(
                    name = fileInfo.name,
                    isDirectory = fileInfo.isDirectory,
                    size = fileInfo.size,
                    lastModified = java.util.Date(), // TODO: 从SMB获取实际修改时间
                    fullPath = fileInfo.path
                )

                enhancedFiles.add(SmbFileWithMetadata(
                    file = smbFile,
                    metadata = metadata
                ))
            }
            Log.d(TAG, "Enhanced ${enhancedFiles.size} files with metadata")
            Result.success(enhancedFiles)
        } catch (e: Exception) {
            Result.failure(e)
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