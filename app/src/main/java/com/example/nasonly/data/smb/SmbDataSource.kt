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
    private val metadataCache: com.example.nasonly.data.cache.MediaMetadataCache
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
        generateThumbnails: Boolean = true
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
                    // 首先检查缓存
                    val cachedFile = metadataCache.getFileMetadata(fileInfo.path)
                    if (cachedFile != null) {
                        enhancedFile = cachedFile
                        Log.d(TAG, "Using cached metadata for: ${fileInfo.path}")
                    } else {
                        // 检查是否正在处理中
                        if (!metadataCache.isProcessing(fileInfo.path)) {
                            // 标记为正在处理
                            if (metadataCache.markProcessing(fileInfo.path)) {
                                try {
                                    val metadata = videoMetadataExtractor.extractMetadata(fileInfo.path)
                                    var thumbnailPath: String? = null
                                    
                                    // 检查缩略图缓存
                                    if (generateThumbnails) {
                                        thumbnailPath = metadataCache.getThumbnailPath(fileInfo.path)
                                        if (thumbnailPath == null) {
                                            thumbnailPath = videoMetadataExtractor.generateThumbnail(fileInfo.path)
                                            if (thumbnailPath != null) {
                                                metadataCache.cacheThumbnailPath(fileInfo.path, thumbnailPath)
                                            }
                                        }
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
                                        audioSampleRate = metadata.audioSampleRate
                                    )
                                    
                                    // 缓存增强后的文件信息
                                    metadataCache.cacheFileMetadata(fileInfo.path, enhancedFile)
                                    
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to extract metadata for ${fileInfo.path}", e)
                                    // 使用原始文件信息
                                } finally {
                                    // 标记处理完成
                                    metadataCache.markProcessed(fileInfo.path)
                                }
                            } else {
                                // 如果正在处理中，使用原始文件信息
                                Log.d(TAG, "File already processing: ${fileInfo.path}")
                            }
                        }
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
    val audioSampleRate: Int = 0
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
        } else ""
    
    val resolution: String
        get() = if (videoWidth > 0 && videoHeight > 0) {
            "${videoWidth}x${videoHeight}"
        } else ""
    
    val displayBitrate: String
        get() = if (bitrate > 0) {
            when {
                bitrate < 1000 -> "${bitrate} bps"
                bitrate < 1000_000 -> "%.1f Kbps".format(bitrate / 1000.0)
                else -> "%.1f Mbps".format(bitrate / 1000_000.0)
            }
        } else ""
}
