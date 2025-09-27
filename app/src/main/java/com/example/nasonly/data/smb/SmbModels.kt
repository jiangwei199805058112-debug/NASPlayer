package com.example.nasonly.data.smb

import com.example.nasonly.data.media.VideoMetadata
import java.util.Date

/**
 * SMB 文件信息数据类
 */
data class SmbFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean
)

/**
 * SMB 文件详细信息
 */
data class SmbFile(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Date,
    val fullPath: String
)

/**
 * 包含元数据的 SMB 文件信息
 */
data class SmbFileWithMetadata(
    val file: SmbFile,
    val metadata: VideoMetadata? = null
)

/**
 * SMB 共享信息数据类
 */
data class SmbShareInfo(
    val name: String,
    val type: String = "DISK",
    val comment: String = ""
)

/**
 * SMB 凭据数据类
 */
data class SmbCredentials(
    val domain: String? = null,
    val username: String? = null,
    val password: String? = null
)

/**
 * SMB 错误类型枚举
 */
enum class SmbErrorType {
    AUTH_FAILED,
    ACCESS_DENIED,
    NOT_FOUND,
    NETWORK,
    UNKNOWN
}