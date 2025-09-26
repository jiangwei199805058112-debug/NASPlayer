# NAS Player 测试指南

## 测试概览

本项目包含了完整的测试体系，涵盖单元测试、集成测试、UI测试和端到端测试，确保播放、文件浏览、历史记录、SMB连接等核心功能的稳定性。

## 测试类型

### 1. 单元测试 (Unit Tests)
位置：`app/src/test/java/`

#### 数据层测试
- `PlaybackHistoryDaoTest.kt` - 播放历史数据访问对象测试
- `SmbConnectionManagerTest.kt` - SMB连接管理器测试
- `SmbDataSourceTest.kt` - SMB数据源测试

#### 性能组件测试
- `MediaMetadataCacheTest.kt` - 媒体元数据缓存测试
- `PerformanceMonitorTest.kt` - 性能监控器测试

#### ViewModel层测试
- `MediaLibraryViewModelTest.kt` - 媒体库视图模型测试
- `SettingsViewModelTest.kt` - 设置视图模型测试
- `PlaylistViewModelTest.kt` - 播放列表视图模型测试

### 2. UI测试 (Instrumented Tests)
位置：`app/src/androidTest/java/`

#### 屏幕测试
- `MainScreenTest.kt` - 主屏幕UI测试
- `MediaLibraryScreenTest.kt` - 媒体库屏幕测试
- `SettingsScreenTest.kt` - 设置屏幕测试

#### 集成测试
- `EndToEndIntegrationTest.kt` - 端到端集成测试

#### 性能测试
- `PerformanceBenchmark.kt` - 性能基准测试

## 运行测试

### 本地运行

#### 运行所有单元测试
```bash
./gradlew testDebugUnitTest
```

#### 运行特定单元测试
```bash
./gradlew testDebugUnitTest --tests="*PlaybackHistoryDaoTest*"
```

#### 运行所有UI测试
```bash
./gradlew connectedDebugAndroidTest
```

#### 运行特定UI测试
```bash
./gradlew connectedDebugAndroidTest --tests="*MainScreenTest*"
```

#### 生成测试覆盖率报告
```bash
./gradlew jacocoTestReport
```

#### 运行代码质量检查
```bash
./gradlew lintDebug
./gradlew detekt
./gradlew spotbugsDebug
```

### CI/CD运行

测试将在以下情况自动运行：
- 推送到 `main` 或 `develop` 分支
- 创建针对 `main` 分支的Pull Request

CI/CD流水线包括：
1. **测试阶段** - 运行单元测试和UI测试
2. **安全扫描** - SpotBugs和依赖漏洞检查
3. **代码质量** - SonarCloud分析和Detekt检查
4. **性能测试** - 基准测试
5. **构建发布** - 生成签名APK

## 测试配置

### 测试覆盖率
- 目标覆盖率：80%以上
- 覆盖率报告：`app/build/reports/jacoco/jacocoTestReport/`

### 代码质量工具

#### Detekt
- 配置文件：`config/detekt/detekt.yml`
- Kotlin代码静态分析
- 代码风格和潜在问题检查

#### SpotBugs  
- 配置文件：`config/spotbugs/exclude.xml`
- Java/Kotlin字节码分析
- 安全漏洞和bug检测

#### 依赖漏洞检查
- 配置文件：`config/dependency-check/suppressions.xml`
- 第三方依赖安全扫描
- CVE数据库匹配

## 测试最佳实践

### 单元测试
1. **使用Mock对象** - 隔离被测试组件
2. **测试边界条件** - 包含空值、极值、异常情况
3. **验证状态和行为** - 检查返回值和方法调用
4. **命名清晰** - 测试方法名应描述测试场景

### UI测试
1. **使用测试标签** - 为UI组件添加testTag
2. **等待异步操作** - 使用waitForIdle()等待操作完成
3. **模拟用户操作** - 点击、滑动、输入等真实交互
4. **验证UI状态** - 检查显示内容和组件状态

### 集成测试
1. **测试完整流程** - 从开始到结束的用户场景
2. **模拟真实环境** - 尽可能接近生产环境
3. **错误处理验证** - 测试各种异常情况
4. **性能验证** - 确保操作在合理时间内完成

## 测试数据管理

### 测试用例数据
- 使用工厂方法创建测试数据
- 避免硬编码，使用常量或配置
- 测试数据应具有代表性

### Mock数据
- SMB连接使用模拟服务器响应
- 文件列表使用预定义测试数据
- 播放历史使用时间戳变化数据

## 持续改进

### 测试维护
1. **定期更新** - 随着功能更新同步测试
2. **移除过时测试** - 清理不再相关的测试用例
3. **重构测试代码** - 保持测试代码的可读性
4. **监控测试性能** - 避免测试运行时间过长

### 覆盖率改进
1. **识别未覆盖代码** - 查看覆盖率报告
2. **添加缺失测试** - 为关键路径补充测试
3. **评估覆盖率质量** - 不只追求数量，更要质量

## 故障排除

### 常见问题

#### 测试环境问题
- **AVD启动失败** - 检查虚拟设备配置
- **权限问题** - 确保测试应用有必要权限
- **网络连接** - UI测试可能需要网络访问

#### 测试失败处理
- **查看详细日志** - 使用 `--info` 或 `--debug` 参数
- **检查测试报告** - 查看HTML格式的测试报告
- **验证测试环境** - 确保测试设备满足要求

#### 性能测试问题
- **测试设备性能** - 使用一致的测试环境
- **基准测试稳定性** - 多次运行取平均值
- **资源清理** - 确保测试后清理资源

## 报告位置

测试完成后，可以在以下位置查看报告：

- **单元测试报告**：`app/build/reports/tests/testDebugUnitTest/index.html`
- **UI测试报告**：`app/build/reports/androidTests/connected/index.html`
- **覆盖率报告**：`app/build/reports/jacoco/jacocoTestReport/html/index.html`
- **Lint报告**：`app/build/reports/lint-results-debug.html`
- **Detekt报告**：`app/build/reports/detekt/detekt.html`
- **SpotBugs报告**：`app/build/reports/spotbugs/spotbugs.html`
- **依赖检查报告**：`app/build/reports/dependency-check-report.html`

## 联系方式

如有测试相关问题，请：
1. 查看本文档的故障排除部分
2. 检查GitHub Issues中的已知问题
3. 创建新的Issue描述问题和环境信息