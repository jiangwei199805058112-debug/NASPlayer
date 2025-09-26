package com.example.nasonly.data.smb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.share.File as SmbFile
import java.io.IOException

@Singleton
class SmbConnectionManager @Inject constructor() : SmbManager {
    companion object {
        private const val TAG = "SmbConnectionManager"
        private const val DEFAULT_TIMEOUT = 60000
        private const val SMB_PORT = 445
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
            
            // 使用 smbj 打开文件
            val smbFile: SmbFile = currentShare.openFile(
                path,
                setOf(com.hierynomus.mssmb2.SMB2ShareAccess.FILE_SHARE_READ),
                setOf(com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN),
                setOf(com.hierynomus.mssmb2.SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
                setOf(com.hierynomus.mssmb2.SMB2ShareAccess.FILE_SHARE_READ),
                null
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
                setOf(com.hierynomus.mssmb2.SMB2ShareAccess.FILE_SHARE_WRITE),
                setOf(com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OVERWRITE_IF),
                setOf(com.hierynomus.mssmb2.SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
                setOf(com.hierynomus.mssmb2.SMB2ShareAccess.FILE_SHARE_WRITE),
                null
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

    // 新增方法：列出目录内容
    suspend fun listDirectory(path: String = ""): List<SmbFileInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                Log.w(TAG, "Attempting to list directory without connection")
                if (!connectAsync()) {
                    return@withContext emptyList()
                }
            }

            val currentShare = diskShare
            if (currentShare == null) {
                Log.e(TAG, "No disk share available for listing directory: $path")
                return@withContext emptyList()
            }

            Log.d(TAG, "Listing directory: $path using smbj")
            
            val files = mutableListOf<SmbFileInfo>()
            val directoryPath = if (path.isEmpty()) "*" else "$path/*"
            
            currentShare.list(directoryPath).forEach { fileInfo ->
                val fileName = fileInfo.fileName
                val fileAttributes = fileInfo.fileAttributes
                val isDirectory = fileAttributes and 0x10L != 0L // FILE_ATTRIBUTE_DIRECTORY
                val fileSize = if (isDirectory) 0L else fileInfo.endOfFile
                val fullPath = if (path.isEmpty()) fileName else "$path/$fileName"
                
                // 跳过 . 和 .. 目录
                if (fileName != "." && fileName != "..") {
                    files.add(SmbFileInfo(fileName, fullPath, fileSize, isDirectory))
                }
            }
            
            Log.d(TAG, "Found ${files.size} files/directories in $path")
            files
        } catch (e: SMBApiException) {
            Log.e(TAG, "SMB API error listing directory $path: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list directory $path: ${e.message}", e)
            emptyList()
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
}

sealed class SmbConnectionResult {
    data class Success(val message: String) : SmbConnectionResult()
    data class Error(val message: String) : SmbConnectionResult()
}
