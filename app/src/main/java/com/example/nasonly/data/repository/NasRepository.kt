package com.example.nasonly.data.repository

import com.example.nasonly.data.discovery.NasDiscoveryManager
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbDataSource
import com.example.nasonly.data.smb.SmbFileInfo
import com.example.nasonly.model.NasDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NAS Repository - 整合SMB连接和设备发现功能
 */
@Singleton
class NasRepository @Inject constructor(
    private val smbConnectionManager: SmbConnectionManager,
    private val smbDataSource: SmbDataSource,
    private val nasDiscoveryManager: NasDiscoveryManager,
) {

    /**
     * 发现网络中的NAS设备
     */
    fun discoverNasDevices(): Flow<List<NasDevice>> {
        return nasDiscoveryManager.discoverAll()
    }

    /**
     * 连接到指定的NAS设备
     */
    suspend fun connectToNas(device: NasDevice, username: String, password: String, share: String = "public"): Boolean {
        return try {
            // 先配置连接参数，使用提供的共享名称或默认值
            smbConnectionManager.configure(device.ip.hostAddress!!, share, username, password, "")
            // 然后尝试连接
            smbConnectionManager.connectAsync()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取指定路径的文件列表
     */
    suspend fun getFileList(path: String): List<SmbFileInfo> {
        // TODO: Fix this method to work with new SMB API
        // val result = smbDataSource.listFiles(path)
        // return result.getOrNull() ?: emptyList()
        return emptyList()
    }

    /**
     * 停止设备发现
     */
    fun stopDiscovery() {
        // nasDiscoveryManager 现在没有 stopDiscovery 方法，使用 Flow 的取消机制
    }

    /**
     * 断开SMB连接
     */
    suspend fun disconnect() {
        smbConnectionManager.disconnect()
    }
}
