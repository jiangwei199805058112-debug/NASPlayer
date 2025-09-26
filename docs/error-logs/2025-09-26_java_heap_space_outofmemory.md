# 构建错误报告 - Java Heap Space OutOfMemoryError

## 📋 错误概览

- **报错时间**: 2025-09-26
- **报错类型**: 构建失败 (Java OutOfMemoryError)
- **严重程度**: 高 (阻止构建完成)
- **影响范围**: 整个项目构建流程

## 🔧 环境信息

- **操作系统**: Windows
- **Gradle版本**: 8.6
- **Android Gradle Plugin**: 8.2.2
- **JDK版本**: Java 17
- **Android SDK**: API 34
- **Kotlin版本**: 1.9.22

## ⚠️ 报错摘要

```
java.lang.OutOfMemoryError: Java heap space
Error while merging dex archives
Task ':app:mergeExtDexDebug' FAILED
```

## 📝 完整错误堆栈

```
> Task :app:mergeExtDexDebug FAILED
AGPBI: {"kind":"error","text":"java.lang.OutOfMemoryError: Java heap space","sources":[{}],"tool":"D8"}
com.android.builder.dexing.DexArchiveMergerException: Error while merging dex archives: 
	at com.android.builder.dexing.D8DexArchiveMerger.getMergingExceptionToRethrow(D8DexArchiveMerger.java:159)
	at com.android.builder.dexing.D8DexArchiveMerger.mergeDexArchives(D8DexArchiveMerger.java:147)
	at com.android.build.gradle.internal.tasks.DexMergingWorkAction.merge(DexMergingTask.kt:891)
	...
Caused by: java.lang.OutOfMemoryError: Java heap space
	at java.base/java.nio.HeapByteBuffer.<init>(Unknown Source)
	at java.base/java.nio.ByteBuffer.allocate(Unknown Source)
	at com.android.tools.r8.ByteBufferProvider.acquireByteBuffer(R8_8.2.47_115170b0e238ab4c8fd3abe4aa31d20c98f8a77f61775e861794cc2d75fbdf13:1)
	...
```

## 🔍 错误分析

### 根本原因
1. **内存不足**: Gradle JVM 堆内存配置过小 (2GB)，无法处理大量依赖库的 dex 合并
2. **AndroidManifest 配置**: 使用了已废弃的 `package` 属性
3. **构建优化不足**: 缺乏内存优化配置

### 触发条件
- 项目依赖库较多 (Hilt, Room, ExoPlayer, Compose BOM 等)
- Dex 合并过程需要大量内存分配
- 并行构建任务增加内存压力

### 影响范围
- 所有构建任务无法完成
- APK 生成失败
- CI/CD 流水线中断

## 🛠️ 修复方案

### 1. 内存配置优化
**文件**: `gradle.properties`
```properties
# 增加 JVM 堆内存到 4GB
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8

# 启用构建优化
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
android.enableR8.fullMode=true
```

### 2. AndroidManifest 修复
**文件**: `app/src/main/AndroidManifest.xml`
```xml
<!-- 移除废弃的 package 属性 -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
```

### 3. 构建配置优化
**文件**: `app/build.gradle.kts`
```kotlin
android {
    // 配置打包选项
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}
```

## ✅ 修复验证

### 验证步骤
1. 清理项目缓存: `./gradlew clean`
2. 重新构建: `./gradlew assembleDebug`
3. 运行测试: `./gradlew test`

### 预期结果
- 构建成功完成，无 OutOfMemoryError
- APK 正常生成
- 所有警告信息清除

## 📚 相关资源

- [Android Gradle Plugin 内存配置](https://developer.android.com/studio/build/optimize-your-build#memory)
- [Gradle 性能优化指南](https://docs.gradle.org/current/userguide/performance.html)
- [R8 代码压缩配置](https://developer.android.com/studio/build/shrink-code)

## 🔄 后续预防措施

1. **监控构建内存使用**: 定期检查构建日志中的内存使用情况
2. **依赖管理**: 定期清理不必要的依赖库
3. **构建优化**: 启用更多 Gradle 构建优化选项
4. **CI/CD 配置**: 确保 CI 环境有足够的内存分配

---
**状态**: ✅ 已修复  
**修复提交**: `d972009` & `35c5343`  
**验证时间**: 2025-09-26