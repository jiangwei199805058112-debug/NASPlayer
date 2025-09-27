package com.example.nasonly.data.remote.smb

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

data class SmbHost(val host: String)
data class ShareName(val value: String)

class SmbShareEnumerator {
    private fun ctx(user: String?, pass: String?): CIFSContext {
        val props = Properties().apply {
            put("jcifs.smb.client.minVersion", "SMB202")
            put("jcifs.smb.client.maxVersion", "SMB311")
            put("jcifs.smb.client.useUnicode", "true")
            put("jcifs.smb.client.signingPreferred", "true")
        }
        val base = BaseContext(PropertyConfiguration(props))
        val auth = NtlmPasswordAuthenticator("", user ?: "", pass ?: "")
        return base.withCredentials(auth)
    }

    /** 列出主机上的共享名；失败则尝试匿名/guest 回退 */
    suspend fun listShares(
        host: String,
        user: String? = null,
        pass: String? = null
    ): List<ShareName> = withContext(Dispatchers.IO) {
        val tries = listOf(
            ctx(user, pass),                 // 指定账号
            ctx(null, null),                 // 匿名（部分“公共=开”的 NAS）
            ctx("guest", "")                 // guest
        )
        var lastErr: Exception? = null
        for (c in tries) {
            try {
                val root = SmbFile("smb://$host/", c)
                if (!root.exists()) continue
                val arr = root.listFiles() ?: emptyArray()
                // 仅保留目录型共享；去掉隐藏的 $ 共享（根据需要可去掉过滤）
                return@withContext arr
                    .filter { it.isDirectory }
                    .map { it.name.removeSuffix("/") }
                    .filter { it.isNotBlank() && !it.endsWith("$") }
                    .distinct()
                    .map { ShareName(it) }
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: IllegalStateException("列出共享失败")
    }
}