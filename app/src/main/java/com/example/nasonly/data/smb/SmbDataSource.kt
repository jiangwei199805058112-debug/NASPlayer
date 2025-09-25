package com.example.nasonly.data.smb

import java.io.InputStream

class SmbDataSource(private val smbManager: SmbManager) {
    fun getInputStream(path: String): InputStream? {
        if (!smbManager.isConnected()) {
            smbManager.connect()
        }
        return smbManager.openInputStream(path)
    }

    fun seekStream(inputStream: InputStream, position: Long): Boolean {
        return smbManager.seek(inputStream, position)
    }

    fun close() {
        smbManager.close()
    }
}
