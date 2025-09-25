package com.example.nasonly.repository

import com.example.nasonly.data.smb.SmbDataSource
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbRepository @Inject constructor(
    private val smbDataSource: SmbDataSource
) {
    fun getInputStream(path: String): InputStream? = smbDataSource.getInputStream(path)
    fun seekStream(inputStream: InputStream, position: Long): Boolean = smbDataSource.seekStream(inputStream, position)
    fun close() = smbDataSource.close()
}
