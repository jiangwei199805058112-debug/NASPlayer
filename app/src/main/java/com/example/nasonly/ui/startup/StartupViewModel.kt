package com.example.nasonly.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.preferences.UserPreferences
import com.example.nasonly.data.smb.SmbManager
import com.example.nasonly.model.NasHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val smb: SmbManager,
) : ViewModel() {

    sealed interface StartupState {
        data object Checking : StartupState
        data class AutoConnect(val host: NasHost) : StartupState
        data object GoToDiscovery : StartupState
    }

    private val _startupState = MutableStateFlow<StartupState>(StartupState.Checking)
    val startupState: StateFlow<StartupState> = _startupState

    init {
        checkAutoConnect()
    }

    private fun checkAutoConnect() {
        viewModelScope.launch {
            val lastIp = userPreferences.lastNasIp.first()
            val lastUsername = userPreferences.lastUsername.first()

            if (lastIp != null && lastUsername != null) {
                // 尝试自动连接到最后连接的NAS
                val host = NasHost(
                    host = lastIp,
                    share = "",
                    username = lastUsername,
                    password = "", // 需要用户输入或保存密码，这里先用空密码尝试
                    domain = "",
                )

                // 如果密码为空，跳过自动连接，直接进入发现页面
                if (host.password.isEmpty()) {
                    Timber.i("Skipping auto-connect due to empty password for host: $host")
                    _startupState.value = StartupState.GoToDiscovery
                } else {
                    runCatching {
                        smb.connect(
                            host = host.host,
                            share = host.share,
                            username = host.username,
                            password = host.password,
                            domain = host.domain,
                        )
                    }.onSuccess {
                        Timber.i("Auto-connected to last NAS: $host")
                        _startupState.value = StartupState.AutoConnect(host)
                    }.onFailure { e ->
                        Timber.w(e, "Auto-connect failed for $host")
                        _startupState.value = StartupState.GoToDiscovery
                    }
                }
            } else {
                _startupState.value = StartupState.GoToDiscovery
            }
        }
    }
}
