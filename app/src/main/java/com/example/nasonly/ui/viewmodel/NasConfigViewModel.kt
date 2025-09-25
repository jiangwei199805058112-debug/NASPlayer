package com.example.nasonly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.datastore.NasPrefs
import com.example.nasonly.repository.SmbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NasConfigViewModel @Inject constructor(
    private val nasPrefs: NasPrefs,
    private val smbRepository: SmbRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NasConfigUiState())
    val uiState: StateFlow<NasConfigUiState> = _uiState

    init {
        viewModelScope.launch {
            nasPrefs.flow().collect { config ->
                _uiState.value = _uiState.value.copy(
                    host = config.host,
                    share = config.share,
                    username = config.username,
                    password = config.password,
                    domain = config.domain,
                    smbVersion = config.smbVersion
                )
            }
        }
    }

    fun saveConfig(
        host: String,
        share: String,
        username: String,
        password: String,
        domain: String,
        smbVersion: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // 保存到 DataStore
                nasPrefs.save(host, share, username, password, domain, smbVersion)
                
                // 配置 SMB 连接
                smbRepository.configureConnection(host, share, username, password, domain)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    testResult = "配置已保存"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "保存配置失败: ${e.message}"
                )
            }
        }
    }

    fun testConnection(
        host: String,
        share: String,
        username: String,
        password: String,
        domain: String,
        smbVersion: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, testResult = null)
                
                // 配置连接
                smbRepository.configureConnection(host, share, username, password, domain)
                
                // 测试连接
                val result = smbRepository.testConnection()
                result.fold(
                    onSuccess = { message ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            testResult = message
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "连接测试失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "连接测试异常: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class NasConfigUiState(
    val host: String = "",
    val share: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val smbVersion: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val testResult: String? = null
)