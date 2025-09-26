# 🤖 NAS Player 无人值守自动修复系统

## 📋 系统概述

本系统是一个完整的 CI/CD 自动修复解决方案，集成了 GitHub Actions 工作流和 OpenAI API，能够智能分析构建错误并自动生成修复方案。

## 🏗️ 系统架构

### 核心组件

1. **主 CI 流水线** (`.github/workflows/android-ci.yml`)
   - 执行标准 Android 构建流程
   - 构建失败时自动触发修复系统

2. **自动修复工作流** (`.github/workflows/auto-fix.yml`)
   - 智能错误修复的核心引擎
   - 支持手动触发和其他工作流调用

3. **Python 修复脚本** (`scripts/fix_errors.py`)
   - 使用 OpenAI API 进行错误分析
   - 智能生成并应用修复方案

## 🚀 功能特性

### ✨ 智能错误分析
- 自动解析 Gradle 构建输出
- 提取错误上下文和文件位置
- 分类处理不同类型的构建错误

### 🔧 自动修复机制
- 基于 OpenAI GPT-4o-mini 模型
- 生成针对性的修复方案
- 支持文件创建、修改、删除操作

### 🔄 循环验证
- 最多 3 次自动修复尝试
- 每次修复后重新构建验证
- 智能判断修复效果

### 📊 详细日志
- 完整的修复过程记录
- 错误分析和修复建议
- GitHub Actions 集成界面

## ⚙️ 配置要求

### 必需的 GitHub Secrets
```
OPENAI_API_KEY: OpenAI API 密钥
```

### 环境依赖
- Ubuntu 最新版 (GitHub Actions Runner)
- JDK 17 (Temurin 发行版)
- Python 3.10
- Android SDK

### Python 依赖包
```
openai>=1.0.0
requests>=2.25.0
pyyaml>=6.0
```

## 📝 使用方法

### 自动触发
当主分支推送代码且构建失败时，系统将自动启动修复流程：

```bash
git push origin main  # 如果构建失败，自动触发修复
```

### 手动触发
在 GitHub Actions 页面手动运行 "NASPlayer Auto Fix & Build Loop" 工作流。

### 工作流调用
其他工作流可以调用自动修复功能：

```yaml
- name: Call Auto Fix
  uses: ./.github/workflows/auto-fix.yml
  secrets: inherit
```

## 🔍 工作流程详解

### 1. 错误检测阶段
```
构建失败 → 捕获构建日志 → 解析错误信息 → 提取上下文
```

### 2. AI 分析阶段
```
构建项目上下文 → 调用 OpenAI API → 生成修复方案 → 验证方案合理性
```

### 3. 修复应用阶段
```
解析修复指令 → 应用文件更改 → 执行必要命令 → 验证修复结果
```

### 4. 验证循环
```
重新构建 → 检查构建状态 → 成功则完成，失败则重复
```

## 📋 支持的错误类型

### Kotlin/Java 编译错误
- 语法错误
- 类型不匹配
- 导入缺失
- 方法签名错误

### Android 构建错误
- 资源文件问题
- Manifest 配置错误
- 依赖冲突
- 版本兼容性问题

### Gradle 配置错误
- 插件配置
- 依赖声明
- 构建脚本语法

## 🛡️ 安全措施

### API 密钥保护
- 使用 GitHub Secrets 存储敏感信息
- 不在日志中暴露 API 密钥

### 修复范围限制
- 仅修改项目源代码文件
- 不执行危险的系统命令
- 限制文件操作权限

### 错误处理
- 修复失败时不会破坏现有代码
- 完整的错误日志记录
- 优雅的降级处理

## 📊 监控和统计

### 修复成功率
系统会在日志中记录：
- 尝试修复的错误数量
- 成功修复的错误数量
- 修复所需的平均时间

### 错误分类统计
- 按错误类型分类统计
- 常见错误模式识别
- 修复策略优化建议

## 🔧 自定义配置

### 修改最大尝试次数
在 `scripts/fix_errors.py` 中修改：
```python
self.max_attempts = 3  # 修改为所需次数
```

### 调整 AI 模型参数
```python
model="gpt-4o-mini",    # 可切换为其他模型
temperature=0.1,        # 调整创造性
max_tokens=2000        # 调整响应长度
```

### 自定义错误模式
在 `extract_error_context` 方法中添加新的错误匹配模式。

## 🚨 故障排除

### 常见问题

1. **OpenAI API 调用失败**
   - 检查 API 密钥是否正确设置
   - 确认 API 配额是否充足
   - 验证网络连接状态

2. **Git 推送失败**
   - 检查 GitHub Token 权限
   - 确认分支保护规则
   - 验证提交用户配置

3. **构建环境问题**
   - 确认 JDK 版本匹配
   - 检查 Android SDK 安装
   - 验证 Gradle 包装器

### 调试模式

启用详细日志：
```yaml
- name: Run Auto Fix
  run: python3 scripts/fix_errors.py
  env:
    PYTHONPATH: .
    LOG_LEVEL: DEBUG
```

## 📈 性能优化

### 缓存策略
- Gradle 缓存复用
- Python 依赖缓存
- Android SDK 缓存

### 并行处理
- 支持多个错误并行分析
- 异步 API 调用
- 分布式构建支持

## 🔄 版本更新

### 系统升级
1. 更新 Python 依赖版本
2. 升级 OpenAI API 版本
3. 优化错误匹配规则
4. 增强修复策略

### 向后兼容
- 保持现有配置格式
- 平滑的迁移路径
- 完整的变更日志

## 📞 技术支持

如遇到问题，请检查：
1. GitHub Actions 执行日志
2. Python 脚本输出信息
3. OpenAI API 调用结果
4. Git 操作状态

---

**注意**: 本系统需要有效的 OpenAI API 密钥才能正常工作。请确保在 GitHub 仓库设置中正确配置 `OPENAI_API_KEY` 密钥。