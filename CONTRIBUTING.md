# 参与贡献

欢迎提交 Issue 和 Pull Request。本仓库是 Deekseep 的 **DeepSeek 2.7.1 移植二创版**，由张云安（ZW-SYS）维护；原项目见 [lllucccian/Deekseep](https://github.com/lllucccian/Deekseep)，原作者的贡献指南存档于 [docs/upstream/CONTRIBUTING_upstream.md](docs/upstream/CONTRIBUTING_upstream.md)。

> 涉及原版（2.2.x–2.4.1）逻辑本身的问题，建议同时反馈给原作者。

## 提交前

1. 先读 [免责声明](DISCLAIMER.md) 和 [构建说明](docs/BUILDING.md)。
2. 搜一下现有 Issue，避免重复。
3. 写清楚：DeepSeek 版本与渠道（如 2.7.1 / code287 大陆版）、Android 版本、Xposed 框架（LSPosed / LSPatch / 免 root 虚拟机如 FPA）。
4. 改动只针对一个功能，不要顺手大面积重构。

## Pull Request 要求

- 说明现象和根因。
- 写明在哪个宿主版本、哪台设备上验证过。
- 可选 Hook 必须“失败即放行”（fail-open），找不到符号时不能影响 DeepSeek 正常使用。
- 行为有变化时同步更新 README、CHANGELOG 和 docs。
- 不要提交签名密钥、APK、聊天数据库、完整日志或任何个人数据。

## 不接受的内容

以下功能本移植版**不会合并**，请不要提交：

- 额度 / 专家模式解锁
- 风控 SDK 中和、防封、规避检测
- Android ID / OAID 等设备标识伪装
- 登录、地区限制解锁
- 让 Agent 伪装成人工操作以躲避检测

## 源码边界

不要提交：

- DeepSeek 官方 APK；
- DeepSeek 的反编译源码或逆向生成物（`module/hostmap/` 里的符号映射表除外，它只含类名、方法名对照）；
- 官方资源文件；
- 账号凭据、Token、签名 URL、设备标识。
