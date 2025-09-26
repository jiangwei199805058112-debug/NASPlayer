package com.example.nasonly.data.cache

import android.util.Log
import android.util.LruCache
import com.example.nasonly.data.smb.SmbFileInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 媒体文件元数据缓存管理器
 * 使用LRU缓存策略，避免重复的元数据提取和缩略图生成
 */
@Singleton  
class MediaMetadataCache @Inject constructor() {
    
    // 延迟注入性能监控器以避免循环依赖
    @Inject
    lateinit var performanceMonitor: com.example.nasonly.core.performance.PerformanceMonitor
    
    companion object {
        private const val TAG = "MediaMetadataCache"
        private const val MAX_CACHE_SIZE = 200 // 最大缓存文件数量
        private const val MAX_THUMBNAIL_CACHE_SIZE = 50 * 1024 * 1024 // 50MB缩略图缓存
    }
    
    // 文件元数据缓存
    private val metadataCache = LruCache<String, SmbFileInfo>(MAX_CACHE_SIZE)
    
    // 缩略图路径缓存
    private val thumbnailCache = object : LruCache<String, String>(MAX_THUMBNAIL_CACHE_SIZE) {
        override fun sizeOf(key: String, value: String): Int {
            // 估算缓存大小（实际应该根据图片文件大小计算）
            return 1024 * 1024 // 1MB per thumbnail
        }
    }
    
    // 正在处理的文件路径集合，避免重复处理
    private val processingFiles = mutableSetOf<String>()
    
    /**
     * 获取缓存的文件元数据
     */
    fun getFileMetadata(filePath: String): SmbFileInfo? {
        val result = metadataCache.get(filePath)
        try {
            if (::performanceMonitor.isInitialized) {
                if (result != null) {
                    performanceMonitor.recordCacheHit()
                } else {
                    performanceMonitor.recordCacheMiss()
                }
            }
        } catch (e: Exception) {
            // 忽略性能监控错误
        }
        return result
    }
    
    /**
     * 缓存文件元数据
     */
    fun cacheFileMetadata(filePath: String, fileInfo: SmbFileInfo) {
        metadataCache.put(filePath, fileInfo)
        Log.d(TAG, "Cached metadata for: $filePath")
    }
    
    /**
     * 获取缓存的缩略图路径
     */
    fun getThumbnailPath(filePath: String): String? {
        return thumbnailCache.get(filePath)
    }
    
    /**
     * 缓存缩略图路径
     */
    fun cacheThumbnailPath(filePath: String, thumbnailPath: String) {
        thumbnailCache.put(filePath, thumbnailPath)
        Log.d(TAG, "Cached thumbnail for: $filePath")
    }
    
    /**
     * 检查文件是否正在处理中
     */
    fun isProcessing(filePath: String): Boolean {
        synchronized(processingFiles) {
            return processingFiles.contains(filePath)
        }
    }
    
    /**
     * 标记文件开始处理
     */
    fun markProcessing(filePath: String): Boolean {
        synchronized(processingFiles) {
            return if (!processingFiles.contains(filePath)) {
                processingFiles.add(filePath)
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 标记文件处理完成
     */
    fun markProcessed(filePath: String) {
        synchronized(processingFiles) {
            processingFiles.remove(filePath)
        }
    }
    
    /**
     * 清除指定目录的缓存
     */
    fun clearDirectoryCache(directoryPath: String) {
        val keysToRemove = mutableListOf<String>()
        
        // 收集需要移除的键
        val snapshot = metadataCache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith(directoryPath)) {
                keysToRemove.add(key)
            }
        }
        
        // 移除缓存项
        keysToRemove.forEach { key ->
            metadataCache.remove(key)
        }
        
        // 清除对应的缩略图缓存
        val thumbnailSnapshot = thumbnailCache.snapshot()
        for (key in thumbnailSnapshot.keys) {
            if (key.startsWith(directoryPath)) {
                thumbnailCache.remove(key)
            }
        }
        
        Log.d(TAG, "Cleared cache for directory: $directoryPath (${keysToRemove.size} items)")
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAll() {
        metadataCache.evictAll()
        thumbnailCache.evictAll()
        synchronized(processingFiles) {
            processingFiles.clear()
        }
        Log.d(TAG, "Cleared all cache")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            metadataCacheSize = metadataCache.size(),
            metadataMaxSize = metadataCache.maxSize(),
            thumbnailCacheSize = thumbnailCache.size(),
            thumbnailMaxSize = thumbnailCache.maxSize(),
            processingCount = processingFiles.size
        )
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val metadataCacheSize: Int,
    val metadataMaxSize: Int,
    val thumbnailCacheSize: Int,
    val thumbnailMaxSize: Int,
    val processingCount: Int
)