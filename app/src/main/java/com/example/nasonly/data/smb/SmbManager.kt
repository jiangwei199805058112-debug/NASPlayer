package com.example.nasonly.data.smb

import java.io.InputStream
import java.io.OutputStream
import java.io.Closeable

interface SmbManager : Closeable {
    fun connect(): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun openInputStream(path: String): InputStream?
    fun openOutputStream(path: String): OutputStream?
    fun seek(inputStream: InputStream, position: Long): Boolean
    override fun close() {
        disconnect()
    }
}
