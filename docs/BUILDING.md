# 从源码构建（1.8.0 移植版）

本版本通过 `compose-launcher` 的 Gradle 工程构建，产物为 Open 版（不含 Local API、隧道和受保护构建载荷）。原版构建说明存档于 [upstream/BUILDING_upstream.md](upstream/BUILDING_upstream.md)。

## 环境

- JDK 17
- Gradle 9.x（作者使用 9.8.2）
- Android SDK Platform 35 或更新，Build Tools 35.0.0（`aapt2`、`zipalign`、`apksigner`）
- 设置 `ANDROID_SDK_ROOT` 或 `ANDROID_HOME`

> `compose-launcher/gradle.properties` 中的 `android.aapt2FromMavenOverride` 指向 Termux 路径（原作者在手机上构建）。电脑上请删除这一行，或在命令行用 `-Pandroid.aapt2FromMavenOverride=<你的 build-tools>/aapt2` 覆盖。

## 构建

```bash
cd compose-launcher
gradle --no-daemon -PprotectedBuild=false -PlocalApiIncluded=false :app:assembleDebug
```

APK 输出在 `compose-launcher/app/build/outputs/apk/debug/`。

## 对齐与签名

发布版是**未签名 + zipalign** 的 APK，由使用者自己签名：

```bash
zipalign -f -p 4 in.apk aligned.apk
apksigner sign --ks 你的.jks --out Deekseep-1.8.0-signed.apk aligned.apk
apksigner verify Deekseep-1.8.0-signed.apk
```

请使用你自己的签名密钥，不要把密钥提交到仓库。

## 其他说明

- `scripts/gradle-open-debug.sh` 是作者沙盒环境用的一键脚本（依赖固定路径），仅供参考。
- `scripts/build-all.sh` 与 `module-universal/build.sh` 为原版构建脚本，用于旧宿主版本。
- 2.7.1 的符号映射在 `module/hostmap/`，说明见 [PORT_NOTES.md](../PORT_NOTES.md)。
