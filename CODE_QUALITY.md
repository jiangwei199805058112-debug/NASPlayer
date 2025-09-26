# Code Quality Tools

本项目集成了以下代码质量检查工具：

## 🔍 工具概述

### Detekt
- **用途**: Kotlin 静态代码分析工具
- **功能**: 检测代码复杂度、潜在问题、代码异味等
- **配置文件**: `config/detekt/detekt.yml`

### ktlint
- **用途**: Kotlin 代码风格检查和格式化工具
- **功能**: 强制执行 Kotlin 官方代码风格规范
- **配置文件**: `.ktlint.conf`, `.editorconfig`

## 🚀 本地使用

### 运行代码检查
```bash
# 运行 detekt 检查
./gradlew detekt

# 运行 ktlint 检查
./gradlew ktlintCheck

# 运行所有检查
./gradlew check
```

### 修复代码风格问题
```bash
# 自动修复 ktlint 发现的格式问题
./gradlew ktlintFormat
```

### 查看报告
检查完成后，报告会生成在以下位置：
- **Detekt**: `app/build/reports/detekt/`
- **ktlint**: `app/build/reports/ktlint/`

## 🤖 CI/CD 集成

### GitHub Actions
项目配置了 GitHub Actions 工作流 (`.github/workflows/ci.yml`)，会在以下情况自动运行：
- Push 到 `main` 分支
- 创建 Pull Request 到 `main` 分支
- 手动触发

### 工作流步骤
1. **代码质量检查** (Ubuntu)
   - ✅ 运行 detekt 静态分析
   - ✅ 运行 ktlint 代码风格检查
   - ✅ 构建 debug APK 验证编译
   - 📊 上传报告文件
   - 💬 在 PR 中评论结果

2. **Android CI** (Windows self-hosted)
   - 仅在代码质量检查通过后运行
   - 执行完整的 Android 构建和测试

### PR 检查
每个 Pull Request 都会自动触发检查，确保：
- 代码符合项目风格规范
- 没有静态分析警告
- 代码能够成功编译

## ⚙️ 配置说明

### Detekt 配置
主配置文件：`config/detekt/detekt.yml`
- 基于默认配置构建
- 启用格式化规则
- 生成多种格式报告 (HTML, XML, TXT, SARIF, MD)
- 对测试文件有特殊规则

### ktlint 配置
配置文件：`.ktlint.conf` 和 `.editorconfig`
- 启用 Android 兼容模式
- 最大行长度：120 字符
- 缩进：4 个空格
- 生成多种格式报告

### IDE 集成
推荐在 IDE 中安装相应插件：
- **Android Studio/IntelliJ**: detekt 插件
- **VS Code**: Kotlin 插件 + ktlint 扩展

## 🛠️ 自定义规则

### 禁用特定规则
在 `detekt.yml` 中设置：
```yaml
rule-set:
  RuleName:
    active: false
```

在代码中使用注解：
```kotlin
@Suppress("RuleName")
class MyClass
```

### 排除文件
在配置文件的 `excludes` 部分添加路径模式：
```yaml
excludes: ['**/generated/**', '**/test/**']
```

## 📈 报告查看

### 本地报告
- HTML 报告：浏览器中打开 `app/build/reports/detekt/detekt.html`
- 控制台输出：直接在终端查看结果

### CI 报告
- GitHub Actions 会上传报告作为工件
- 可以在 Actions 页面下载查看
- PR 会收到自动评论汇总结果

## 🔧 故障排除

### 常见问题
1. **构建失败**: 检查是否有代码风格违规
2. **内存不足**: 增加 gradle.properties 中的内存设置
3. **插件版本冲突**: 确保版本目录文件 `libs.versions.toml` 中的版本一致

### 临时跳过检查
```bash
# 跳过 detekt
./gradlew build -x detekt

# 跳过 ktlint
./gradlew build -x ktlintCheck
```

## 📝 最佳实践

1. **提交前检查**: 始终在提交前运行 `./gradlew check`
2. **增量修复**: 逐步修复现有代码的问题，而不是一次性全部修复
3. **团队约定**: 团队成员应就代码风格达成一致
4. **持续改进**: 定期审查和更新规则配置

## 🔗 相关链接

- [Detekt 官方文档](https://detekt.dev/)
- [ktlint 官方文档](https://pinterest.github.io/ktlint/)
- [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)