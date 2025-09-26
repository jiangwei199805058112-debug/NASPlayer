package com.example.nasonly.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import com.example.nasonly.MainActivity
import com.example.nasonly.data.model.MediaFile
import com.example.nasonly.ui.theme.NASOnlyTheme
import com.example.nasonly.ui.viewmodel.MediaLibraryViewModel
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
class MediaLibraryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Mock
    private lateinit var mockViewModel: MediaLibraryViewModel

    private val testFiles = listOf(
        MediaFile(
            name = "Video1.mp4",
            path = "/path/to/Video1.mp4",
            isDirectory = false,
            size = 1024L * 1024L,
            lastModified = System.currentTimeMillis(),
            mimeType = "video/mp4"
        ),
        MediaFile(
            name = "Video2.mkv",
            path = "/path/to/Video2.mkv",
            isDirectory = false,
            size = 2048L * 1024L,
            lastModified = System.currentTimeMillis(),
            mimeType = "video/x-matroska"
        ),
        MediaFile(
            name = "Movies",
            path = "/path/to/Movies",
            isDirectory = true,
            size = 0L,
            lastModified = System.currentTimeMillis(),
            mimeType = null
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        hiltRule.inject()
        
        // 设置默认的mock行为
        setupDefaultMockBehavior()
    }

    private fun setupDefaultMockBehavior() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(emptyList()))
        `when`(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        `when`(mockViewModel.errorMessage).thenReturn(MutableStateFlow(null))
        `when`(mockViewModel.currentPath).thenReturn(MutableStateFlow("/"))
        `when`(mockViewModel.hasMoreFiles).thenReturn(MutableStateFlow(false))
        `when`(mockViewModel.isLoadingMore).thenReturn(MutableStateFlow(false))
    }

    /**
     * 测试空状态显示
     */
    @Test
    fun testEmptyStateDisplay() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(emptyList()))
        `when`(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证无文件时的提示信息
        composeTestRule.onNodeWithText("当前目录没有媒体文件").assertIsDisplayed()
        composeTestRule.onNodeWithText("请检查SMB连接或选择其他目录").assertIsDisplayed()
    }

    /**
     * 测试加载状态显示
     */
    @Test
    fun testLoadingStateDisplay() {
        `when`(mockViewModel.isLoading).thenReturn(MutableStateFlow(true))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证加载指示器显示
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("加载中...").assertIsDisplayed()
    }

    /**
     * 测试文件列表显示
     */
    @Test
    fun testFileListDisplay() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))
        `when`(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证文件项目显示
        composeTestRule.onNodeWithText("Video1.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video2.mkv").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movies").assertIsDisplayed()

        // 验证文件大小显示
        composeTestRule.onNodeWithText("1.0 MB").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.0 MB").assertIsDisplayed()
    }

    /**
     * 测试文件点击功能
     */
    @Test
    fun testFileClickHandling() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))
        `when`(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击视频文件
        composeTestRule.onNodeWithText("Video1.mp4").performClick()
        
        // 验证viewModel方法被调用
        verify(mockViewModel).playFile(testFiles[0])

        // 点击目录
        composeTestRule.onNodeWithText("Movies").performClick()
        
        // 验证导航方法被调用
        verify(mockViewModel).navigateToDirectory(testFiles[2])
    }

    /**
     * 测试刷新功能
     */
    @Test
    fun testRefreshFunctionality() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 执行下拉刷新
        composeTestRule.onNodeWithTag("file_list")
            .performTouchInput {
                swipeDown()
            }

        composeTestRule.waitForIdle()

        // 验证刷新方法被调用
        verify(mockViewModel).refreshFiles()
    }

    /**
     * 测试加载更多功能
     */
    @Test
    fun testLoadMoreFunctionality() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))
        `when`(mockViewModel.hasMoreFiles).thenReturn(MutableStateFlow(true))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 滚动到列表底部
        composeTestRule.onNodeWithTag("file_list")
            .performScrollToIndex(testFiles.size - 1)

        composeTestRule.waitForIdle()

        // 验证加载更多方法被调用
        verify(mockViewModel).loadMoreFiles()
    }

    /**
     * 测试错误状态显示
     */
    @Test
    fun testErrorStateDisplay() {
        val errorMessage = "连接SMB服务器失败"
        `when`(mockViewModel.errorMessage).thenReturn(MutableStateFlow(errorMessage))
        `when`(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证错误消息显示
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("重试").assertIsDisplayed()
    }

    /**
     * 测试错误重试功能
     */
    @Test
    fun testErrorRetryFunctionality() {
        val errorMessage = "网络连接失败"
        `when`(mockViewModel.errorMessage).thenReturn(MutableStateFlow(errorMessage))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击重试按钮
        composeTestRule.onNodeWithText("重试").performClick()

        // 验证重试方法被调用
        verify(mockViewModel).retryLoading()
    }

    /**
     * 测试路径导航显示
     */
    @Test
    fun testPathNavigationDisplay() {
        val currentPath = "/Movies/Action/"
        `when`(mockViewModel.currentPath).thenReturn(MutableStateFlow(currentPath))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证路径显示
        composeTestRule.onNodeWithText("Movies").assertIsDisplayed()
        composeTestRule.onNodeWithText("Action").assertIsDisplayed()
    }

    /**
     * 测试路径导航点击
     */
    @Test
    fun testPathNavigationClick() {
        val currentPath = "/Movies/Action/"
        `when`(mockViewModel.currentPath).thenReturn(MutableStateFlow(currentPath))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击路径中的"Movies"
        composeTestRule.onNodeWithText("Movies").performClick()

        // 验证导航方法被调用
        verify(mockViewModel).navigateToPath("/Movies/")
    }

    /**
     * 测试搜索功能
     */
    @Test
    fun testSearchFunctionality() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击搜索图标
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // 验证搜索框显示
        composeTestRule.onNodeWithTag("search_text_field").assertIsDisplayed()

        // 输入搜索关键词
        composeTestRule.onNodeWithTag("search_text_field")
            .performTextInput("video")

        composeTestRule.waitForIdle()

        // 验证搜索方法被调用
        verify(mockViewModel).searchFiles("video")
    }

    /**
     * 测试文件类型图标显示
     */
    @Test
    fun testFileTypeIconDisplay() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证不同文件类型的图标
        composeTestRule.onNodeWithTag("file_icon_video").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_icon_folder").assertIsDisplayed()
    }

    /**
     * 测试长按文件显示菜单
     */
    @Test
    fun testFileLongPressMenu() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 长按文件
        composeTestRule.onNodeWithText("Video1.mp4")
            .performTouchInput {
                longClick()
            }

        composeTestRule.waitForIdle()

        // 验证菜单项显示
        composeTestRule.onNodeWithText("添加到播放列表").assertIsDisplayed()
        composeTestRule.onNodeWithText("文件信息").assertIsDisplayed()
    }

    /**
     * 测试文件菜单操作
     */
    @Test
    fun testFileMenuActions() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 长按文件显示菜单
        composeTestRule.onNodeWithText("Video1.mp4")
            .performTouchInput {
                longClick()
            }

        composeTestRule.waitForIdle()

        // 点击"添加到播放列表"
        composeTestRule.onNodeWithText("添加到播放列表").performClick()

        // 验证相应方法被调用
        verify(mockViewModel).addToPlaylist(testFiles[0])
    }

    /**
     * 测试排序功能
     */
    @Test
    fun testSortingFunctionality() {
        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 点击排序按钮
        composeTestRule.onNodeWithTag("sort_button").performClick()

        // 验证排序选项显示
        composeTestRule.onNodeWithText("按名称排序").assertIsDisplayed()
        composeTestRule.onNodeWithText("按大小排序").assertIsDisplayed()
        composeTestRule.onNodeWithText("按修改时间排序").assertIsDisplayed()

        // 选择排序方式
        composeTestRule.onNodeWithText("按大小排序").performClick()

        // 验证排序方法被调用
        verify(mockViewModel).sortFiles(MediaLibraryViewModel.SortOrder.SIZE_DESC)
    }

    /**
     * 测试滚动位置保持
     */
    @Test
    fun testScrollPositionMaintained() {
        val manyFiles = (1..50).map { i ->
            MediaFile(
                name = "Video$i.mp4",
                path = "/path/to/Video$i.mp4",
                isDirectory = false,
                size = 1024L * i,
                lastModified = System.currentTimeMillis(),
                mimeType = "video/mp4"
            )
        }

        `when`(mockViewModel.files).thenReturn(MutableStateFlow(manyFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 滚动到中间位置
        composeTestRule.onNodeWithTag("file_list")
            .performScrollToIndex(25)

        // 验证滚动到的项目可见
        composeTestRule.onNodeWithText("Video26.mp4").assertIsDisplayed()

        // 模拟配置变更
        composeTestRule.activity?.recreate()
        composeTestRule.waitForIdle()

        // 验证滚动位置大致保持（可能会有轻微偏差）
        composeTestRule.onNodeWithText("Video26.mp4").assertIsDisplayed()
    }

    /**
     * 测试深色主题适配
     */
    @Test
    fun testDarkThemeAdaptation() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))

        composeTestRule.setContent {
            NASOnlyTheme(darkTheme = true) {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证在深色主题下UI正常显示
        composeTestRule.onNodeWithText("Video1.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_list").assertIsDisplayed()
    }

    /**
     * 测试无障碍功能
     */
    @Test
    fun testAccessibilitySupport() {
        `when`(mockViewModel.files).thenReturn(MutableStateFlow(testFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        // 验证文件项目有适当的语义信息
        composeTestRule.onNodeWithText("Video1.mp4")
            .assertHasClickAction()
            .assert(hasContentDescription())

        composeTestRule.onNodeWithText("Movies")
            .assertHasClickAction()
            .assert(hasContentDescription())
    }

    /**
     * 测试性能 - 大列表滚动流畅性
     */
    @Test
    fun testLargeListScrollPerformance() {
        val manyFiles = (1..1000).map { i ->
            MediaFile(
                name = "Video$i.mp4",
                path = "/path/to/Video$i.mp4",
                isDirectory = false,
                size = 1024L * i,
                lastModified = System.currentTimeMillis(),
                mimeType = "video/mp4"
            )
        }

        `when`(mockViewModel.files).thenReturn(MutableStateFlow(manyFiles))

        composeTestRule.setContent {
            NASOnlyTheme {
                val navController = rememberNavController()
                MediaLibraryScreen(
                    navController = navController,
                    viewModel = mockViewModel
                )
            }
        }

        val startTime = System.currentTimeMillis()

        // 快速滚动测试
        composeTestRule.onNodeWithTag("file_list")
            .performScrollToIndex(500)

        composeTestRule.waitForIdle()

        val endTime = System.currentTimeMillis()
        val scrollTime = endTime - startTime

        // 验证滚动在合理时间内完成
        assert(scrollTime < 2000) { "Large list scrolling took too long: ${scrollTime}ms" }

        // 验证滚动后UI正常
        composeTestRule.onNodeWithText("Video501.mp4").assertIsDisplayed()
    }
}