package com.example.nasonly.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.discovery.DeviceInfo
import com.example.nasonly.data.smb.SmbManager
import com.example.nasonly.model.NasHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NasDiscoveryViewModel @Inject constructor(
    private val smb: SmbManager
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data object Connecting : UiState
        data class Connected(val host: NasHost) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 一次性导航事件（不重放、不丢失） */
    sealed interface NavEvent {
        data class ToLibrary(val host: NasHost) : NavEvent
    }
    private val _navEvents = MutableSharedFlow<NavEvent>(replay = 0, extraBufferCapacity = 1)
    val navEvents: SharedFlow<NavEvent> = _navEvents.asSharedFlow()

    private val connectMutex = Mutex()

    // 简单的发现状态（模拟）
    data class DiscoveryState(
        val isDiscovering: Boolean = false,
        val isConnecting: Boolean = false,
        val devices: List<DeviceInfo> = emptyList()
    )
    private val _discoveryState = MutableStateFlow(DiscoveryState())
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    /**
     * 开始发现NAS设备（模拟）
     */
    fun startDiscovery() {
        _discoveryState.value = _discoveryState.value.copy(isDiscovering = true)
        // 模拟发现一些设备
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // 模拟延迟
            val mockDevices = listOf(
                DeviceInfo("NAS-001", "192.168.1.100", "SMB"),
                DeviceInfo("NAS-002", "192.168.1.101", "SMB")
            )
            _discoveryState.value = _discoveryState.value.copy(isDiscovering = false, devices = mockDevices)
        }
    }

    /**
     * 停止发现
     */
    fun stopDiscovery() {
        _discoveryState.value = _discoveryState.value.copy(isDiscovering = false)
    }

    /**
     * 连接到指定设备
     */
    fun connectToDevice(device: DeviceInfo, username: String, password: String) {
        val host = NasHost(
            host = device.ip,
            share = "", // 默认空，或从某处获取
            username = username,
            password = password,
            domain = ""
        )
        connect(host)
    }

    fun connect(host: NasHost) {
        viewModelScope.launch(Dispatchers.IO) {
            connectMutex.withLock {
                _uiState.value = UiState.Connecting
                runCatching {
                    smb.connect(
                        host = host.host,
                        share = host.share,
                        username = host.username,
                        password = host.password,
                        domain = host.domain
                    )
                }.onSuccess {
                    Timber.i("SMB Connected: $host")
                    _uiState.value = UiState.Connected(host)
                    _navEvents.tryEmit(NavEvent.ToLibrary(host))
                }.onFailure { e ->
                    Timber.w(e, "SMB connect failed")
                    _uiState.value = UiState.Error(e.message ?: "连接失败")
                }
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            // nasRepository.disconnect() 如果有
            _uiState.value = UiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
