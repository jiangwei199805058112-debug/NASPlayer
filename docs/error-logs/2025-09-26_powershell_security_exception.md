# 构建错误报告 - PowerShell Security Exception

## 📋 错误概览

- **报错时间**: 2025-09-26
- **报错类型**: PowerShell 安全策略限制
- **严重程度**: 高 (阻止 GitHub Actions 执行)
- **影响范围**: 自托管 Windows Runner 上的所有 PowerShell 脚本执行

## 🔧 环境信息

- **操作系统**: Windows (自托管 Runner)
- **PowerShell版本**: PowerShell 5.x/7.x
- **GitHub Actions Runner**: 自托管 Windows Runner
- **执行策略**: Restricted/AllSigned (过于严格)

## ⚠️ 报错摘要

```
PSSecurityException: UnauthorizedAccess
PowerShell execution policy prevents script execution
CategoryInfo: SecurityError: (:) [], PSSecurityException
FullyQualifiedErrorId: UnauthorizedAccess
```

## 📝 完整错误堆栈

```
+ . 'C:\actions-runner\_work\_temp\<script-id>.ps1'
+   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : SecurityError: (:) [], PSSecurityException
    + FullyQualifiedErrorId : UnauthorizedAccess

The execution of scripts is disabled on this system. 
For more information, see about_Execution_Policies at https:/go.microsoft.com/fwlink/?LinkID=135170.
```

## 🔍 错误分析

### 根本原因
1. **PowerShell 执行策略过严**: Windows 系统默认的 PowerShell 执行策略设置为 `Restricted` 或 `AllSigned`
2. **GitHub Actions 临时脚本**: GitHub Actions 会创建临时 PowerShell 脚本文件来执行工作流步骤
3. **安全策略冲突**: 临时脚本无法通过严格的执行策略验证

### 触发条件
- 在自托管 Windows Runner 上运行 GitHub Actions
- 工作流中包含使用 PowerShell 的步骤
- PowerShell 执行策略设置过于严格

### 影响范围
- 所有使用 PowerShell 的工作流步骤
- 自托管 Windows Runner 无法正常执行任务
- CI/CD 流水线完全中断

## 🛠️ 修复方案

### 1. 在工作流中设置 PowerShell 执行策略
**文件**: `.github/workflows/android-ci.yml`

在所有 PowerShell 步骤之前添加：
```yaml
- name: Set PowerShell execution policy
  shell: pwsh
  run: Set-ExecutionPolicy -ExecutionPolicy Unrestricted -Scope Process
```

### 2. 完整的工作流修复
在 `Checkout` 步骤之后立即添加执行策略设置：

```yaml
steps:
- name: Checkout
  uses: actions/checkout@v4

- name: Set PowerShell execution policy
  shell: pwsh
  run: Set-ExecutionPolicy -ExecutionPolicy Unrestricted -Scope Process

# ... 其他步骤
```

### 3. 可选：系统级永久解决方案
如果需要永久解决，可以在 Runner 主机上执行：
```powershell
# 以管理员身份运行
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope LocalMachine
```

## ✅ 修复验证

### 验证步骤
1. 修改工作流文件添加执行策略设置
2. 提交并推送更改
3. 触发 GitHub Actions 工作流
4. 检查 PowerShell 步骤是否正常执行

### 预期结果
- PowerShell 脚本正常执行，无安全异常
- GitHub Actions 工作流顺利完成
- 所有构建和测试步骤正常运行

## 📚 相关资源

- [PowerShell Execution Policies](https://docs.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies)
- [GitHub Actions PowerShell Support](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#jobsjob_idstepsshell)
- [Self-hosted Runner Configuration](https://docs.github.com/en/actions/hosting-your-own-runners/configuring-the-self-hosted-runner-application-as-a-service)

## 🔄 后续预防措施

1. **Runner 配置标准化**: 为所有自托管 Windows Runner 设置统一的 PowerShell 执行策略
2. **工作流模板**: 在所有 Windows 工作流模板中包含执行策略设置
3. **文档更新**: 更新部署文档，包含 PowerShell 配置要求
4. **监控检查**: 定期检查 Runner 的 PowerShell 配置状态

## 🎯 安全考虑

- `Unrestricted` 策略仅在进程范围内生效，不影响系统安全
- 临时脚本执行完毕后策略自动失效
- 建议在生产环境中使用 `RemoteSigned` 策略作为平衡方案

---
**状态**: 🔄 待修复  
**优先级**: 高  
**预计修复时间**: 立即