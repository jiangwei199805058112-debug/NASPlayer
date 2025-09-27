package com.example.nasonly.data.smb

import android.util.Log
import com.example.nasonly.data.media.VideoMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbDataSource @Inject constructor(
    private val smbManager: SmbManager,
    private val videoMetadataExtractor: VideoMetadataExtractor,
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

            if (!smbManager.isConnected()) {
                Log.d(TAG, "SMB not connected, attempting to connect...")
                if (!smbManager.connect()) {
                    return@withContext Result.failure(IOException("无法连接到 SMB 服务器"))
                }
            }

            // 使用真实的 SMB 文件列表获取逻辑
            val files = if (smbManager is SmbConnectionManager) {
                smbManager.listDirectory(directoryPath)
            } else {
                // 如果不是我们的实现，返回空列表
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

    /**
     * 获取增强的文件信息，包含视频元数据和缩略图
     */
    suspend fun listFilesWithMetadata(
        directoryPath: String,
        includeMetadata: Boolean = true,
        generateThumbnails: Boolean = true,
    ): Result<List<SmbFileInfo>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listing files with metadata in directory: $directoryPath")

            // 首先获取基本文件信息
            val basicFilesResult = listFiles(directoryPath)
            if (basicFilesResult.isFailure) {
                return@withContext basicFilesResult
            }

            val basicFiles = basicFilesResult.getOrNull() ?: emptyList()
            val enhancedFiles = mutableListOf<SmbFileInfo>()

            for (fileInfo in basicFiles) {
                var enhancedFile = fileInfo

                // 如果是视频文件且需要元数据
                if (fileInfo.isVideoFile && includeMetadata) {
                    try {
                        val metadata = videoMetadataExtractor.extractMetadata(fileInfo.path)
                        var thumbnailPath: String? = null

                        // 生成缩略图
                        if (generateThumbnails) {
                            thumbnailPath = videoMetadataExtractor.generateThumbnail(fileInfo.path)
                        }

                        enhancedFile = fileInfo.copy(
                            duration = metadata.duration,
                            thumbnailPath = thumbnailPath,
                            videoWidth = metadata.width,
                            videoHeight = metadata.height,
                            frameRate = metadata.frameRate,
                            bitrate = metadata.bitrate,
                            codecName = metadata.codecName,
                            audioChannels = metadata.audioChannels,
                            audioSampleRate = metadata.audioSampleRate,
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to extract metadata for ${fileInfo.path}", e)
                        // 使用原始文件信息
                    }
                }

                enhancedFiles.add(enhancedFile)
            }

            Log.d(TAG, "Enhanced ${enhancedFiles.size} files with metadata")
            Result.success(enhancedFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files with metadata", e)
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

data class SmbFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long = 0L,
    val duration: Long = 0L,
    val thumbnailPath: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val frameRate: Float = 0f,
    val bitrate: Long = 0L,
    val codecName: String? = null,
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0,
) {
    val isVideoFile: Boolean
        get() = !isDirectory && name.lowercase().run {
            endsWith(".mp4") || endsWith(".mkv") || endsWith(".avi") ||
                endsWith(".mov") || endsWith(".wmv") || endsWith(".flv") ||
                endsWith(".webm") || endsWith(".m4v") || endsWith(".3gp")
        }

    val displaySize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
            size < 1024 * 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
            else -> "%.1f GB".format(size / (1024.0 * 1024.0 * 1024.0))
        }

    val displayDuration: String
        get() = if (duration > 0) {
            val hours = duration / 3600000
            val minutes = (duration % 3600000) / 60000
            val seconds = (duration % 60000) / 1000
            when {
                hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
                else -> "%d:%02d".format(minutes, seconds)
            }
        } else {
            ""
        }

    val resolution: String
        get() = if (videoWidth > 0 && videoHeight > 0) {
            "${videoWidth}x$videoHeight"
        } else {
            ""
        }

    val displayBitrate: String
        get() = if (bitrate > 0) {
            when {
                bitrate < 1000 -> "$bitrate bps"
                bitrate < 1000_000 -> "%.1f Kbps".format(bitrate / 1000.0)
                else -> "%.1f Mbps".format(bitrate / 1000_000.0)
            }
        } else {
            ""
        }
}
