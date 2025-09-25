package com.example.nasonly.core.player

import android.net.Uri
import com.example.nasonly.data.smb.SmbDataSource
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream

class SmbMediaDataSource(
    private val smbDataSource: SmbDataSource
) : DataSource {
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRead: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val path = uri?.path ?: throw IOException("Invalid SMB path")
        runBlocking {
            val result = smbDataSource.getInputStream(path)
            inputStream = result.getOrNull() ?: throw IOException("Failed to open SMB stream: ${result.exceptionOrNull()?.message}")
        }
        if (dataSpec.position > 0) {
            val seekResult = runBlocking {
                smbDataSource.seekStream(inputStream!!, dataSpec.position)
            }
            if (seekResult.getOrNull() != true) {
                throw IOException("Seek failed: ${seekResult.exceptionOrNull()?.message}")
            }
        }
        bytesRead = 0
        return C.LENGTH_UNSET
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return try {
            val stream = inputStream ?: throw IOException("Stream not open")
            val read = stream.read(buffer, offset, readLength)
            if (read > 0) bytesRead += read.toLong()
            read
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        inputStream = null
        uri = null
        bytesRead = 0
        smbDataSource.close()
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // Transfer listener functionality can be added here if needed
        // For now, we'll provide an empty implementation
    }

    class Factory(private val smbDataSource: SmbDataSource) : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbMediaDataSource(smbDataSource)
    }
}
