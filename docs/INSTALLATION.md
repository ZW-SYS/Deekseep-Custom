# 安装

> [!CAUTION]
> **装过原作者签名版或其他签名版本的，必须先卸载**，否则会提示签名不一致、安装失败。

1. 安装 LSPosed / LSPatch 等兼容 Xposed 的框架（免 root 可用 FPA 等虚拟机环境，部分功能可能受限）。
2. 从本仓库 Release 下载 `YunSeek-1.8.1.1-unsigned-aligned.apk`，用你自己的密钥签名（见 [BUILDING.md](BUILDING.md#对齐与签名)）。
3. 安装后在框架中启用 YunSeek，作用域只勾选 `com.deepseek.chat`。
4. 强制停止并重新打开 DeepSeek。在 DeepSeek 设置页可以找到模块入口。

## 支持的宿主

- **国内版 DeepSeek 2.7.1（code 287）**、**国内版 2.7.2（code 289）**、**国际 / Google Play 版 2.7.2（code 290）**：一个 APK 自动识别。
- 2.7.3：尚未适配，模块会自动停用宿主相关功能。
- 2.2.x–2.4.1、GP 2.5.0：沿用原版代码，未重新测试。

## 建议

- 重要对话先备份。
- Agent 保持默认半自动模式；重要账号不要开启全自动回传。
- 遇到问题先看 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)。
