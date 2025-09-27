package com.example.nasonly.data.remote.smb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class SmbHostScanner(
    private val shareEnumerator: SmbShareEnumerator = SmbShareEnumerator(),
    private val fileScanner: SmbScanner = SmbScanner(), // 复用你之前的 jcifs-ng 版 SmbScanner（递归 + 过滤视频）
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * 扫描整台主机上的所有共享；每发现一个共享或视频都会回调
     * @param onShare 发现共享时回调，如 "Public"、"备份文件" 等
     */
    suspend fun scanAllShares(
        host: String,
        user: String? = null,
        pass: String? = null,
        onShare: (String) -> Unit = {},
        onFound: (String, RemoteVideo) -> Unit = { _, _ -> }
    ): List<RemoteVideo> = withContext(io) {
        val shares = runCatching {
            shareEnumerator.listShares(host, user, pass)
        }.getOrElse {
            // 枚举失败时的兜底（按你 NAS 常见共享名做种子）；可增删
            listOf("Public", "Transmission", "TimeMachineBackup", "备份文件").map { ShareName(it) }
        }

        val dispatcher = Dispatchers.IO.limitedParallelism(4) // 限制并发，避免把 NAS 撑满
        coroutineScope {
            shares.map { s ->
                async(dispatcher) {
                    val share = s.value
                    onShare(share)
                    val root = "smb://$host/$share/." // 根目录一律用 "."
                    runCatching {
                        fileScanner.scanVideos(
                            rootPath = root,
                            creds = SmbCredentials(username = user, password = pass)
                        ) { v -> onFound(share, v) }
                    }.getOrElse { emptyList() }
                }
            }.awaitAll().flatten()
        }
    }
}