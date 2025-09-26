package com.example.nasonly.ui.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 沉浸式全屏管理器
 * 用于播放器页面的全屏沉浸式显示
 */
object ImmersiveFullscreenManager {
    
    /**
     * 进入全屏沉浸模式
     * - 隐藏状态栏和导航栏
     * - 设置沉浸式体验
     */
    fun enterFullscreen(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
    
    /**
     * 退出全屏模式
     * - 显示状态栏和导航栏
     * - 恢复正常显示
     */
    fun exitFullscreen(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
    
    /**
     * 切换全屏模式
     */
    fun toggleFullscreen(activity: Activity, isFullscreen: Boolean) {
        if (isFullscreen) {
            enterFullscreen(activity)
        } else {
            exitFullscreen(activity)
        }
    }
}