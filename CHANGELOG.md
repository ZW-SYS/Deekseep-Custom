# Deekseep Changelog

> 1.8.0-port 起为张云安（ZW-SYS）的 2.7.1 移植版；1.7.5 及更早的条目为原作者 Lucian 的原版记录，原样保留。


## YunSeek 1.8.1（versionCode 37）

- 改名 YunSeek（包名不变）；一个 APK 支持 DeepSeek 国内 2.7.1(287) / 国内 2.7.2(289) / 国际 2.7.2(290)
- 桌面 App 重做：修复图标点不开，M3 + Monet，激活卡片 + 功能分类，动画，数据中心（ZIP/JSON 导入导出）
- 模块内界面 M3 化；删除交流群、原作者留言、卡密验证、自动更新检查
- 新增：本地提示词库、模型显示名（闪电/深思）、会话置顶、单条导出、快捷复制、隐藏对话、应用锁、后台防窥、防截屏、纯黑模式、渐变壁纸、自定义字体、液态玻璃输入框
- Agent 保留半自动与限速；剪贴板工具改为默认关闭
- 安全底线功能入口删除、代码硬关闭

## [1.8.0-port, device-test fixes] - 2026-10-28

### Agent
- **Semi-auto delivery (default)**: every tool result / screenshot / text attachment waits for
  the user to tap Send on a confirmation card (Send / Copy & later / Later). Held results stay
  in Agent runs and can be retried.
- **Full-auto delivery** is off by default and can only be enabled after a risk notice and an
  explicit acknowledgement checkbox. The rate limiter (8 s gap, 5/min, 60/h) applies to it.
- Nothing disguises or hides Agent requests; the server receives the same message either way.

### Fixes
- Blank settings hub on 2.7.1 (category card re-attached).
- Agent tool results not delivered on 2.7.1 (send bridge is now 7-arg `oj2.k0`).
- Missing launcher icon in compose-launcher.
- Image import (wallpaper / avatar / stickers): `openInputStream` → `openFileDescriptor` →
  file-path fallbacks, detailed error text, and picker rotation after a read failure.

### Known issues
- Shizuku may report "connection failed" even when authorised.
- Some rootless virtual environments (e.g. FPA) return `ENOENT` for gallery images.

## [1.8.0-port] - 2026-10-25

Community port of the GPL-3.0 Open edition to the latest mainland DeepSeek build. Not an
official release by the original author (lllucccian); original copyright, license and
attribution are unchanged.

### Host Version Compatibility
- **Mainland DeepSeek 2.7.1 (versionCode 287)** added as a new, deliberately limited
  generation (`v271`). Detection: `sx1` = ChatFullCompletionRequest (11-argument constructor,
  `du1` marker interface), no `com.pairip`.
- Ported hooks (symbols re-located in 2.7.1 by serialization names / analytics strings /
  exact signatures): module entry on the settings page (`gt0.Q` page_show "settings"),
  custom system-prompt injection (`sx1` constructor), and removal of the injected prompt
  wrapper from synced history (`ey1` / `gy.v` / REQUEST fragment).
- Every other host-internal feature is **not installed** on 2.7.1. Unmapped logical symbols
  resolve to an unloadable sentinel so stale hooks fail closed instead of hooking unrelated
  classes. Older generations (2.2.x–2.4.1, GP 2.5.0) keep their existing paths unchanged.
- Risk-control (SMSDK/Shumei) bypass, anti-ban heuristics, local quota unlock, region/login
  unlocks and remote feature-flag overrides are intentionally **not** ported to 2.7.1.

### New Features
- **Conversation tools**: pick any locally stored conversation (title filter) and copy the
  whole conversation, export it as Markdown or plain text, share it via the Android share
  sheet, or search inside it (tap a hit to copy that message). Read-only, host-independent.
- **Text size**: DeepSeek-only text scale (80%–160%) applied through a framework
  configuration override on MainActivity; works on every host generation.

### Removed
- All sponsorship/donation entries: 爱发电/Afdian links, WeChat donation QR code image and
  dialogs (in-host hub, module settings activity, Compose launcher), and README/DISCLAIMER
  sponsor lines.

### Other
- New launcher icon (adaptive icon + legacy mipmaps, mdpi–xxxhdpi); generator script in
  `assets/icon/make_icon.py`.
- New regression test `module/test-port-v271.sh`.
- Version 1.8.0-port (versionCode 36).

---

## [1.7.5] - 2026-09-20

### Critical Announcement & Distribution Changes
- **Indefinite Suspension of Updates**: Due to severe abuse, unauthorized code reselling, and egregious GPL license violations by copycat projects, all public repository updates are temporarily suspended indefinitely.
- **Closed Edition Exclusivity**: The Closed edition is no longer provided, updated, or distributed through GitHub releases or repository commits. Users can join the official Telegram group ([@Deekseepapp](https://t.me/Deekseepapp)) to obtain Closed edition builds.

---

### What's New in Version 1.7.5

#### 1. Host Version Compatibility
- **DeepSeek 2.4.1 Adaptation**: Full compatibility branch implemented for Mainland China DeepSeek v2.4.1 (versionCode 257).

#### 2. Chat & Message Management
- **Batch Chat Deletion**: Multi-select mode in conversation history now supports one-tap batch deletion of selected sessions.
- **Local Quota Unlock**: Removed local chat count edit limits (bypassed the 5-turn session modification constraint).
- **Thinking Chain Code Copy**: Added a one-click copy button for reasoning/thinking code blocks to easily copy code outputs.
- **Anti-Recall Reliability & Context Preservation**: Fixed edge-case failures where recalled messages could disappear, and preserved conversation context for revoked/retracted messages.

#### 3. Security & Anti-Risk Bypass
- **Risk Control SDK Bypass**: Added runtime inline bypass for SMSDK / Shumei device risk-control detection routines.
- **Anti-Ban Protection**: Implemented defensive heuristics and randomized behaviors to reduce account risk and mitigate automatic bans.

#### 4. UI Polish & Navigation
- **Theme Color Extraction**: Dynamic host theme color palette synchronization with custom HEX accent overrides.
- **Entry Setting Renamed**: "使用原生入口" (Use Native Entry) renamed to "使用旧版入口" (Use Legacy Entry) for clarity.
- **UI Navigation Manager**: Integrated Activity and Compose route manager to navigate hidden host pages and toggle route visibility.

#### 5. Diagnostic Tools & System Logs
- **High-Frequency Raw Logging**: Added high-throughput, unmasked real-time hook trace logging with automatic multi-volume rotation.
- **Export Diagnostic Logs**: One-click export of system diagnostic logs, crash dumps, and runtime traces into a `.zip` archive.
- **Application Data Backup**: Added support for backing up and restoring user application data.

#### 6. Architecture & Framework Extensions
- **Java Plugin Framework v3**: Robust runtime Java plugin system featuring dedicated hook catalogs, ABI contracts, and lifecycle management.
- **Remote Feature Flags Extension**: Expanded runtime remote feature toggles and gray-release configuration overrides.

#### 7. Agent & MCP Integration
- **Built-in Termux Integration**: Integrated Termux runtime environment support for Agent execution (*Note: Compatibility issues may exist depending on device architecture and environment*).
- **Agent Model Context Protocol (MCP)**: Added MCP tool protocol support with streaming HTTP/SSE server connectivity and automated capability discovery.
- **Multi-Round Segmented Prompt Injection**: Split prompt injection logic into coordinated multi-turn phases for complex instruction flows.

#### 8. General Fixes
- Addressed numerous edge-case bugs, memory leaks, and UI rendering inconsistencies across various Android versions. Please explore the release for further improvements.
