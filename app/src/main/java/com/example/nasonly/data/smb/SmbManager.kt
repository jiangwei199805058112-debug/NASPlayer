package com.example.nasonly.data.smb

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

interface SmbManager : Closeable {
    fun connect(): Boolean
    suspend fun connect(host: String, share: String, username: String, password: String, domain: String = ""): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun openInputStream(path: String): InputStream?
    fun openOutputStream(path: String): OutputStream?
    fun seek(inputStream: InputStream, position: Long): Boolean
    override fun close() {
        disconnect()
    }
}
