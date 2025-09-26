-keep,allowobfuscation,allowshrinking class androidx.benchmark.** { *; }

# Keep benchmark classes
-keep class com.example.nasonly.benchmark.** { *; }

# Keep performance monitoring classes for benchmarking
-keep class com.example.nasonly.monitoring.** { *; }

# Keep media-related classes that might be benchmarked
-keep class com.example.nasonly.data.model.** { *; }

# Preserve method names for benchmark profiling
-keepnames class ** {
    @androidx.benchmark.* <methods>;
}

# Keep ExoPlayer classes for media benchmarks
-keep class com.google.android.exoplayer2.** { *; }

# Keep SMB-related classes for network benchmarks
-keep class com.hierynomus.smbj.** { *; }