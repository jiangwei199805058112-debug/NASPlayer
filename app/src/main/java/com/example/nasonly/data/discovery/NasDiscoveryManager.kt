package com.example.nasonly.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NAS 设备自动发现管理器
 * 支持多种发现协议：mDNS/DNS-SD, SSDP, WS-Discovery, 子网扫描
 */
@Singleton
class NasDiscoveryManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "NasDiscoveryManager"
        private const val SMB_SERVICE_TYPE = "_smb._tcp"
        private const val SSDP_PORT = 1900
        private const val WS_DISCOVERY_PORT = 3702
        private const val SMB_PORT = 445
        private const val DISCOVERY_TIMEOUT = 10000L // 10秒
        private const val PING_TIMEOUT = 1000 // 1秒
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // 存储发现到的设备
    private val discoveredDevices = mutableSetOf<DeviceInfo>()
    private var discoveryJob: Job? = null

    /**
     * 开始NAS发现，返回Flow持续发送发现到的设备
     */
    fun startDiscovery(): Flow<List<DeviceInfo>> = flow {
        discoveredDevices.clear()

        try {
            Log.d(TAG, "Starting NAS discovery with multiple protocols")

            // 并行执行多种发现方法
            coroutineScope {
                val jobs = listOf(
                    async { discoverViaMdns() },
                    async { discoverViaSsdp() },
                    async { discoverViaWsDiscovery() },
                    async { discoverViaSubnetScan() },
                )

                // 等待一段时间让发现结果累积
                delay(DISCOVERY_TIMEOUT)

                // 取消所有发现任务
                jobs.forEach { it.cancel() }
            }

            Log.d(TAG, "Discovery completed, found ${discoveredDevices.size} devices")
            emit(discoveredDevices.toList())
        } catch (e: Exception) {
            Log.e(TAG, "Error during discovery: ${e.message}", e)
            emit(discoveredDevices.toList())
        }
    }

    /**
     * mDNS/DNS-SD 发现
     */
    private suspend fun discoverViaMdns() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting mDNS discovery for SMB services")

            val channel = Channel<DeviceInfo>(Channel.UNLIMITED)

            val discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    Log.e(TAG, "mDNS discovery start failed: $errorCode")
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    Log.e(TAG, "mDNS discovery stop failed: $errorCode")
                }

                override fun onDiscoveryStarted(serviceType: String?) {
                    Log.d(TAG, "mDNS discovery started for: $serviceType")
                }

                override fun onDiscoveryStopped(serviceType: String?) {
                    Log.d(TAG, "mDNS discovery stopped for: $serviceType")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    serviceInfo?.let { info ->
                        Log.d(TAG, "mDNS service found: ${info.serviceName}")
                        resolveService(info, channel)
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                    Log.d(TAG, "mDNS service lost: ${serviceInfo?.serviceName}")
                }
            }

            nsdManager.discoverServices(SMB_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

            // 收集发现的设备
            try {
                withTimeout(DISCOVERY_TIMEOUT) {
                    while (true) {
                        val device = channel.receive()
                        discoveredDevices.add(device)
                        Log.d(TAG, "Added mDNS device: ${device.name} (${device.ip})")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.d(TAG, "mDNS discovery timeout")
            }

            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "mDNS discovery error: ${e.message}", e)
        }
    }

    /**
     * 解析mDNS服务
     */
    private fun resolveService(serviceInfo: NsdServiceInfo, channel: Channel<DeviceInfo>) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Service resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { info ->
                    val device = DeviceInfo(
                        name = info.serviceName,
                        ip = info.host?.hostAddress ?: "",
                        protocol = "mDNS",
                    )
                    channel.trySend(device)
                }
            }
        }

        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    /**
     * SSDP (UPnP) 发现
     */
    private suspend fun discoverViaSsdp() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting SSDP discovery")

            val ssdpMessage = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "ST: upnp:rootdevice\r\n" +
                "MX: 3\r\n\r\n"

            val socket = DatagramSocket()
            socket.soTimeout = 5000

            val group = InetAddress.getByName("239.255.255.250")
            val packet = DatagramPacket(
                ssdpMessage.toByteArray(),
                ssdpMessage.length,
                group,
                SSDP_PORT,
            )

            socket.send(packet)

            // 监听响应
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 5000) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)

                    val response = String(responsePacket.data, 0, responsePacket.length)
                    val deviceIp = responsePacket.address.hostAddress

                    if (response.contains("SERVER:") && deviceIp != null) {
                        // 检查是否支持SMB
                        if (checkSmbPort(deviceIp)) {
                            val device = DeviceInfo(
                                name = "UPnP Device",
                                ip = deviceIp,
                                protocol = "SSDP",
                            )
                            discoveredDevices.add(device)
                            Log.d(TAG, "Added SSDP device: ${device.name} (${device.ip})")
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // 正常超时，继续
                }
            }

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "SSDP discovery error: ${e.message}", e)
        }
    }

    /**
     * WS-Discovery 发现
     */
    private suspend fun discoverViaWsDiscovery() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting WS-Discovery")

            val wsDiscoveryMessage = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsd="http://schemas.xmlsoap.org/ws/2005/04/discovery">
                    <soap:Header>
                        <wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>
                        <wsa:MessageID>urn:uuid:${java.util.UUID.randomUUID()}</wsa:MessageID>
                        <wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>
                    </soap:Header>
                    <soap:Body>
                        <wsd:Probe/>
                    </soap:Body>
                </soap:Envelope>
            """.trimIndent()

            val socket = DatagramSocket()
            socket.soTimeout = 3000

            val group = InetAddress.getByName("239.255.255.250")
            val packet = DatagramPacket(
                wsDiscoveryMessage.toByteArray(),
                wsDiscoveryMessage.length,
                group,
                WS_DISCOVERY_PORT,
            )

            socket.send(packet)

            // 监听响应
            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 3000) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)

                    val deviceIp = responsePacket.address.hostAddress

                    if (deviceIp != null && checkSmbPort(deviceIp)) {
                        val device = DeviceInfo(
                            name = "WS-Discovery Device",
                            ip = deviceIp,
                            protocol = "WS-Discovery",
                        )
                        discoveredDevices.add(device)
                        Log.d(TAG, "Added WS-Discovery device: ${device.name} (${device.ip})")
                    }
                } catch (e: SocketTimeoutException) {
                    // 正常超时，继续
                }
            }

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "WS-Discovery error: ${e.message}", e)
        }
    }

    /**
     * 子网扫描发现（回退方案）
     */
    private suspend fun discoverViaSubnetScan() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting subnet scan discovery")

            val subnet = getCurrentSubnet()
            if (subnet == null) {
                Log.w(TAG, "Cannot determine current subnet")
                return@withContext
            }

            Log.d(TAG, "Scanning subnet: $subnet")

            // 并行扫描子网中的IP地址
            val jobs = mutableListOf<Job>()

            for (i in 1..254) {
                val job = async {
                    val ip = "$subnet.$i"
                    if (pingHost(ip) && checkSmbPort(ip)) {
                        val device = DeviceInfo(
                            name = "SMB Server",
                            ip = ip,
                            protocol = "Subnet Scan",
                        )
                        discoveredDevices.add(device)
                        Log.d(TAG, "Added subnet scan device: ${device.name} (${device.ip})")
                    }
                }
                jobs.add(job)
            }

            // 等待所有扫描完成
            jobs.forEach { job ->
                runCatching { job.join() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Subnet scan error: ${e.message}", e)
        }
    }

    /**
     * 获取当前子网
     */
    private fun getCurrentSubnet(): String? {
        try {
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            if (ipAddress == 0) return null

            val ip = String.format(
                "%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
            )

            return ip
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current subnet: ${e.message}", e)
            return null
        }
    }

    /**
     * Ping 主机检查连通性
     */
    private suspend fun pingHost(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(ip)
            address.isReachable(PING_TIMEOUT)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查SMB端口是否开放
     */
    private suspend fun checkSmbPort(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, SMB_PORT), PING_TIMEOUT)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 停止发现
     */
    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveredDevices.clear()
        Log.d(TAG, "Discovery stopped")
    }
}

/**
 * 发现到的设备信息
 */
data class DeviceInfo(
    val name: String,
    val ip: String,
    val protocol: String,
)
