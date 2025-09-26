package com.example.nasonly.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.discovery.DeviceInfo
import com.example.nasonly.data.repository.NasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NasDiscoveryViewModel @Inject constructor(
    private val nasRepository: NasRepository,
) : ViewModel() {

    private val _discoveryState = MutableStateFlow(DiscoveryState())
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    /**
     * 开始发现NAS设备
     */
    fun startDiscovery() {
        _discoveryState.value = _discoveryState.value.copy(isDiscovering = true, devices = emptyList())

        viewModelScope.launch {
            nasRepository.discoverNasDevices().collect { devices ->
                _discoveryState.value = _discoveryState.value.copy(
                    isDiscovering = false,
                    devices = devices,
                    error = null,
                )
            }
        }
    }

    /**
     * 连接到指定设备
     */
    fun connectToDevice(device: DeviceInfo, username: String, password: String) {
        _discoveryState.value = _discoveryState.value.copy(isConnecting = true, error = null)

        viewModelScope.launch {
            try {
                val success = nasRepository.connectToNas(device, username, password)
                if (success) {
                    _discoveryState.value = _discoveryState.value.copy(
                        isConnecting = false,
                        connectedDevice = device,
                        error = null,
                    )
                } else {
                    _discoveryState.value = _discoveryState.value.copy(
                        isConnecting = false,
                        error = "连接失败：用户名或密码错误",
                    )
                }
            } catch (e: Exception) {
                _discoveryState.value = _discoveryState.value.copy(
                    isConnecting = false,
                    error = "连接失败：${e.message}",
                )
            }
        }
    }

    /**
     * 停止发现
     */
    fun stopDiscovery() {
        nasRepository.stopDiscovery()
        _discoveryState.value = _discoveryState.value.copy(isDiscovering = false)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            nasRepository.disconnect()
            _discoveryState.value = _discoveryState.value.copy(connectedDevice = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}

/**
 * 发现状态
 */
data class DiscoveryState(
    val isDiscovering: Boolean = false,
    val isConnecting: Boolean = false,
    val devices: List<DeviceInfo> = emptyList(),
    val connectedDevice: DeviceInfo? = null,
    val error: String? = null,
)
