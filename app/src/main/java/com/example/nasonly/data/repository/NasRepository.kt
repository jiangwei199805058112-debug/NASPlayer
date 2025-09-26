package com.example.nasonly.data.repository

import com.example.nasonly.data.discovery.DeviceInfo
import com.example.nasonly.data.discovery.NasDiscoveryManager
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NAS Repository - 整合SMB连接和设备发现功能
 */
@Singleton
class NasRepository @Inject constructor(
    private val smbConnectionManager: SmbConnectionManager,
    private val smbDataSource: SmbDataSource,
    private val nasDiscoveryManager: NasDiscoveryManager
) {
    
    /**
     * 发现网络中的NAS设备
     */
    fun discoverNasDevices(): Flow<List<DeviceInfo>> {
        return nasDiscoveryManager.startDiscovery()
    }
    
    /**
     * 连接到指定的NAS设备
     */
    suspend fun connectToNas(deviceInfo: DeviceInfo, username: String, password: String): Boolean {
        return try {
            smbConnectionManager.connectAsync(deviceInfo.ip, username, password)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取指定路径的文件列表
     */
    suspend fun getFileList(path: String): List<com.example.nasonly.data.smb.FileItem> {
        return smbDataSource.listFiles(path)
    }
    
    /**
     * 停止设备发现
     */
    fun stopDiscovery() {
        nasDiscoveryManager.stopDiscovery()
    }
    
    /**
     * 断开SMB连接
     */
    suspend fun disconnect() {
        smbConnectionManager.disconnect()
    }
}