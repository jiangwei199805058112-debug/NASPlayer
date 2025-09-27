package com.example.nasonly.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.discovery.NasDiscoveryManager
import com.example.nasonly.data.preferences.UserPreferences
import com.example.nasonly.data.smb.SmbManager
import com.example.nasonly.model.NasDevice
import com.example.nasonly.model.NasHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NasDiscoveryViewModel @Inject constructor(
    private val smb: SmbManager,
    private val discoveryManager: NasDiscoveryManager,
    private val userPreferences: UserPreferences,
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

    // 发现UI状态
    data class DiscoveryUiState(
        val isDiscovering: Boolean = false,
        val devices: List<NasDevice> = emptyList(),
        val error: String? = null,
    )
    private val _discoveryState = MutableStateFlow(DiscoveryUiState())
    val discoveryState: StateFlow<DiscoveryUiState> = _discoveryState.asStateFlow()

    private var discoveryJob: Job? = null

    /**
     * 开始发现NAS设备
     */
    fun startDiscovery() {
        if (discoveryJob?.isActive == true) {
            Timber.d("Discovery already in progress, ignoring start request")
            return
        }

        _discoveryState.value = DiscoveryUiState(isDiscovering = true, devices = emptyList(), error = null)
        discoveryManager.acquireMulticastLock()

        discoveryJob = viewModelScope.launch {
            try {
                discoveryManager.discoverAll()
                    .collect { devices ->
                        _discoveryState.value = _discoveryState.value.copy(devices = devices)
                    }
                Timber.d("Discovery completed successfully")
            } catch (e: Throwable) {
                Timber.w(e, "Discovery failed")
                _discoveryState.value = _discoveryState.value.copy(
                    isDiscovering = false,
                    error = e.message ?: "发现失败",
                )
            } finally {
                _discoveryState.value = _discoveryState.value.copy(isDiscovering = false)
                discoveryManager.releaseMulticastLock()
                Timber.d("Discovery job finished")
            }
        }
    }

    /**
     * 停止发现
     */
    fun stopDiscovery() {
        Timber.d("Stopping discovery")
        discoveryJob?.cancel()
        discoveryJob = null
        _discoveryState.value = _discoveryState.value.copy(isDiscovering = false)
        discoveryManager.releaseMulticastLock()
    }

    /**
     * 连接到指定设备
     */
    fun connectToDevice(device: NasDevice, username: String, password: String) {
        val host = NasHost(
            host = device.ip.hostAddress!!,
            share = "", // 默认空，或从某处获取
            username = username,
            password = password,
            domain = "",
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
                        domain = host.domain,
                    )
                }.onSuccess {
                    Timber.i("SMB Connected: $host")
                    // 保存最后连接的NAS信息
                    userPreferences.setLastNasConnection(host.host, host.username)
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
