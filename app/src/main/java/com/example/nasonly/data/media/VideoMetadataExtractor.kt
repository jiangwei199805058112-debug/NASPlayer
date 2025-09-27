package com.example.nasonly.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频元数据提取器
 * 用于提取视频文件的详细信息，包括时长、分辨率、比特率等
 */
@Singleton
class VideoMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * 提取视频元数据
     */
    suspend fun extractMetadata(videoPath: String): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 0f

            // 音频信息
            val audioChannels = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
            val audioSampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.toIntOrNull() ?: 0

            // 编解码器信息
            val videoCodec = getVideoCodec(videoPath)

            VideoMetadata(
                duration = duration,
                width = width,
                height = height,
                frameRate = frameRate,
                bitrate = bitrate,
                codecName = videoCodec,
                audioChannels = audioChannels,
                audioSampleRate = audioSampleRate,
            )
        } catch (e: Exception) {
            VideoMetadata()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
        }
    }

    /**
     * 生成视频缩略图
     */
    suspend fun generateThumbnail(videoPath: String, timeUs: Long = 1000000L): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)

            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                val thumbnailFile = File(context.cacheDir, "thumbnails")
                if (!thumbnailFile.exists()) {
                    thumbnailFile.mkdirs()
                }

                val fileName = "${videoPath.hashCode()}.jpg"
                val file = File(thumbnailFile, fileName)

                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }

                bitmap.recycle()
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
        }
    }

    /**
     * 批量生成缩略图
     */
    suspend fun generateThumbnails(videoPaths: List<String>): Map<String, String?> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, String?>()

        videoPaths.forEach { videoPath ->
            results[videoPath] = generateThumbnail(videoPath)
        }

        results
    }

    /**
     * 清理缓存的缩略图
     */
    suspend fun clearThumbnailCache() = withContext(Dispatchers.IO) {
        try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (thumbnailDir.exists()) {
                thumbnailDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 忽略清理异常
        }
    }

    /**
     * 获取视频编解码器信息
     */
    private fun getVideoCodec(videoPath: String): String? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(videoPath)

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime?.startsWith("video/") == true) {
                    return when (mime) {
                        "video/avc" -> "H.264"
                        "video/hevc" -> "H.265"
                        "video/mp4v-es" -> "MPEG-4"
                        "video/3gpp" -> "H.263"
                        "video/x-vnd.on2.vp8" -> "VP8"
                        "video/x-vnd.on2.vp9" -> "VP9"
                        "video/av01" -> "AV1"
                        else -> mime.substringAfter("video/").uppercase()
                    }
                }
            }

            return null
        } catch (e: Exception) {
            return null
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
        }
    }
}

/**
 * 视频元数据数据类
 */
data class VideoMetadata(
    val duration: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val bitrate: Long = 0L,
    val codecName: String? = null,
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0,
)
