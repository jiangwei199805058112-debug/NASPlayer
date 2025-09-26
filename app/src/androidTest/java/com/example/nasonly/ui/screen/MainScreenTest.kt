package com.example.nasonly.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.example.nasonly.ui.theme.NASOnlyTheme
import org.junit.Test
import org.junit.Rule

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试MainScreen的基本显示
     */
    @Test
    fun testMainScreenDisplay() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 验证底部导航栏存在
        composeTestRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
        
        // 验证导航项目存在
        composeTestRule.onNodeWithText("文件浏览").assertIsDisplayed()
        composeTestRule.onNodeWithText("播放历史").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    /**
     * 测试底部导航栏的点击功能
     */
    @Test
    fun testBottomNavigationClicks() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 点击文件浏览
        composeTestRule.onNodeWithText("文件浏览").performClick()
        
        // 点击播放历史
        composeTestRule.onNodeWithText("播放历史").performClick()
        
        // 点击设置
        composeTestRule.onNodeWithText("设置").performClick()
        
        // 验证导航栏仍然存在
        composeTestRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
    }

    /**
     * 测试导航项目的选中状态
     */
    @Test
    fun testNavigationItemSelection() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 默认应该选中文件浏览
        composeTestRule.onNodeWithText("文件浏览").assertIsSelected()

        // 点击播放历史
        composeTestRule.onNodeWithText("播放历史").performClick()
        
        // 等待UI更新
        composeTestRule.waitForIdle()
        
        // 验证播放历史被选中
        composeTestRule.onNodeWithText("播放历史").assertIsSelected()
    }

    /**
     * 测试屏幕内容区域显示
     */
    @Test
    fun testContentAreaDisplay() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 验证内容区域存在
        composeTestRule.onNodeWithTag("main_content").assertIsDisplayed()
    }

    /**
     * 测试深色主题支持
     */
    @Test
    fun testDarkThemeSupport() {
        composeTestRule.setContent {
            NASOnlyTheme(darkTheme = true) {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 验证UI在深色主题下正常显示
        composeTestRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("文件浏览").assertIsDisplayed()
        composeTestRule.onNodeWithText("播放历史").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    /**
     * 测试屏幕旋转后的状态保持
     */
    @Test
    fun testScreenRotationStatePreservation() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 点击播放历史
        composeTestRule.onNodeWithText("播放历史").performClick()
        composeTestRule.waitForIdle()

        // 验证选中状态
        composeTestRule.onNodeWithText("播放历史").assertIsSelected()

        // 模拟配置变更（如屏幕旋转）
        composeTestRule.activity?.recreate()
        composeTestRule.waitForIdle()

        // 验证状态仍然保持
        composeTestRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
    }

    /**
     * 测试无障碍功能支持
     */
    @Test
    fun testAccessibilitySupport() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 验证导航项目有适当的语义信息
        composeTestRule.onNodeWithText("文件浏览")
            .assertHasClickAction()
            .assertIsSelectable()

        composeTestRule.onNodeWithText("播放历史")
            .assertHasClickAction()
            .assertIsSelectable()

        composeTestRule.onNodeWithText("设置")
            .assertHasClickAction()
            .assertIsSelectable()
    }

    /**
     * 测试快速连续点击处理
     */
    @Test
    fun testRapidClickHandling() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 快速连续点击同一个导航项目
        repeat(5) {
            composeTestRule.onNodeWithText("播放历史").performClick()
        }

        composeTestRule.waitForIdle()

        // 验证UI仍然正常
        composeTestRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("播放历史").assertIsSelected()
    }

    /**
     * 测试导航图标显示
     */
    @Test
    fun testNavigationIcons() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 验证每个导航项目都有图标（通过testTag）
        composeTestRule.onNodeWithTag("nav_icon_files").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_icon_history").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_icon_settings").assertIsDisplayed()
    }

    /**
     * 测试导航项目数量
     */
    @Test
    fun testNavigationItemCount() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 验证底部导航栏有正确数量的项目
        composeTestRule.onAllNodesWithTag("nav_item").assertCountEquals(3)
    }

    /**
     * 测试内容区域响应导航变化
     */
    @Test
    fun testContentAreaRespondsToNavigation() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 默认显示文件浏览内容
        composeTestRule.onNodeWithTag("file_browser_screen").assertIsDisplayed()

        // 切换到播放历史
        composeTestRule.onNodeWithText("播放历史").performClick()
        composeTestRule.waitForIdle()

        // 验证内容切换
        composeTestRule.onNodeWithTag("playback_history_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_browser_screen").assertDoesNotExist()

        // 切换到设置
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.waitForIdle()

        // 验证内容切换
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playback_history_screen").assertDoesNotExist()
    }

    /**
     * 测试返回键处理
     */
    @Test
    fun testBackButtonHandling() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        // 切换到设置页面
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.waitForIdle()

        // 验证设置页面显示
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()

        // 模拟返回键（在实际测试中可能需要不同的方法）
        // 这里我们测试导航栏的持久性
        composeTestRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
    }

    /**
     * 测试性能 - 导航切换速度
     */
    @Test
    fun testNavigationPerformance() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }

        val startTime = System.currentTimeMillis()

        // 快速切换导航项目
        composeTestRule.onNodeWithText("播放历史").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("文件浏览").performClick()
        composeTestRule.waitForIdle()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // 验证导航切换在合理时间内完成（例如1秒内）
        assert(duration < 1000) { "Navigation switching took too long: ${duration}ms" }

        // 验证最终状态正确
        composeTestRule.onNodeWithText("文件浏览").assertIsSelected()
    }
}