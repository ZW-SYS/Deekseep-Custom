# Deekseep · 2.7.1 移植二创版

> 面向官方 DeepSeek Android App **2.7.1（code287，大陆版）** 的 LSPosed/Xposed 模块。
> 本项目是基于原作者 **Lucian** 的开源项目 [lllucccian/Deekseep](https://github.com/lllucccian/Deekseep) 的**二次创作版本**，由 **张云安（ZW-SYS）** 移植与二开。本项目不是 DeepSeek 官方项目。

> [!WARNING]
> **签名冲突：安装前必须先卸载旧版。**
> 本版本使用独立签名（发布的是未签名 APK，由使用者自行签名），与原作者发布的签名版本**不同**。
> 如果你装过原作者签名的 Deekseep，或任何其他签名的同包名版本，**请先卸载**再安装，否则会提示“签名不一致 / 安装失败”。

---

## 许可与致谢

- 本项目遵循 **GNU GPL-3.0**，完整条款见 [LICENSE](LICENSE)。二创版本同样以 GPL-3.0 开源，**免费，不收费**。
- 原作者：**Lucian**（[lllucccian/Deekseep](https://github.com/lllucccian/Deekseep)）。原作者的署名、版权声明与 LICENSE 均原样保留；原版 README 及其他原版说明文档存档于 [docs/upstream/](docs/upstream/)。
- 二开：**张云安（ZW-SYS）**。
- 第三方组件声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 核心特性

**会话与界面**
- 提示词库
- 会话置顶
- 原生侧栏多选：批量选择、删除、置顶
- 消息 / 会话导出与搜索，支持导出单条消息
- 字体大小调节
- 主题色、首页欢迎语、欢迎页鲸鱼动画
- 思考链复制
- 文件类型支持、会话刷新与历史记录
- 模型文件卡片

**Agent**
- 工具状态样式
- 剪贴板工具（**默认关闭**）
- **半自动模式（默认）**：每一步工具结果都会弹窗预览，点“发送”才会发回 DeepSeek；可选“复制并稍后”或“稍后”，未发送的结果保留在 Agent 运行记录里，随时重试。
- **全自动回传（默认关闭，高风险）**：在 Agent 设置中开启，须阅读风险提示并勾选同意。已有用户反馈：DeepSeek 2.4.1 及以后，全自动使用 Agent 后账号很快被强制下线、要求重新验证甚至封禁数天（原版模块同样如此）。本模块不伪装、不规避任何检测，重要账号请勿开启。
- **Agent 限速（仅全自动模式）**：两步之间至少间隔 8 秒、每分钟最多 5 步、每小时最多 60 步；超出部分排队执行、不丢弃。
  原因：Agent 的每一步都是用你的账号发出的真实 DeepSeek 消息，连续高速调用可能触发账号限制。限速不伪装身份、不规避任何检测。

**门槛版防撤回**
- 入口默认为灰色。开启时会弹出安全警告，必须勾选“已完整阅读并理解风险”后才能启用。
- 警告内容：该功能会保留被 DeepSeek 内容安全机制撤回的回答，属于绕过官方安全措施；可能违反 DeepSeek 用户协议并导致账号受限；本模块**不提供任何防封措施**；2.7.1 上仍在适配，可能不生效。

**其他**
- 公告、彩蛋（明码展示）、原作者致谢
- “数据用于优化体验”开关
- 更新弹窗拦截
- 隐藏指定设置项；已移除捐赠入口

## 关闭或未移植的功能

**出于安全与合规底线，物理关闭（即使加同意弹窗也不做）：**
- 密码登录捕获
- 风控 SDK 中和、防封、Android ID / OAID 设备标识伪装
- 登录 / 地区解锁
- 给 Agent 做伪装以躲避检测
- 网络抓包、PoW、图像凭证等安全或付费机制绕过

**官方已自然淘汰：**
- **专家模式 / 额度解锁**：DeepSeek 2.7.1 已把识图、快速、深度专家三个模式合并为一个，宿主中不再存在单独的专家模式与额度，因此无需也无法移植。

**未移植：**
- 远程功能开关（以及依赖它的附件引导提示）
- 心跳 JSON Patch 分发器：2.7.1 中已被合并重构，内容过滤钩子已覆盖主要场景

## 构建

- 包名：`com.dsmod.probe`
- versionCode：`36`，versionName：`1.8.0`
- 目标宿主：DeepSeek 2.7.1 / code287（大陆版）

**方式一：手工脚本（模块 APK）**

```bash
cd module-universal
bash build.sh
```

**方式二：Gradle（Open 版 debug 包）**

```bash
cd compose-launcher
gradle -PprotectedBuild=false -PlocalApiIncluded=false :app:assembleDebug
```

发布的 APK 为**未签名 + zipalign** 版本。使用者需自行签名，例如：

```bash
apksigner sign --ks 你的.jks --out Deekseep-signed.apk Deekseep-1.8.0-unsigned-aligned.apk
```

## 测试状态

钩子映射已在 JVM 沙盒中校验（2.4.1 → 2.7.1 符号表），并在免 root 虚拟机（FPA）上做过真机测试，**尚未覆盖全部机型**。已知问题见 [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)。遇到问题欢迎提 Issue，并附上 LSPosed 日志。

## 文档

- [安装](docs/INSTALLATION.md) · [构建](docs/BUILDING.md) · [功能范围](docs/FEATURES.md) · [常见问题](docs/TROUBLESHOOTING.md)
- [更新日志](CHANGELOG.md) · [参与贡献](CONTRIBUTING.md) · [安全策略](SECURITY.md) · [第三方声明](THIRD_PARTY_NOTICES.md)
- [移植笔记](PORT_NOTES.md) · [原版文档存档](docs/upstream/)

## 免责声明

本项目仅供学习与研究。使用本模块修改官方 App 行为可能违反 DeepSeek 用户协议，由此产生的账号或其他后果由使用者自行承担。详见 [DISCLAIMER.md](DISCLAIMER.md)。
