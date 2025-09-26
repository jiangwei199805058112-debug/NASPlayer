package com.example.nasonly.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nasonly.monitoring.PerformanceMonitor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 性能基准测试
 * 测试关键组件的性能表现
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * 测试性能监控器的开销
     */
    @Test
    fun benchmarkPerformanceMonitorOverhead() {
        val monitor = PerformanceMonitor()
        
        benchmarkRule.measureRepeated {
            monitor.recordOperation("test_operation", 100L)
        }
    }

    /**
     * 测试缓存性能
     */
    @Test
    fun benchmarkCacheOperations() {
        // 这里可以添加具体的缓存性能测试
        benchmarkRule.measureRepeated {
            // 模拟缓存操作
            val data = "test_data"
            val processed = data.hashCode()
        }
    }

    /**
     * 测试文件列表处理性能
     */
    @Test
    fun benchmarkFileListProcessing() {
        val testFiles = (1..1000).map { "file_$it.mp4" }
        
        benchmarkRule.measureRepeated {
            val filtered = testFiles.filter { it.endsWith(".mp4") }
            val sorted = filtered.sorted()
            sorted.size // 确保操作被执行
        }
    }

    /**
     * 测试元数据提取性能
     */
    @Test
    fun benchmarkMetadataExtraction() {
        val testMetadata = mapOf(
            "duration" to "120000",
            "resolution" to "1920x1080",
            "bitrate" to "5000000"
        )
        
        benchmarkRule.measureRepeated {
            val processed = testMetadata.entries.associate { 
                it.key to it.value.toString() 
            }
            processed.size
        }
    }
}