package com.example.nasonly.data.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.nasonly.model.NasDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.runningFold
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NAS 设备自动发现管理器
 * 支持 mDNS、NetBIOS、端口探测三路并行
 */
@Singleton
class NasDiscoveryManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "NASDiscovery"
        private const val MDNS_PORT = 5353
        private const val NETBIOS_PORT = 137
        private const val SMB_PORT = 445
        private const val DISCOVERY_TIMEOUT = 3000L // 3秒每路，减少总时间
        private const val OVERALL_TIMEOUT = 4000L // 整体4秒，减少总时间
    }

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * 获取并持有 MulticastLock
     */
    fun acquireMulticastLock() {
        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock("nas_mdns_lock").apply {
                setReferenceCounted(true)
            }
        }
        if (!multicastLock!!.isHeld) {
            multicastLock!!.acquire()
            Log.d(TAG, "MulticastLock acquired")
        }
    }

    /**
     * 释放 MulticastLock
     */
    fun releaseMulticastLock() {
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "MulticastLock released")
            }
        }
    }

    /**
     * 逐条发射发现的设备
     */
    fun discover(): Flow<NasDevice> = channelFlow {
        withContext(Dispatchers.IO) {
            val dedup = ConcurrentHashMap.newKeySet<String>()
            suspend fun emitAll(list: List<NasDevice>) {
                list.forEach { dev ->
                    try {
                        val key = dev.ip.hostAddress!!
                        if (dedup.add(key)) {
                            send(dev)
                            Log.d(TAG, "Discovered device: ${dev.name ?: "Unknown"} at ${dev.ip.hostAddress}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to emit device ${dev.ip.hostAddress}: ${e.message}")
                    }
                }
            }
            try {
                withTimeout(OVERALL_TIMEOUT) {
                    coroutineScope {
                        launch { emitAll(mdnsProbe()) }
                        launch { emitAll(netbiosProbe()) }
                        launch { emitAll(portProbe()) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Discovery failed: ${e.message}")
            }
        }
    }

    /**
     * 聚合发射设备列表
     */
    fun discoverAll(): Flow<List<NasDevice>> = discover()
        .runningFold(emptyList<NasDevice>()) { acc, item ->
            if (acc.any { it.ip == item.ip }) acc else acc + item
        }

    /**
     * mDNS 探测 _smb._tcp
     */
    private suspend fun mdnsProbe(): List<NasDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<NasDevice>()
        val socket = MulticastSocket(MDNS_PORT)
        try {
            socket.joinGroup(InetAddress.getByName("224.0.0.251"))
            socket.soTimeout = 1000 // 1秒超时，减少阻塞时间

            val query = byteArrayOf(
                // DNS 查询头部
                0x00.toByte(), 0x00.toByte(), // ID
                0x01.toByte(), 0x00.toByte(), // Flags: recursion desired
                0x00.toByte(), 0x01.toByte(), // QDCOUNT: 1 question
                0x00.toByte(), 0x00.toByte(), // ANCOUNT
                0x00.toByte(), 0x00.toByte(), // NSCOUNT
                0x00.toByte(), 0x00.toByte(), // ARCOUNT
                // Question: _smb._tcp.local
                0x04.toByte(), 0x5f.toByte(), 0x73.toByte(), 0x6d.toByte(), 0x62.toByte(), // _smb
                0x04.toByte(), 0x5f.toByte(), 0x74.toByte(), 0x63.toByte(), 0x70.toByte(), // _tcp
                0x05.toByte(), 0x6c.toByte(), 0x6f.toByte(), 0x63.toByte(), 0x61.toByte(), 0x6c.toByte(), // local
                0x00.toByte(), // null terminator
                0x00.toByte(), 0x0c.toByte(), // QTYPE: PTR
                0x00.toByte(), 0x01, // QCLASS: IN
            )

            socket.send(DatagramPacket(query, query.size, InetAddress.getByName("224.0.0.251"), MDNS_PORT))

            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)

            // 使用更安全的循环，避免长时间阻塞
            val startTime = System.currentTimeMillis()
            var receiveCount = 0
            while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT && receiveCount < 10) {
                try {
                    socket.receive(packet)
                    receiveCount++
                    // 简单解析响应，提取IP（实际实现需要完整DNS解析）
                    val ip = packet.address
                    if (ip.isSiteLocalAddress) {
                        devices.add(NasDevice(ip, "SMB Device", true))
                    }
                } catch (e: Exception) {
                    // Socket超时或其他错误，跳出循环
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "mDNS probe failed: ${e.message}")
        } finally {
            socket.close()
        }
        devices
    }

    /**
     * NetBIOS 探测 UDP 137
     */
    private suspend fun netbiosProbe(): List<NasDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<NasDevice>()
        val socket = DatagramSocket()
        try {
            socket.broadcast = true
            socket.soTimeout = 1000 // 1秒超时，减少阻塞时间

            val query = byteArrayOf(
                // NetBIOS Name Query
                0x82.toByte(), 0x28.toByte(), // Transaction ID
                0x00.toByte(), 0x00.toByte(), // Flags
                0x00.toByte(), 0x01.toByte(), // Questions
                0x00.toByte(), 0x00.toByte(), // Answer RRs
                0x00.toByte(), 0x00.toByte(), // Authority RRs
                0x00.toByte(), 0x00.toByte(), // Additional RRs
                // Question: *<00>
                0x20.toByte(), 0x43.toByte(), 0x4b.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(), 0x41.toByte(),
                0x00.toByte(), // Name
                0x00.toByte(), 0x21.toByte(), // Type: NBSTAT
                0x00.toByte(), 0x01.toByte(), // Class: IN
            )

            val broadcastAddr = getBroadcastAddress()
            socket.send(DatagramPacket(query, query.size, broadcastAddr, NETBIOS_PORT))

            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)

            // 使用更安全的循环，避免长时间阻塞
            val startTime = System.currentTimeMillis()
            var receiveCount = 0
            while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT && receiveCount < 5) {
                try {
                    socket.receive(packet)
                    receiveCount++
                    val ip = packet.address
                    if (ip.isSiteLocalAddress) {
                        devices.add(NasDevice(ip, "NetBIOS Device", true))
                    }
                } catch (e: Exception) {
                    // Socket超时或其他错误，跳出循环
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "NetBIOS probe failed: ${e.message}")
        } finally {
            socket.close()
        }
        devices
    }

    /**
     * 端口探测 445
     */
    private suspend fun portProbe(): List<NasDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<NasDevice>()
        val localAddr = getLocalAddress()
        if (localAddr == null) return@withContext devices

        val subnet = getSubnet(localAddr)
        for (i in 1..254) {
            val ip = InetAddress.getByName("$subnet.$i")
            if (ip == localAddr) continue

            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, SMB_PORT), 500) // 500ms timeout
                socket.close()
                devices.add(NasDevice(ip, "SMB Port Open", true))
            } catch (e: Exception) {
                // Not reachable
            }
        }
        devices
    }

    @Suppress("DEPRECATION")
    private fun getBroadcastAddress(): InetAddress {
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress
        val subnet = (ip and 0xFFFFFF00.toInt()) or 0xFF
        return InetAddress.getByAddress(
            byteArrayOf(
                (subnet shr 24).toByte(),
                (subnet shr 16).toByte(),
                (subnet shr 8).toByte(),
                subnet.toByte(),
            ),
        )
    }

    private fun getLocalAddress(): InetAddress? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val intf = interfaces.nextElement()
            val addresses = intf.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address && addr.isSiteLocalAddress) {
                    return addr
                }
            }
        }
        return null
    }

    private fun getSubnet(addr: InetAddress): String {
        val bytes = addr.address
        return "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}"
    }
}
