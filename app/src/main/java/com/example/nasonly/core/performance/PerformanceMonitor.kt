package com.example.nasonly.core.performance

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控管理器
 * 跟踪应用关键性能指标，如文件加载时间、缓存命中率等
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
    }
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // 正在进行的操作追踪
    private val ongoingOperations = mutableMapOf<String, Long>()
    
    /**
     * 开始性能计时
     */
    fun startOperation(operationName: String): String {
        val operationId = "${operationName}_${System.currentTimeMillis()}"
        ongoingOperations[operationId] = SystemClock.elapsedRealtime()
        Log.d(TAG, "Started operation: $operationName")
        return operationId
    }
    
    /**
     * 结束性能计时并记录结果
     */
    fun endOperation(operationId: String, operationName: String) {
        val startTime = ongoingOperations.remove(operationId)
        if (startTime != null) {
            val duration = SystemClock.elapsedRealtime() - startTime
            recordOperationTime(operationName, duration)
            Log.d(TAG, "Completed operation: $operationName in ${duration}ms")
        }
    }
    
    /**
     * 记录文件加载时间
     */
    fun recordFileLoadTime(duration: Long) {
        val current = _performanceMetrics.value
        _performanceMetrics.value = current.copy(
            totalFileLoads = current.totalFileLoads + 1,
            averageFileLoadTime = updateAverage(
                current.averageFileLoadTime,
                duration,
                current.totalFileLoads + 1
            ),
            lastFileLoadTime = duration
        )
    }
    
    /**
     * 记录缓存命中
     */
    fun recordCacheHit() {
        val current = _performanceMetrics.value
        _performanceMetrics.value = current.copy(
            cacheHits = current.cacheHits + 1,
            cacheHitRate = calculateCacheHitRate(
                current.cacheHits + 1,
                current.cacheMisses
            )
        )
    }
    
    /**
     * 记录缓存未命中
     */
    fun recordCacheMiss() {
        val current = _performanceMetrics.value
        _performanceMetrics.value = current.copy(
            cacheMisses = current.cacheMisses + 1,
            cacheHitRate = calculateCacheHitRate(
                current.cacheHits,
                current.cacheMisses + 1
            )
        )
    }
    
    /**
     * 记录元数据提取时间
     */
    fun recordMetadataExtractionTime(duration: Long) {
        val current = _performanceMetrics.value
        _performanceMetrics.value = current.copy(
            totalMetadataExtractions = current.totalMetadataExtractions + 1,
            averageMetadataExtractionTime = updateAverage(
                current.averageMetadataExtractionTime,
                duration,
                current.totalMetadataExtractions + 1
            )
        )
    }
    
    /**
     * 记录缩略图生成时间
     */
    fun recordThumbnailGenerationTime(duration: Long) {
        val current = _performanceMetrics.value
        _performanceMetrics.value = current.copy(
            totalThumbnailGenerations = current.totalThumbnailGenerations + 1,
            averageThumbnailGenerationTime = updateAverage(
                current.averageThumbnailGenerationTime,
                duration,
                current.totalThumbnailGenerations + 1
            )
        )
    }
    
    /**
     * 记录当前加载的文件数量
     */
    fun updateLoadedFilesCount(count: Int) {
        _performanceMetrics.value = _performanceMetrics.value.copy(
            currentLoadedFiles = count
        )
    }
    
    /**
     * 记录内存使用情况
     */
    fun updateMemoryUsage(usedMemory: Long, maxMemory: Long) {
        _performanceMetrics.value = _performanceMetrics.value.copy(
            memoryUsage = (usedMemory.toDouble() / maxMemory * 100).toFloat()
        )
    }
    
    /**
     * 重置性能指标
     */
    fun resetMetrics() {
        _performanceMetrics.value = PerformanceMetrics()
        ongoingOperations.clear()
        Log.d(TAG, "Performance metrics reset")
    }
    
    /**
     * 获取性能报告
     */
    fun getPerformanceReport(): String {
        val metrics = _performanceMetrics.value
        return buildString {
            appendLine("=== 性能报告 ===")
            appendLine("文件加载:")
            appendLine("  总次数: ${metrics.totalFileLoads}")
            appendLine("  平均时间: ${metrics.averageFileLoadTime}ms")
            appendLine("  最近一次: ${metrics.lastFileLoadTime}ms")
            appendLine()
            appendLine("缓存性能:")
            appendLine("  命中率: ${String.format("%.1f", metrics.cacheHitRate)}%")
            appendLine("  命中次数: ${metrics.cacheHits}")
            appendLine("  未命中次数: ${metrics.cacheMisses}")
            appendLine()
            appendLine("元数据提取:")
            appendLine("  总次数: ${metrics.totalMetadataExtractions}")
            appendLine("  平均时间: ${metrics.averageMetadataExtractionTime}ms")
            appendLine()
            appendLine("缩略图生成:")
            appendLine("  总次数: ${metrics.totalThumbnailGenerations}")
            appendLine("  平均时间: ${metrics.averageThumbnailGenerationTime}ms")
            appendLine()
            appendLine("当前状态:")
            appendLine("  已加载文件: ${metrics.currentLoadedFiles}")
            appendLine("  内存使用率: ${String.format("%.1f", metrics.memoryUsage)}%")
        }
    }
    
    private fun recordOperationTime(operationName: String, duration: Long) {
        when (operationName) {
            "file_load" -> recordFileLoadTime(duration)
            "metadata_extraction" -> recordMetadataExtractionTime(duration)
            "thumbnail_generation" -> recordThumbnailGenerationTime(duration)
        }
    }
    
    private fun updateAverage(currentAverage: Long, newValue: Long, count: Int): Long {
        return if (count == 1) {
            newValue
        } else {
            ((currentAverage * (count - 1)) + newValue) / count
        }
    }
    
    private fun calculateCacheHitRate(hits: Int, misses: Int): Float {
        val total = hits + misses
        return if (total == 0) 0f else (hits.toFloat() / total * 100)
    }
}

/**
 * 性能指标数据类
 */
data class PerformanceMetrics(
    val totalFileLoads: Int = 0,
    val averageFileLoadTime: Long = 0L,
    val lastFileLoadTime: Long = 0L,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    val cacheHitRate: Float = 0f,
    val totalMetadataExtractions: Int = 0,
    val averageMetadataExtractionTime: Long = 0L,
    val totalThumbnailGenerations: Int = 0,
    val averageThumbnailGenerationTime: Long = 0L,
    val currentLoadedFiles: Int = 0,
    val memoryUsage: Float = 0f
)