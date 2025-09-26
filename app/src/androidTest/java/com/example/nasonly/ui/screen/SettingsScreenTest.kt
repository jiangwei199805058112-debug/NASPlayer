package com.example.nasonly.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import com.example.nasonly.MainActivity
import com.example.nasonly.ui.theme.NASOnlyTheme
import com.example.nasonly.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.Rule
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@HiltAndroidTest
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Mock
    private lateinit var mockViewModel: SettingsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        hiltRule.inject()
        
        // 设置默认的mock行为
        setupDefaultMockBehavior()
    }

    private fun setupDefaultMockBehavior() {
        `when`(mockViewModel.smbHost).thenReturn(MutableStateFlow(""))
        `when`(mockViewModel.smbShare).thenReturn(MutableStateFlow(""))
        `when`(mockViewModel.smbUsername).thenReturn(MutableStateFlow(""))
        `when`(mockViewModel.smbPassword).thenReturn(MutableStateFlow(""))
        `when`(mockViewModel.smbDomain).thenReturn(MutableStateFlow("WORKGROUP"))
        `when`(mockViewModel.resumePlayback).thenReturn(MutableStateFlow(true))
        `when`(mockViewModel.cacheSizeMB).thenReturn(MutableStateFlow(100))
        `when`(mockViewModel.networkTimeoutSeconds).thenReturn(MutableStateFlow(30))
        `when`(mockViewModel.bufferSizeKB).thenReturn(MutableStateFlow(1024))
        `when`(mockViewModel.maxBufferMs).thenReturn(MutableStateFlow(50000))
        `when`(mockViewModel.minBufferMs).thenReturn(MutableStateFlow(2500))
        `when`(mockViewModel.autoClearCache).thenReturn(MutableStateFlow(false))
        `when`(mockViewModel.wifiOnlyStreaming).thenReturn(MutableStateFlow(false))
        `when`(mockViewModel.isDarkMode).thenReturn(MutableStateFlow(false))
    }

    /**
     * 测试设置页面基本显示
     */
    @Test
    fun testSettingsScreenDisplay() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证设置页面标题
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()

        // 验证主要设置分组显示
        composeTestRule.onNodeWithText("SMB连接设置").assertIsDisplayed()
        composeTestRule.onNodeWithText("播放设置").assertIsDisplayed()
        composeTestRule.onNodeWithText("系统设置").assertIsDisplayed()
    }

    /**
     * 测试SMB连接设置
     */
    @Test
    fun testSmbConnectionSettings() {
        `when`(mockViewModel.smbHost).thenReturn(MutableStateFlow("192.168.1.100"))
        `when`(mockViewModel.smbShare).thenReturn(MutableStateFlow("media"))
        `when`(mockViewModel.smbUsername).thenReturn(MutableStateFlow("testuser"))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证SMB设置字段显示
        composeTestRule.onNodeWithText("服务器地址").assertIsDisplayed()
        composeTestRule.onNodeWithText("共享文件夹").assertIsDisplayed()
        composeTestRule.onNodeWithText("用户名").assertIsDisplayed()
        composeTestRule.onNodeWithText("密码").assertIsDisplayed()
        composeTestRule.onNodeWithText("域").assertIsDisplayed()

        // 验证当前值显示
        composeTestRule.onNodeWithText("192.168.1.100").assertIsDisplayed()
        composeTestRule.onNodeWithText("media").assertIsDisplayed()
        composeTestRule.onNodeWithText("testuser").assertIsDisplayed()
    }

    /**
     * 测试SMB设置输入
     */
    @Test
    fun testSmbSettingsInput() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 输入服务器地址
        composeTestRule.onNodeWithTag("smb_host_field")
            .performTextClearance()
            .performTextInput("192.168.1.200")

        // 验证ViewModel方法被调用
        verify(mockViewModel).setSmbHost("192.168.1.200")

        // 输入共享文件夹
        composeTestRule.onNodeWithTag("smb_share_field")
            .performTextClearance()
            .performTextInput("videos")

        verify(mockViewModel).setSmbShare("videos")

        // 输入用户名
        composeTestRule.onNodeWithTag("smb_username_field")
            .performTextClearance()
            .performTextInput("newuser")

        verify(mockViewModel).setSmbUsername("newuser")
    }

    /**
     * 测试密码字段隐藏显示切换
     */
    @Test
    fun testPasswordVisibilityToggle() {
        `when`(mockViewModel.smbPassword).thenReturn(MutableStateFlow("secret123"))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证密码字段默认隐藏
        composeTestRule.onNodeWithTag("smb_password_field")
            .assertTextEquals("•••••••••")

        // 点击显示密码按钮
        composeTestRule.onNodeWithTag("password_visibility_toggle")
            .performClick()

        // 验证密码显示
        composeTestRule.onNodeWithTag("smb_password_field")
            .assertTextContains("secret123")

        // 再次点击隐藏密码
        composeTestRule.onNodeWithTag("password_visibility_toggle")
            .performClick()

        // 验证密码再次隐藏
        composeTestRule.onNodeWithTag("smb_password_field")
            .assertTextEquals("•••••••••")
    }

    /**
     * 测试连接测试功能
     */
    @Test
    fun testConnectionTest() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击测试连接按钮
        composeTestRule.onNodeWithText("测试连接").performClick()

        // 验证测试连接方法被调用
        verify(mockViewModel).testConnection()
    }

    /**
     * 测试播放设置开关
     */
    @Test
    fun testPlaybackSettings() {
        `when`(mockViewModel.resumePlayback).thenReturn(MutableStateFlow(true))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证播放设置开关状态
        composeTestRule.onNodeWithText("断点续播").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_playback_switch")
            .assertIsOn()

        // 切换开关状态
        composeTestRule.onNodeWithTag("resume_playback_switch")
            .performClick()

        // 验证ViewModel方法被调用
        verify(mockViewModel).setResumePlayback(false)
    }

    /**
     * 测试缓存大小设置
     */
    @Test
    fun testCacheSizeSettings() {
        `when`(mockViewModel.cacheSizeMB).thenReturn(MutableStateFlow(100))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证缓存大小显示
        composeTestRule.onNodeWithText("缓存大小").assertIsDisplayed()
        composeTestRule.onNodeWithText("100 MB").assertIsDisplayed()

        // 点击缓存大小设置
        composeTestRule.onNodeWithText("缓存大小").performClick()

        // 验证缓存大小调整器显示
        composeTestRule.onNodeWithTag("cache_size_slider").assertIsDisplayed()

        // 调整缓存大小
        composeTestRule.onNodeWithTag("cache_size_slider")
            .performTouchInput {
                swipeRight()
            }

        // 验证设置被更新（具体值取决于滑块实现）
        verify(mockViewModel, atLeastOnce()).setCacheSizeMB(anyInt())
    }

    /**
     * 测试网络超时设置
     */
    @Test
    fun testNetworkTimeoutSettings() {
        `when`(mockViewModel.networkTimeoutSeconds).thenReturn(MutableStateFlow(30))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证网络超时显示
        composeTestRule.onNodeWithText("网络超时").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 秒").assertIsDisplayed()

        // 点击网络超时设置
        composeTestRule.onNodeWithText("网络超时").performClick()

        // 验证超时设置对话框显示
        composeTestRule.onNodeWithTag("timeout_picker_dialog").assertIsDisplayed()

        // 选择新的超时值
        composeTestRule.onNodeWithText("60").performClick()
        composeTestRule.onNodeWithText("确定").performClick()

        // 验证设置被更新
        verify(mockViewModel).setNetworkTimeoutSeconds(60)
    }

    /**
     * 测试缓冲区设置
     */
    @Test
    fun testBufferSettings() {
        `when`(mockViewModel.bufferSizeKB).thenReturn(MutableStateFlow(1024))
        `when`(mockViewModel.maxBufferMs).thenReturn(MutableStateFlow(50000))
        `when`(mockViewModel.minBufferMs).thenReturn(MutableStateFlow(2500))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证缓冲区设置显示
        composeTestRule.onNodeWithText("缓冲区大小").assertIsDisplayed()
        composeTestRule.onNodeWithText("1024 KB").assertIsDisplayed()
        composeTestRule.onNodeWithText("最大缓冲时间").assertIsDisplayed()
        composeTestRule.onNodeWithText("50 秒").assertIsDisplayed()
        composeTestRule.onNodeWithText("最小缓冲时间").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.5 秒").assertIsDisplayed()
    }

    /**
     * 测试高级设置折叠展开
     */
    @Test
    fun testAdvancedSettingsExpansion() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证高级设置默认折叠
        composeTestRule.onNodeWithText("高级设置").assertIsDisplayed()
        composeTestRule.onNodeWithText("缓冲区大小").assertDoesNotExist()

        // 点击展开高级设置
        composeTestRule.onNodeWithText("高级设置").performClick()

        // 验证高级设置项目显示
        composeTestRule.onNodeWithText("缓冲区大小").assertIsDisplayed()
        composeTestRule.onNodeWithText("最大缓冲时间").assertIsDisplayed()
        composeTestRule.onNodeWithText("最小缓冲时间").assertIsDisplayed()

        // 再次点击折叠
        composeTestRule.onNodeWithText("高级设置").performClick()

        // 验证高级设置项目隐藏
        composeTestRule.onNodeWithText("缓冲区大小").assertDoesNotExist()
    }

    /**
     * 测试系统设置开关
     */
    @Test
    fun testSystemSettings() {
        `when`(mockViewModel.autoClearCache).thenReturn(MutableStateFlow(false))
        `when`(mockViewModel.wifiOnlyStreaming).thenReturn(MutableStateFlow(false))
        `when`(mockViewModel.isDarkMode).thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证系统设置开关
        composeTestRule.onNodeWithText("自动清理缓存").assertIsDisplayed()
        composeTestRule.onNodeWithText("仅WiFi播放").assertIsDisplayed()
        composeTestRule.onNodeWithText("深色主题").assertIsDisplayed()

        // 切换开关状态
        composeTestRule.onNodeWithTag("auto_clear_cache_switch").performClick()
        verify(mockViewModel).setAutoClearCache(true)

        composeTestRule.onNodeWithTag("wifi_only_switch").performClick()
        verify(mockViewModel).setWifiOnlyStreaming(true)

        composeTestRule.onNodeWithTag("dark_mode_switch").performClick()
        verify(mockViewModel).setDarkMode(true)
    }

    /**
     * 测试缓存清理功能
     */
    @Test
    fun testCacheClear() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击清理缓存按钮
        composeTestRule.onNodeWithText("清理缓存").performClick()

        // 验证确认对话框显示
        composeTestRule.onNodeWithText("确认清理缓存").assertIsDisplayed()
        composeTestRule.onNodeWithText("这将删除所有缓存的文件和元数据").assertIsDisplayed()

        // 确认清理
        composeTestRule.onNodeWithText("清理").performClick()

        // 验证清理方法被调用
        verify(mockViewModel).clearCache()
    }

    /**
     * 测试关于页面
     */
    @Test
    fun testAboutPage() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击关于按钮
        composeTestRule.onNodeWithText("关于").performClick()

        // 验证关于信息显示
        composeTestRule.onNodeWithText("NAS Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("版本").assertIsDisplayed()
        composeTestRule.onNodeWithText("开源许可").assertIsDisplayed()
    }

    /**
     * 测试设置项目搜索
     */
    @Test
    fun testSettingsSearch() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击搜索图标
        composeTestRule.onNodeWithTag("settings_search").performClick()

        // 输入搜索关键词
        composeTestRule.onNodeWithTag("search_field")
            .performTextInput("缓存")

        // 验证相关设置项目显示
        composeTestRule.onNodeWithText("缓存大小").assertIsDisplayed()
        composeTestRule.onNodeWithText("自动清理缓存").assertIsDisplayed()

        // 验证不相关项目隐藏
        composeTestRule.onNodeWithText("服务器地址").assertDoesNotExist()
    }

    /**
     * 测试导入导出设置
     */
    @Test
    fun testImportExportSettings() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 测试导出设置
        composeTestRule.onNodeWithText("导出设置").performClick()
        verify(mockViewModel).exportSettings()

        // 测试导入设置
        composeTestRule.onNodeWithText("导入设置").performClick()
        
        // 验证文件选择器启动（具体实现取决于文件选择器）
        verify(mockViewModel).importSettings()
    }

    /**
     * 测试设置验证
     */
    @Test
    fun testSettingsValidation() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 输入无效的服务器地址
        composeTestRule.onNodeWithTag("smb_host_field")
            .performTextClearance()
            .performTextInput("invalid..address")

        // 验证错误提示显示
        composeTestRule.onNodeWithText("请输入有效的IP地址或域名").assertIsDisplayed()

        // 输入有效地址
        composeTestRule.onNodeWithTag("smb_host_field")
            .performTextClearance()
            .performTextInput("192.168.1.100")

        // 验证错误提示消失
        composeTestRule.onNodeWithText("请输入有效的IP地址或域名").assertDoesNotExist()
    }

    /**
     * 测试滚动和布局
     */
    @Test
    fun testScrollingAndLayout() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 滚动到底部
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasText("关于"))

        // 验证底部项目可见
        composeTestRule.onNodeWithText("关于").assertIsDisplayed()

        // 滚动回顶部
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasText("SMB连接设置"))

        // 验证顶部项目可见
        composeTestRule.onNodeWithText("SMB连接设置").assertIsDisplayed()
    }

    /**
     * 测试深色主题切换效果
     */
    @Test
    fun testDarkModeToggleEffect() {
        `when`(mockViewModel.isDarkMode).thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 切换到深色主题
        composeTestRule.onNodeWithTag("dark_mode_switch").performClick()

        // 验证主题切换方法被调用
        verify(mockViewModel).setDarkMode(true)

        // 在实际应用中，这里可能需要重新创建Activity或Fragment来测试主题变化
    }

    /**
     * 测试网络状态检测
     */
    @Test
    fun testNetworkStatusDetection() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 模拟网络断开
        // 验证相关设置项目显示警告或禁用状态
        composeTestRule.onNodeWithTag("network_status_indicator").assertExists()
    }

    /**
     * 测试键盘导航
     */
    @Test
    fun testKeyboardNavigation() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                SettingsScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 测试Tab键导航
        composeTestRule.onNodeWithTag("smb_host_field")
            .requestFocus()
            .assertIsFocused()

        // 模拟Tab键移动到下一个字段
        // 注意：实际的键盘导航测试可能需要不同的方法
        composeTestRule.onNodeWithTag("smb_share_field")
            .performClick()
            .assertIsFocused()
    }
}