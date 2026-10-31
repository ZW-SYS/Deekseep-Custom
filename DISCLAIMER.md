# 免责声明

## 关于本项目

本仓库是开源项目 [Deekseep](https://github.com/lllucccian/Deekseep)（原作者 Lucian，GPL-3.0）的**移植二创版**，由张云安（ZW-SYS）适配到 DeepSeek Android 2.7.1。

- 本项目不是 DeepSeek / 深度求索、幻方、LSPosed、Xposed 的官方产品，也不由原作者 Lucian 维护或背书；移植版的问题请在本仓库反馈。
- 相关产品名称与商标归各自所有者。
- 原作者的免责声明存档于 [docs/upstream/DISCLAIMER_upstream.md](docs/upstream/DISCLAIMER_upstream.md)。

## 兼容范围

- 主要目标：**DeepSeek 2.7.1（versionCode 287，大陆版）**。
- 2.7.1 只移植了部分功能，未移植或底线关闭的功能见 [README](README.md)。
- 更旧的宿主（2.2.x–2.4.1、GP 2.5.0）沿用原版代码路径，本移植版**未重新测试**。
- DeepSeek 一旦更新，模块可能失效，需要重新适配。

## 账号风险

修改官方 App 的行为可能违反 DeepSeek 用户协议。

- 已有用户反馈：DeepSeek 2.4.1 及以后，使用 Agent（尤其是全自动连续调用）后，账号被强制下线、要求重新验证，甚至封禁数天；原版模块同样出现过。
- 本模块默认使用**半自动 Agent**（每步手动确认发送）；全自动回传默认关闭，开启前须阅读风险说明并勾选同意。
- 本模块**不提供任何防封、伪装或规避检测措施**。账号受限等后果由使用者自行承担。重要账号请谨慎使用。

## 本地数据与隐私

部分功能会读取或修改本地会话数据。重要对话请先备份。

导出的会话、日志、截图可能包含隐私内容，分享或提交 Issue 前请先脱敏。

## 无担保

本项目按 GNU GPL-3.0 许可“按原样”提供，不附带任何明示或暗示的担保。详见 [LICENSE](LICENSE)。
