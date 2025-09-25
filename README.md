# NASPlayer

[![Android CI](https://github.com/jiangwei199805058112-debug/NASPlayer/actions/workflows/android-ci.yml/badge.svg)](https://github.com/jiangwei199805058112-debug/NASPlayer/actions/workflows/android-ci.yml)

Android NAS 播放器应用，支持 SMB 协议播放 NAS 设备上的视频文件。

## 🎯 主要功能

### ✅ 已实现功能

- **SMB 连接管理**：支持连接到 SMB/NAS 设备
- **视频播放**：使用 ExoPlayer 支持多种视频格式
- **文件浏览**：浏览 NAS 设备上的媒体文件
- **播放历史**：自动记录播放历史和播放进度
  - 双标签页界面（媒体文件 / 播放历史）
  - 自动保存播放进度（每10秒）
  - 播放历史列表显示
  - 支持删除单个历史记录或清除全部
- **进度恢复**：从上次播放位置继续播放
- **手势支持**：播放器手势控制

### 🚧 开发中功能

- **播放列表管理**：创建、编辑、删除播放列表
- **设置界面增强**：播放器设置和系统设置
- **视频列表优化**：缩略图预览、文件信息、排序过滤

## 🏗️ 技术架构

### 核心技术栈

- **Kotlin** + **Jetpack Compose**：现代化 Android 开发
- **Material 3**：Material Design 3.0 设计系统
- **Hilt**：依赖注入框架
- **Room**：本地数据库存储
- **ExoPlayer**：专业视频播放引擎
- **JCIFS-NG**：SMB 协议支持
- **Coroutines**：异步编程

### 架构设计

- **MVVM 架构**：清晰的数据流和状态管理
- **Repository 模式**：数据访问层抽象
- **组件化设计**：可复用的 UI 组件
- **响应式编程**：基于 Flow 的数据流

## 📱 界面预览

### 主界面
- **媒体文件标签**：显示 NAS 设备上的视频文件列表
- **播放历史标签**：显示最近播放的视频和播放进度

### 播放器界面
- **全屏播放**：沉浸式播放体验
- **手势控制**：点击显示控制面板，手势调节音量和亮度
- **自动保存**：播放进度自动保存到历史记录

## 🚀 开始使用
