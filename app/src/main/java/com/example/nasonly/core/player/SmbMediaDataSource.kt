package com.example.nasonly.core.player

import android.net.Uri
import com.example.nasonly.data.smb.SmbDataSource
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
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
        inputStream = smbDataSource.getInputStream(path)
        if (inputStream == null) throw IOException("Failed to open SMB stream")
        if (dataSpec.position > 0) {
            if (!smbDataSource.seekStream(inputStream!!, dataSpec.position)) {
                throw IOException("Seek failed")
            }
        }
        bytesRead = 0
        return C.LENGTH_UNSET
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return try {
            val stream = inputStream ?: throw IOException("Stream not open")
            val read = stream.read(buffer, offset, readLength)
            if (read > 0) bytesRead += read
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

    class Factory(private val smbDataSource: SmbDataSource) : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbMediaDataSource(smbDataSource)
    }
}
