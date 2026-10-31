# 安全策略

## 支持范围

安全修复只针对本仓库的最新源码和最新 Release（当前为 1.8.0，适配 DeepSeek 2.7.1）。旧版模块、他人二次打包或自行改签的版本不在支持范围内。

原作者的安全策略存档于 [docs/upstream/SECURITY_upstream.md](docs/upstream/SECURITY_upstream.md)。原版自身的漏洞请同时报告给原作者。

## 报告漏洞

可能泄露会话数据、任意文件、凭据或造成不安全跨进程访问的问题，**请不要发公开 Issue**，请使用本仓库 Security 页面的 “Report a vulnerability”（私密安全通告）。

请附上：

- 模块版本与 commit；
- Android、Xposed 框架、DeepSeek 版本；
- 最小复现步骤；
- 影响范围与所需前提；
- 脱敏后的报错堆栈；
- 可选的修复建议。

不要附带真实聊天数据库、完整日志、签名密钥或 DeepSeek APK。

## 安全模型

模块运行在 DeepSeek 进程内，启用后拥有与 DeepSeek 相同的数据可见范围。因此本项目的原则是：

- 不在用户不知情时导出 DeepSeek 私有数据；
- 不把凭据或敏感运行时数据写进仓库；
- 诊断与高风险功能默认关闭，开启前明确告知；
- Agent 默认半自动，工具结果发出前由用户确认；
- 剪贴板等敏感工具默认关闭；
- 可选 Hook 失败即放行。

本项目无法保护已 root 或已被攻破的设备。
