package com.example.nasonly.core.player

import android.net.Uri
import com.example.nasonly.data.smb.SmbDataSource
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
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
        
        // TODO: CRITICAL - runBlocking should not be used in DataSource.open()
        // This blocks the calling thread and violates ExoPlayer's contract.
        // Consider pre-opening streams or using async initialization.
        try {
            val result = runBlocking {
                smbDataSource.getInputStream(path)
            }
            inputStream = result.getOrNull() 
                ?: throw IOException("Failed to open SMB stream: ${result.exceptionOrNull()?.message}")
                
            if (dataSpec.position > 0) {
                val seekResult = runBlocking {
                    inputStream?.let { stream ->
                        smbDataSource.seekStream(stream, dataSpec.position)
                    } ?: Result.failure(IOException("Stream is null"))
                }
                if (seekResult.getOrNull() != true) {
                    throw IOException("Seek failed: ${seekResult.exceptionOrNull()?.message}")
                }
            }
            bytesRead = 0
            return C.LENGTH_UNSET.toLong()
        } catch (e: Exception) {
            close() // Ensure cleanup on failure
            throw IOException("Failed to open SMB data source", e)
        }
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
        } catch (e: Exception) {
            // Log but don't throw - close() should be idempotent
            android.util.Log.w("SmbMediaDataSource", "Error closing input stream", e)
        } finally {
            inputStream = null
            uri = null
            bytesRead = 0
        }
        
        try {
            smbDataSource.close()
        } catch (e: Exception) {
            android.util.Log.w("SmbMediaDataSource", "Error closing SMB data source", e)
        }
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // Transfer listener functionality can be added here if needed
        // For now, we'll provide an empty implementation
    }

    class Factory(private val smbDataSource: SmbDataSource) : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbMediaDataSource(smbDataSource)
    }
}
