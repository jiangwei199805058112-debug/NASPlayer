package com.example.nasonly.data.remote.smb

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.net.URI
import java.util.Properties

data class RemoteVideo(
    val name: String,
    val smbUrl: String,
    val size: Long,
    val lastModified: Long
)

data class SmbCredentials(
    val domain: String? = null,
    val username: String? = null,
    val password: String? = null
)

class SmbScanner(
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val videoExt = setOf(
        "mp4","mkv","avi","mov","flv","wmv","ts","m2ts","webm"
    )

    suspend fun scanVideos(
        rootPath: String,                      // UNC or smb:// url
        creds: SmbCredentials = SmbCredentials(),
        onFound: (RemoteVideo) -> Unit = {}
    ): List<RemoteVideo> = withContext(io) {
        val ctx = buildContext(creds)
        val rootUrl = toSmbUrl(rootPath)
        val root = SmbFile(rootUrl, ctx)

        val out = mutableListOf<RemoteVideo>()
        if (!(root.exists() && root.isDirectory)) return@withContext out

        val queue = ArrayDeque<SmbFile>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            coroutineContext.ensureActive()
            val dir = queue.removeFirst()
            val children = try {
                dir.listFiles()
            } catch (e: SmbException) {
                emptyArray()
            }

            for (child in children) {
                if (child.isDirectory) {
                    queue.add(child)
                } else {
                    val name = child.name.removeSuffix("/")
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in videoExt) {
                        val size = runCatching { child.length() }.getOrElse { -1L }
                        val lm   = runCatching { child.lastModified() }.getOrElse { 0L }
                        val url  = child.canonicalPath // smb://host/share/dir/file.ext
                        val item = RemoteVideo(name = name, smbUrl = url, size = size, lastModified = lm)
                        out.add(item)
                        onFound(item)
                    }
                }
            }
        }
        out
    }

    // --- Helpers ---

    private fun buildContext(creds: SmbCredentials): CIFSContext {
        val props = Properties().apply {
            // Prefer SMB2+; 2.1.x supports min/max
            put("jcifs.smb.client.minVersion", "SMB202")
            put("jcifs.smb.client.maxVersion", "SMB311")
            put("jcifs.smb.client.signingPreferred", "true")
            put("jcifs.smb.client.disablePlainTextPasswords", "true")
            put("jcifs.smb.client.useUnicode", "true")
            put("jcifs.smb.client.responseTimeout", "30000")
            put("jcifs.smb.client.soTimeout", "30000")
        }
        val base = BaseContext(PropertyConfiguration(props))
        val auth = NtlmPasswordAuthenticator(
            creds.domain ?: "",
            creds.username ?: "",
            creds.password ?: ""
        )
        return base.withCredentials(auth)
    }

    /**
     * Accepts either:
     *  - Windows UNC: \\host\share\dir\子目录
     *  - Already smb://host/share/dir/子目录
     * Returns normalized smb://.../ (trailing slash for dirs).
     */
    private fun toSmbUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("smb://", ignoreCase = true)) {
            return ensureDirSlash(trimmed)
        }
        // UNC -> smb
        val withoutLeading = trimmed
            .removePrefix("\\\\")
            .removePrefix("//")
        val path = withoutLeading.replace("\\", "/")

        // Build via URI to avoid double-encoding and keep Unicode
        // Split host and path
        val firstSlash = path.indexOf('/')
        val host = if (firstSlash == -1) path else path.substring(0, firstSlash)
        val rest = if (firstSlash == -1) "/" else path.substring(firstSlash).let { if (it.isEmpty()) "/" else it }
        val uri = URI("smb", host, rest, null)
        return ensureDirSlash(uri.toString())
    }

    private fun ensureDirSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}