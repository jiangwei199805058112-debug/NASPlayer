package com.example.nasonly.data.smb

import java.io.InputStream
import java.io.OutputStream
import java.io.Closeable

class SmbConnectionManager : SmbManager {
    private var connected = false

    override fun connect(): Boolean {
        // TODO: 实现 SMB 连接逻辑
        connected = true
        return connected
    }

    override fun disconnect() {
        // TODO: 断开 SMB 连接
        connected = false
    }

    override fun isConnected(): Boolean = connected

    override fun openInputStream(path: String): InputStream? {
        // TODO: 打开 SMB 输入流
        return null
    }

    override fun openOutputStream(path: String): OutputStream? {
        // TODO: 打开 SMB 输出流
        return null
    }

    override fun seek(inputStream: InputStream, position: Long): Boolean {
        // TODO: 支持 seek 操作
        return false
    }

    override fun close() {
        disconnect()
    }
}
