package com.example.nasonly.data.smb

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.hierynomus.smbj.share.File as SmbFile
import java.lang.reflect.Method

@Singleton
class SmbConnectionManager @Inject constructor() : SmbManager {
    /**
     * 枚举顶层共享列表（Disk类型）
     */
    suspend fun listShares(host: String, creds: SmbCredentials): List<SmbShareInfo> = withContext(Dispatchers.IO) {
        probeCommonShares(host, creds)
    }
    fun getCurrentCredentials(): SmbCredentials {
        return SmbCredentials(
            domain = domain,
            username = username,
            password = password
        )
    }

    fun getCurrentHost(): String = host

    /**
     * 探测常见共享名作为 RPC 失败的回退策略
     */
    private suspend fun probeCommonShares(host: String, creds: SmbCredentials): List<SmbShareInfo> = withContext(Dispatchers.IO) {
        val client = SMBClient()
        var connection: Connection? = null
        var session: Session? = null
        val foundShares = mutableListOf<SmbShareInfo>()
        try {
            connection = client.connect(host)
            val auth = AuthenticationContext(creds.username ?: "", creds.password?.toCharArray() ?: charArrayOf(), creds.domain)
            session = connection.authenticate(auth)
            for (shareName in COMMON_SHARE_NAMES) {
                try {
                    val share = session.connectShare(shareName) as? DiskShare
                    if (share != null) {
                        // Try to list root to verify access
                                                val dir = share.openDirectory("\\", emptySet(), null, com.hierynomus.mssmb2.SMB2ShareAccess.ALL, null, emptySet())
                        dir.list()
                        dir.close()
                        foundShares.add(SmbShareInfo(shareName, "DISK", ""))
                        share.close()
                    }
                } catch (e: Exception) {
                    // Share not accessible or doesn't exist, skip
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to probe common shares: ${e.message}", e)
        } finally {
            try { session?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
        }
        foundShares
    }

    private val isConnected = AtomicBoolean(false)
    private var host: String = ""
    private var share: String = ""
    private var username: String = ""
    private var password: String = ""
    private var domain: String = ""

    // smbj 连接对象
    private var smbClient: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null

    suspend fun configure(host: String, share: String, username: String, password: String, domain: String = "") {
        withContext(Dispatchers.IO) {
            this@SmbConnectionManager.host = host
            this@SmbConnectionManager.share = share
            this@SmbConnectionManager.username = username
            this@SmbConnectionManager.password = password
            this@SmbConnectionManager.domain = domain
        }
    }

    // 生产环境不能在主线程访问网络，必须在IO线程中运行网络操作
    suspend fun connectAsync(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (host.isEmpty() || username.isEmpty()) {
                Log.w(TAG, "SMB configuration incomplete: host=$host, username=$username")
                return@withContext false
            }

            disconnectAsync() // 确保先断开之前的连接

            Log.d(TAG, "Attempting to connect to SMB server: $host using smbj")

            // 使用 smbj 建立真实的 SMB 连接
            smbClient = SMBClient()
            connection = smbClient?.connect(host)

            if (connection == null) {
                Log.e(TAG, "Failed to create SMB connection")
                return@withContext false
            }

            // 创建认证上下文
            val authContext = AuthenticationContext(username, password.toCharArray(), domain.ifEmpty { null })
            session = connection?.authenticate(authContext)

            if (session == null) {
                Log.e(TAG, "SMB authentication failed")
                return@withContext false
            }

            // 连接到共享
            if (share.isNotEmpty()) {
                diskShare = session?.connectShare(share) as? DiskShare
                if (diskShare == null) {
                    Log.w(TAG, "Failed to connect to share: $share, but SMB session is valid")
                }
            }

            isConnected.set(true)
            Log.i(TAG, "SMB connection established successfully with smbj")
            true
        } catch (e: SMBApiException) {
            Log.e(TAG, "SMB API error: ${e.message}", e)
            isConnected.set(false)
            false
        } catch (e: IOException) {
            Log.e(TAG, "SMB IO error: ${e.message}", e)
            isConnected.set(false)
            false
        } catch (e: Exception) {
            Log.e(TAG, "SMB connection failed: ${e.message}", e)
            isConnected.set(false)
            false
        }
    }

    override fun connect(): Boolean {
        // 为了保持原有API兼容性，保留同步方法但添加警告注释
        // 生产环境不应使用此方法，应使用connectAsync()
        Log.w(TAG, "Using deprecated synchronous connect method, should use connectAsync() instead")
        return try {
            if (host.isEmpty() || username.isEmpty()) {
                Log.w(TAG, "SMB configuration incomplete: host=$host, username=$username")
                return false
            }

            disconnect() // 确保先断开之前的连接

            Log.d(TAG, "Attempting to connect to SMB server: $host using smbj (sync)")

            // 使用 smbj 建立真实的 SMB 连接（同步版本）
            smbClient = SMBClient()
            connection = smbClient?.connect(host)

            if (connection == null) {
                Log.e(TAG, "Failed to create SMB connection")
                return false
            }

            // 创建认证上下文
            val authContext = AuthenticationContext(username, password.toCharArray(), domain.ifEmpty { null })
            session = connection?.authenticate(authContext)

            if (session == null) {
                Log.e(TAG, "SMB authentication failed")
                return false
            }

            // 连接到共享
            if (share.isNotEmpty()) {
                diskShare = session?.connectShare(share) as? DiskShare
                if (diskShare == null) {
                    Log.w(TAG, "Failed to connect to share: $share, but SMB session is valid")
                }
            }

            isConnected.set(true)
            Log.i(TAG, "SMB connection established successfully with smbj (sync)")
            true
        } catch (e: SMBApiException) {
            Log.e(TAG, "SMB API error: ${e.message}", e)
            isConnected.set(false)
            false
        } catch (e: IOException) {
            Log.e(TAG, "SMB IO error: ${e.message}", e)
            isConnected.set(false)
            false
        } catch (e: Exception) {
            Log.e(TAG, "SMB connection failed: ${e.message}", e)
            isConnected.set(false)
            false
        }
    }

    // 异步版本的断开连接方法，在IO线程中执行
    suspend fun disconnectAsync() = withContext(Dispatchers.IO) {
        try {
            diskShare?.close()
            session?.close()
            connection?.close()
            smbClient?.close()

            diskShare = null
            session = null
            connection = null
            smbClient = null
            isConnected.set(false)
            Log.d(TAG, "SMB connection disconnected (smbj)")
        } catch (e: Exception) {
            Log.w(TAG, "Error during disconnect: ${e.message}")
        }
    }

    override fun disconnect() {
        try {
            diskShare?.close()
            session?.close()
            connection?.close()
            smbClient?.close()

            diskShare = null
            session = null
            connection = null
            smbClient = null
            isConnected.set(false)
            Log.d(TAG, "SMB connection disconnected (smbj)")
        } catch (e: Exception) {
            Log.w(TAG, "Error during disconnect: ${e.message}")
        }
    }

    override suspend fun connect(host: String, share: String, username: String, password: String, domain: String): Boolean {
        configure(host, share, username, password, domain)
        return connectAsync()
    }

    override suspend fun connect(host: String, creds: SmbCredentials): Session = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to SMB server: $host")

            val client = SMBClient()
            val connection = client.connect(host)

            val authContext = AuthenticationContext(
                creds.username ?: "",
                (creds.password ?: "").toCharArray(),
                creds.domain
            )
            val session = connection.authenticate(authContext)

            // Store the connection for later use
            this@SmbConnectionManager.connection = connection
            this@SmbConnectionManager.session = session

            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to SMB server $host", e)
            throw e
        }
    }

    override fun isConnected(): Boolean = isConnected.get() && session != null

    override fun openInputStream(path: String): InputStream? {
        if (!isConnected()) {
            Log.w(TAG, "Attempting to open stream without connection")
            if (!reconnect()) {
                return null
            }
        }

        return try {
            Log.d(TAG, "Opening input stream for path: $path using smbj")

            val currentShare = diskShare
            if (currentShare == null) {
                Log.e(TAG, "No disk share available for path: $path")
                return null
            }

            // 使用 smbj 打开文件（只读）
            val smbFile: SmbFile = currentShare.openFile(
                path,
                setOf(AccessMask.GENERIC_READ),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
            )
            smbFile.inputStream
        } catch (e: SMBApiException) {
            Log.e(TAG, "SMB API error opening file $path: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open input stream for path: $path", e)
            null
        }
    }

    override fun openOutputStream(path: String): OutputStream? {
        if (!isConnected()) {
            Log.w(TAG, "Attempting to open output stream without connection")
            if (!reconnect()) {
                return null
            }
        }

        return try {
            Log.d(TAG, "Opening output stream for path: $path using smbj")

            val currentShare = diskShare
            if (currentShare == null) {
                Log.e(TAG, "No disk share available for path: $path")
                return null
            }

            // 使用 smbj 打开文件用于写入
            val smbFile: SmbFile = currentShare.openFile(
                path,
                setOf(AccessMask.GENERIC_WRITE),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_OPEN_IF,
                setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
            )
            smbFile.outputStream
        } catch (e: SMBApiException) {
            Log.e(TAG, "SMB API error opening file for write $path: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open output stream for path: $path", e)
            null
        }
    }

    override fun seek(inputStream: InputStream, position: Long): Boolean {
        return try {
            Log.d(TAG, "Seeking to position: $position")
            // smbj 的 InputStream 支持 skip 操作
            val skipped = inputStream.skip(position)
            skipped == position
        } catch (e: Exception) {
            Log.e(TAG, "Seek failed: ${e.message}", e)
            false
        }
    }

    private fun reconnect(): Boolean {
        Log.i(TAG, "Attempting to reconnect...")
        disconnect()
        return connect()
    }

    /**
     * 解析 SMB URL，提取相对于共享的路径
     * 例如：smb://192.168.1.1/share/folder -> folder
     * smb://192.168.1.1/share -> ""
     */
    private fun parseSmbPath(fullPath: String): String {
        if (fullPath.startsWith("smb://")) {
            try {
                val uri = java.net.URI(fullPath)
                val path = uri.path ?: return ""
                // 路径格式：/share/relativePath
                val firstSlash = path.indexOf('/')
                if (firstSlash == -1) return ""
                val secondSlash = path.indexOf('/', firstSlash + 1)
                return if (secondSlash == -1) "" else path.substring(secondSlash + 1)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse SMB path: $fullPath", e)
                return fullPath
            }
        }
        return fullPath
    }

    // 新增方法：列出目录内容
    suspend fun listDirectory(shareName: String, relativePath: String = ""): List<SmbFileInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                Log.w(TAG, "Attempting to list directory without connection")
                if (!connectAsync()) {
                    return@withContext emptyList()
                }
            }
            val currentShare = diskShare
            if (currentShare == null) {
                Log.e(TAG, "No disk share available for listing directory: $shareName/$relativePath")
                return@withContext emptyList()
            }
            Log.d(TAG, "Listing directory: $shareName/$relativePath using smbj")
            val files = mutableListOf<SmbFileInfo>()
            val dir = if (relativePath.isEmpty()) "" else relativePath
            val desiredAccess = EnumSet.of(AccessMask.GENERIC_READ)
            val createDisposition = EnumSet.of(SMB2CreateDisposition.FILE_OPEN)
            val createOptions = EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE)
            val shareAccess = SMB2ShareAccess.ALL
            val fileAttributes = EnumSet.noneOf(FileAttributes::class.java)
            val method = DiskShare::class.java.declaredMethods.find { it.name == "openDirectory" && it.parameterCount == 6 }
            val dirHandle = method?.invoke(currentShare, dir, createDisposition, createOptions, shareAccess, null, fileAttributes) as com.hierynomus.smbj.share.Directory
            dirHandle.list().forEach { fileInfo ->
                val fileName = fileInfo.fileName
                val fileAttributes = fileInfo.fileAttributes
                val isDirectory = (fileAttributes and 0x10L) != 0L
                val fileSize = if (isDirectory) 0L else fileInfo.endOfFile
                val fullPath = if (dir.isEmpty()) fileName else "$dir/$fileName"
                if (fileName != "." && fileName != "..") {
                    files.add(SmbFileInfo(fileName, fullPath, fileSize, isDirectory))
                }
            }
            Log.d(TAG, "Found ${files.size} files/directories in $shareName/$relativePath")
            files
        } catch (e: SMBApiException) {
            Log.e(TAG, "SMB API error listing directory $shareName/$relativePath: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list directory $shareName/$relativePath: ${e.message}", e)
            emptyList()
        }
    }
    /**
     * 统一错误码映射
     */
    fun mapError(e: Exception): SmbErrorType {
        return when (e) {
            is SMBApiException -> when (e.message) {
                "STATUS_LOGON_FAILURE" -> SmbErrorType.AUTH_FAILED
                "STATUS_ACCESS_DENIED" -> SmbErrorType.ACCESS_DENIED
                "STATUS_OBJECT_NAME_NOT_FOUND" -> SmbErrorType.NOT_FOUND
                else -> SmbErrorType.NETWORK
            }
            is IOException -> SmbErrorType.NETWORK
            else -> SmbErrorType.NETWORK
        }
    }

    // 获取当前的 DiskShare 实例（供其他类使用）
    fun getDiskShare(): DiskShare? = diskShare

    // 异步版本的连接验证，必须在协程中调用
    suspend fun validateConnectionAsync(): SmbConnectionResult = withContext(Dispatchers.IO) {
        when {
            host.isEmpty() -> SmbConnectionResult.Error("主机地址不能为空")
            username.isEmpty() -> SmbConnectionResult.Error("用户名不能为空")
            password.isEmpty() -> SmbConnectionResult.Error("密码不能为空")
            else -> {
                try {
                    if (connectAsync()) {
                        SmbConnectionResult.Success("连接成功")
                    } else {
                        SmbConnectionResult.Error("连接失败，请检查网络和服务器状态")
                    }
                } catch (e: Exception) {
                    SmbConnectionResult.Error("连接异常: ${e.message}")
                }
            }
        }
    }

    fun validateConnection(): SmbConnectionResult {
        // 保持原有API兼容性，但生产环境应使用validateConnectionAsync()
        return when {
            host.isEmpty() -> SmbConnectionResult.Error("主机地址不能为空")
            username.isEmpty() -> SmbConnectionResult.Error("用户名不能为空")
            password.isEmpty() -> SmbConnectionResult.Error("密码不能为空")
            else -> {
                try {
                    if (connect()) {
                        SmbConnectionResult.Success("连接成功")
                    } else {
                        SmbConnectionResult.Error("连接失败，请检查网络和服务器状态")
                    }
                } catch (e: Exception) {
                    SmbConnectionResult.Error("连接异常: ${e.message}")
                }
            }
        }
    }

    override fun close() {
        disconnect()
    }

    companion object {
        private const val TAG = "SmbConnectionManager"
        private const val DEFAULT_TIMEOUT = 60000
        private const val SMB_PORT = 445
        private val COMMON_SHARE_NAMES = listOf("public", "share", "media", "backup", "data", "documents", "music", "video", "photos", "shared", "nas", "storage")
    }
}

sealed class SmbConnectionResult {
enum class SmbErrorType {
    AUTH_FAILED, ACCESS_DENIED, NOT_FOUND, NETWORK
}
    data class Success(val message: String) : SmbConnectionResult()
    data class Error(val message: String) : SmbConnectionResult()
}
