# 从源码构建（YunSeek 1.8.1.1）

本版本通过 `compose-launcher` 的 Gradle 工程构建，产物为 Open 版（不含 Local API、隧道和受保护构建载荷）。原版构建说明存档于 [upstream/BUILDING_upstream.md](upstream/BUILDING_upstream.md)。

## 环境

- JDK 17
- Gradle 9.x（作者使用 9.8.2）
- Android SDK Platform 35 或更新，Build Tools 35.0.0（`aapt2`、`zipalign`、`apksigner`）
- 设置 `ANDROID_SDK_ROOT` 或 `ANDROID_HOME`

> `compose-launcher/gradle.properties` 中的 `android.aapt2FromMavenOverride` 指向 Termux 路径（原作者在手机上构建）。电脑上请删除这一行，或在命令行用 `-Pandroid.aapt2FromMavenOverride=<你的 build-tools>/aapt2` 覆盖。

## 构建

一键构建（输出 `out/YunSeek-1.8.1.1-unsigned-aligned.apk` 和 `out/SHA256.txt`，结束时确认 APK 不带任何签名）：

```bash
bash scripts/build-all.sh
```

或手动：

```bash
cd compose-launcher
gradle --no-daemon -PprotectedBuild=false -PlocalApiIncluded=false :app:assembleDebug
```

APK 输出在 `compose-launcher/app/build/outputs/apk/debug/`。

## 对齐与签名

发布版是**未签名 + zipalign** 的 APK，由使用者自己签名：

```bash
zipalign -f -p 4 in.apk aligned.apk
apksigner sign --ks 你的.jks --out YunSeek-1.8.1.1-signed.apk aligned.apk
apksigner verify YunSeek-1.8.1.1-signed.apk
```

请使用你自己的签名密钥，不要把密钥提交到仓库。

## 其他说明

- `scripts/gradle-open-debug.sh` 是作者沙盒环境用的一键脚本（依赖固定路径），仅供参考。
- `scripts/build-all-legacy.sh` 与 `module-universal/build.sh` 为原版构建脚本，用于旧宿主版本。
- 宿主映射表（287 / 289 / 290）在 `module/hostmap/`，重新生成用 `bash host/map/multi.sh <versionCode> <宿主APK>`，说明见 [PORT_NOTES.md](../PORT_NOTES.md)。
- 桌面 App 新用到的 Material 扩展图标，需要加进 `compose-launcher/app/build.gradle.kts` 的 `trimMaterialIcons` 白名单，否则会被裁掉。
