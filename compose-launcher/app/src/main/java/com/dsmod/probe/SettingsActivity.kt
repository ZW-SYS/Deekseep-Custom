package com.dsmod.probe

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Base64
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dsmod.probe.ui.CategoryCard
import com.dsmod.probe.ui.NoteCard
import com.dsmod.probe.ui.SectionHeader
import com.dsmod.probe.ui.StatusHeroCard
import com.dsmod.probe.ui.YunSeekTheme
import com.dsmod.probe.ui.pressScale
import com.dsmod.probe.ui.staggeredEntrance
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * YunSeek standalone launcher UI (Material 3 + Monet).
 *
 * The launcher process cannot read DeepSeek's private files. Every module setting shown here is
 * read and written through [ModuleConfigBridge] — the authenticated export/import broadcast that
 * the injected DeepSeek process answers — so DeepSeek must have been opened (with the module
 * active) at least once. Written values take effect after DeepSeek is restarted.
 */
class SettingsActivity : ComponentActivity() {
    companion object {
        @JvmField val VERSION: String = BuildInfo.MODULE_VERSION
        private const val TAG = "YunSeekLauncher"
        private const val APP_NAME = "YunSeek"
        private const val TARGET_PACKAGE = "com.deepseek.chat"
        private const val REPOSITORY = "https://github.com/lllucccian/Deekseep"
        private const val BRIDGE_TIMEOUT_MS = 4000L
        private const val RESULT_TIMEOUT = -2
        private const val MAX_IMPORT_BYTES = 64 * 1024 * 1024
        private const val MAX_LAUNCHER_FILE = 4 * 1024 * 1024
        private const val ZIP_META = "yunseek-backup.json"
        private const val ZIP_SETTINGS = "settings/deekseep-config.json"
        private const val ZIP_LAUNCHER_FILES = "launcher/files/"
        private const val ZIP_LAUNCHER_PREFS = "launcher/shared_prefs/"
        private const val ROUTE_ABOUT = "about"
    }

    private enum class BridgeStatus { IDLE, LOADING, READY, UNAVAILABLE }

    private val main = Handler(Looper.getMainLooper())

    // ---- Compose-observed state owned by the activity -----------------------------------------
    private var refreshTick by mutableIntStateOf(0)
    private var bridgeStatus by mutableStateOf(BridgeStatus.IDLE)
    private var configFiles by mutableStateOf<Map<String, ByteArray>>(emptyMap())
    private var busyMessage by mutableStateOf<String?>(null)

    private var pendingExportZip = true
    private val exportZipLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) exportData(uri, zip = true) }
    private val exportJsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) exportData(uri, zip = false) }
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) importData(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every pre-UI step is optional: a failure here must never keep the launcher from opening.
        runCatching { enableEdgeToEdge() }.onFailure { Log.w(TAG, "edge-to-edge", it) }
        runCatching { UiLanguage.refreshSystem(this) }.onFailure { Log.w(TAG, "language", it) }
        try {
            setContent {
                YunSeekTheme {
                    LauncherApp()
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Compose UI failed to start", error)
            setContentView(TextView(this).apply {
                setPadding(48, 96, 48, 48)
                text = "$APP_NAME ${BuildConfig.VERSION_NAME}\n\n界面加载失败：${error.javaClass.simpleName}"
            })
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }

    private fun tr(zh: String, en: String): String =
        runCatching { UiLanguage.text(this, zh, en) }.getOrDefault(zh)

    // ---- Environment ----------------------------------------------------------------------------

    private data class HostInfo(val label: String, val supported: Boolean, val installed: Boolean)

    @Suppress("DEPRECATION")
    private fun hostInfo(): HostInfo = runCatching {
        val info = packageManager.getPackageInfo(TARGET_PACKAGE, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        when (code) {
            287L -> HostInfo("国内版 2.7.1 (287)", true, true)
            289L -> HostInfo("国内版 2.7.2 (289)", true, true)
            290L -> HostInfo("国际版 2.7.2 (290)", true, true)
            else -> HostInfo("不支持的版本 ${info.versionName} ($code)", false, true)
        }
    }.getOrDefault(HostInfo("未安装 DeepSeek", false, false))

    private fun isActive(): Boolean {
        val framework = runCatching { XposedActivationProvider.isFrameworkConnected() }.getOrDefault(false)
        val target = runCatching { XposedActivationProvider.isTargetRecentlyActive(this) }.getOrDefault(false)
        return framework || target
    }

    private val appVersionLine: String
        get() = "$APP_NAME ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    private fun openDeepSeekSettings() {
        val launch = runCatching { packageManager.getLaunchIntentForPackage(TARGET_PACKAGE) }.getOrNull()
        if (launch == null) {
            toast("未安装 DeepSeek")
            return
        }
        runCatching { startActivity(launch) }.onFailure {
            toast("无法打开 DeepSeek")
            return
        }
        main.postDelayed({
            runCatching { sendBroadcast(ModuleConfigBridge.requestOpenSettings()) }
        }, 900L)
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { toast("无法打开链接") }
    }

    private fun toast(value: String) = runCatching {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    }

    // ---- Config bridge --------------------------------------------------------------------------

    /** Sends one bridge request with a timeout; [done] is called exactly once on the main thread. */
    private fun bridgeRequest(mode: String, json: String?, done: (Int, Bundle?) -> Unit) {
        val finished = AtomicBoolean(false)
        val reply = object : ResultReceiver(main) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (finished.compareAndSet(false, true)) done(resultCode, resultData)
            }
        }
        main.postDelayed({
            if (finished.compareAndSet(false, true)) done(RESULT_TIMEOUT, null)
        }, BRIDGE_TIMEOUT_MS)
        runCatching { sendBroadcast(ModuleConfigBridge.request(mode, json, reply)) }
            .onFailure {
                if (finished.compareAndSet(false, true)) done(ModuleConfigBridge.RESULT_ERROR, null)
            }
    }

    private fun bridgeFailureText(code: Int, data: Bundle?): String = when {
        code == RESULT_TIMEOUT -> "DeepSeek 未响应：请先打开一次 DeepSeek（确认模块已在 LSPosed 中启用），再回来重试"
        data?.getString("error") != null -> "DeepSeek 返回错误：${data.getString("error")}"
        else -> "请先打开一次 DeepSeek，再重试"
    }

    private fun parseConfigFiles(json: String): Map<String, ByteArray> {
        val root = JSONObject(json)
        val files = root.optJSONArray("files") ?: return emptyMap()
        val out = LinkedHashMap<String, ByteArray>()
        for (i in 0 until files.length()) {
            val entry = files.optJSONObject(i) ?: continue
            val path = entry.optString("path", "")
            if (path.isEmpty()) continue
            out[path] = runCatching { Base64.decode(entry.optString("data", ""), Base64.DEFAULT) }
                .getOrDefault(ByteArray(0))
        }
        return out
    }

    private fun loadConfig(onDone: ((String?) -> Unit)? = null) {
        bridgeStatus = BridgeStatus.LOADING
        bridgeRequest(ModuleConfigBridge.MODE_EXPORT, null) { code, data ->
            val json = data?.getString(ModuleConfigBridge.EXTRA_JSON)
            if (code == ModuleConfigBridge.RESULT_OK && !json.isNullOrEmpty()) {
                configFiles = runCatching { parseConfigFiles(json) }.getOrDefault(emptyMap())
                bridgeStatus = BridgeStatus.READY
                onDone?.invoke(json)
            } else {
                bridgeStatus = BridgeStatus.UNAVAILABLE
                onDone?.invoke(null)
            }
        }
    }

    private fun buildConfigJson(files: Map<String, ByteArray>): String {
        val root = JSONObject()
        root.put("format", "deekseep-config")
        root.put("version", 1)
        root.put("module", BuildConfig.VERSION_NAME)
        root.put("exported_at", System.currentTimeMillis())
        val array = JSONArray()
        files.forEach { (path, bytes) ->
            array.put(JSONObject().put("path", path).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
        root.put("files", array)
        return root.toString(2)
    }

    /** Writes a single settings file back into DeepSeek through the existing import path. */
    private fun writeConfigValue(path: String, value: String, onDone: (Boolean) -> Unit) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        bridgeRequest(ModuleConfigBridge.MODE_IMPORT, buildConfigJson(mapOf(path to bytes))) { code, data ->
            if (code == ModuleConfigBridge.RESULT_OK) {
                configFiles = configFiles + (path to bytes)
                toast("已写入。请强制停止并重新打开 DeepSeek 后生效")
                onDone(true)
            } else {
                toast(bridgeFailureText(code, data))
                onDone(false)
            }
        }
    }

    // ---- Export / import -----------------------------------------------------------------------

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    private fun launchExport(zip: Boolean) {
        pendingExportZip = zip
        val name = "yunseek-${BuildConfig.VERSION_NAME}-${timestamp()}" + if (zip) ".zip" else ".json"
        runCatching {
            if (zip) exportZipLauncher.launch(name) else exportJsonLauncher.launch(name)
        }.onFailure { toast("无法打开文件选择器：${it.message}") }
    }

    private fun launchImport() {
        runCatching {
            importLauncher.launch(
                arrayOf(
                    "application/zip", "application/x-zip-compressed", "application/json",
                    "text/json", "text/plain", "application/octet-stream",
                ),
            )
        }.onFailure { toast("无法打开文件选择器：${it.message}") }
    }

    private fun exportData(uri: Uri, zip: Boolean) {
        busyMessage = "正在读取模块设置…"
        loadConfig { json ->
            if (!zip) {
                if (json == null) {
                    busyMessage = null
                    toast(bridgeFailureText(RESULT_TIMEOUT, null))
                    return@loadConfig
                }
                writeInBackground(uri, json.toByteArray(StandardCharsets.UTF_8), "设置已导出（JSON）")
                return@loadConfig
            }
            busyMessage = "正在打包 ZIP…"
            Thread {
                val result = runCatching {
                    contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                        ZipOutputStream(out.buffered()).use { zos -> writeZip(zos, json) }
                    }
                }
                main.post {
                    busyMessage = null
                    result.onSuccess { count ->
                        toast(
                            if (json == null) "已导出启动器数据（$count 项）。DeepSeek 未响应，模块设置未包含"
                            else "模块数据已导出（ZIP，$count 项）",
                        )
                    }.onFailure { toast("导出失败：${it.message}") }
                }
            }.start()
        }
    }

    private fun writeInBackground(uri: Uri, bytes: ByteArray, success: String) {
        Thread {
            val result = runCatching {
                contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) }
            }
            main.post {
                busyMessage = null
                result.onSuccess { toast(success) }.onFailure { toast("导出失败：${it.message}") }
            }
        }.start()
    }

    /** Returns the number of payload entries written. Runs off the main thread. */
    private fun writeZip(zos: ZipOutputStream, settingsJson: String?): Int {
        var count = 0
        val contents = JSONArray()
        fun put(name: String, bytes: ByteArray) {
            zos.putNextEntry(ZipEntry(name))
            zos.write(bytes)
            zos.closeEntry()
            contents.put(name)
            count++
        }
        if (settingsJson != null) put(ZIP_SETTINGS, settingsJson.toByteArray(StandardCharsets.UTF_8))
        listOf(
            File(filesDir.path) to ZIP_LAUNCHER_FILES,
            File(applicationInfo.dataDir, "shared_prefs") to ZIP_LAUNCHER_PREFS,
        ).forEach { (root, prefix) ->
            root.walkTopDown().filter { it.isFile && it.length() <= MAX_LAUNCHER_FILE }.forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                runCatching { put(prefix + relative, file.readBytes()) }
            }
        }
        val meta = JSONObject()
            .put("format", "yunseek-backup")
            .put("version", 1)
            .put("app", APP_NAME)
            .put("app_version", BuildConfig.VERSION_NAME)
            .put("app_version_code", BuildConfig.VERSION_CODE)
            .put("deepseek", hostInfo().label)
            .put("created_at", System.currentTimeMillis())
            .put("contains_module_settings", settingsJson != null)
            .put("contents", contents)
        zos.putNextEntry(ZipEntry(ZIP_META))
        zos.write(meta.toString(2).toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
        zos.putNextEntry(ZipEntry("README.txt"))
        zos.write(
            ("YunSeek 模块数据备份\n\n" +
                "settings/deekseep-config.json：通过配置桥导出的模块设置（与旧版\u201c导出配置\u201d格式相同）。\n" +
                "launcher/：YunSeek 启动器自身的数据文件。\n\n" +
                "在新安装的 YunSeek 中选择\u201c导入恢复\u201d并选择此 ZIP 即可恢复。\n" +
                "提示词库、置顶与会话记录保存在 DeepSeek 私有目录，不在此备份中。\n")
                .toByteArray(StandardCharsets.UTF_8),
        )
        zos.closeEntry()
        return count
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_IMPORT_BYTES) throw IllegalArgumentException("文件过大")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private class ImportPlan(val settingsJson: String?, val launcherFiles: Map<String, ByteArray>)

    private fun importData(uri: Uri) {
        busyMessage = "正在读取备份…"
        Thread {
            val plan = runCatching {
                val bytes = contentResolver.openInputStream(uri)!!.use { readLimited(it) }
                val isZip = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
                if (isZip) parseZip(bytes) else {
                    val text = String(bytes, StandardCharsets.UTF_8).trim().removePrefix("\uFEFF")
                    val root = JSONObject(JSONTokener(text))
                    require(root.optString("format") == "deekseep-config") { "不是有效的 YunSeek / Deekseep 配置文件" }
                    ImportPlan(text, emptyMap())
                }
            }
            main.post {
                plan.onSuccess { applyImport(it) }.onFailure {
                    busyMessage = null
                    toast("导入失败：${it.message}")
                }
            }
        }.start()
    }

    private fun parseZip(bytes: ByteArray): ImportPlan {
        var settings: String? = null
        val launcher = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name
                when {
                    name == ZIP_SETTINGS -> settings = String(readLimited(zis), StandardCharsets.UTF_8)
                    name.startsWith(ZIP_LAUNCHER_FILES) -> {
                        val data = readLimited(zis)
                        if (data.size <= MAX_LAUNCHER_FILE) launcher[name.removePrefix(ZIP_LAUNCHER_FILES)] = data
                    }
                }
            }
        }
        require(settings != null || launcher.isNotEmpty()) { "ZIP 中没有可恢复的 YunSeek 数据" }
        return ImportPlan(settings, launcher)
    }

    private fun applyImport(plan: ImportPlan) {
        var launcherRestored = 0
        val root = runCatching { filesDir.canonicalFile }.getOrNull()
        if (root != null) {
            plan.launcherFiles.forEach { (relative, data) ->
                runCatching {
                    val target = File(root, relative).canonicalFile
                    require(target.path.startsWith(root.path + File.separator))
                    target.parentFile?.mkdirs()
                    target.writeBytes(data)
                    launcherRestored++
                }
            }
        }
        val settings = plan.settingsJson
        if (settings == null) {
            busyMessage = null
            toast("已恢复启动器数据 $launcherRestored 项（备份中不含模块设置）")
            return
        }
        busyMessage = "正在写入 DeepSeek…"
        bridgeRequest(ModuleConfigBridge.MODE_IMPORT, settings) { code, data ->
            busyMessage = null
            if (code == ModuleConfigBridge.RESULT_OK) {
                runCatching { configFiles = parseConfigFiles(settings); bridgeStatus = BridgeStatus.READY }
                toast("导入完成。请强制停止并重新打开 DeepSeek 后生效")
            } else {
                toast(bridgeFailureText(code, data))
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        runCatching {
            if (requestCode == Main.FULL_DATA_BACKUP_REQUEST) {
                ClosedFullDataBackupBridge.handleExportResult(this, resultCode, data)
            } else if (requestCode == Main.FULL_DATA_RESTORE_REQUEST) {
                ClosedFullDataBackupBridge.handleRestoreResult(this, resultCode, data)
            }
        }.onFailure { toast("操作失败：${it.message}") }
    }

    private fun licenseText(): String = runCatching {
        val id = resources.getIdentifier("gpl_3_0", "raw", packageName)
        require(id != 0)
        resources.openRawResource(id).bufferedReader().use { it.readText() }
    }.getOrDefault(
        "YunSeek 以 GNU General Public License v3.0 (GPL-3.0-only) 授权发布。\n\n" +
            "你可以在遵守 GPL-3.0 的前提下自由使用、研究、修改和再分发本软件；" +
            "分发修改版本时必须以相同许可证公开对应源代码。本软件不提供任何担保。\n\n" +
            "完整许可证文本：https://www.gnu.org/licenses/gpl-3.0.html",
    )

    // ---- UI -------------------------------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LauncherApp() {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var route by rememberSaveable { mutableStateOf<String?>(null) }
        var licenseOpen by remember { mutableStateOf(false) }

        BackHandler(enabled = route != null || tab != 0) {
            if (route != null) route = null else tab = 0
        }

        val title = route?.let { r ->
            if (r == ROUTE_ABOUT) "开源声明与关于" else LauncherCatalog.find(r)?.title
        } ?: APP_NAME

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Crossfade(targetState = title, label = "title") { t ->
                            Text(t, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        AnimatedVisibility(visible = route != null, enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { route = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple("首页", Icons.Filled.Home, 0),
                        Triple("功能", Icons.AutoMirrored.Filled.List, 1),
                        Triple("设置", Icons.Filled.Settings, 2),
                    )
                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = route == null && tab == index,
                            onClick = { route = null; tab = index },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val screen = route ?: "tab:$tab"
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val enteringDetail = !targetState.startsWith("tab:") && initialState.startsWith("tab:")
                        val leavingDetail = targetState.startsWith("tab:") && !initialState.startsWith("tab:")
                        when {
                            enteringDetail ->
                                (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(200)))
                            leavingDetail ->
                                (slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(200)))
                            else ->
                                (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 24 }) togetherWith
                                    (fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 32 })
                        }
                    },
                    label = "screen",
                ) { s ->
                    when {
                        s == "tab:0" -> HomeScreen(onOpen = { route = it })
                        s == "tab:1" -> FeaturesScreen(onOpen = { route = it })
                        s == "tab:2" -> SettingsScreen(onOpen = { route = it })
                        s == ROUTE_ABOUT -> AboutScreen(onLicense = { licenseOpen = true })
                        s == LauncherCatalog.DATA_ID -> DataScreen()
                        s == LauncherCatalog.HELP_ID -> HelpScreen(onAbout = { route = ROUTE_ABOUT })
                        else -> LauncherCatalog.find(s)?.let { CategoryScreen(it) }
                            ?: HomeScreen(onOpen = { route = it })
                    }
                }
                val busy = busyMessage
                if (busy != null) BusyDialog(busy)
            }
        }

        if (licenseOpen) {
            AlertDialog(
                onDismissRequest = { licenseOpen = false },
                title = { Text("GNU GPL-3.0") },
                text = {
                    Text(
                        licenseText(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = { TextButton(onClick = { licenseOpen = false }) { Text("关闭") } },
            )
        }
    }

    @Composable
    private fun BusyDialog(message: String) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(18.dp))
                    Text(message)
                }
            },
        )
    }

    @Composable
    private fun PageColumn(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
            Spacer(Modifier.height(12.dp))
        }
    }

    @Composable
    private fun StatusCard(index: Int) {
        val tick = refreshTick
        val active = remember(tick) { isActive() }
        val host = remember(tick) { hostInfo() }
        val lines = buildList {
            add(appVersionLine)
            add("DeepSeek：${host.label}")
            if (!active) add("请在 LSPosed 中启用 YunSeek 并勾选 DeepSeek，然后重启 DeepSeek")
            else if (host.installed && !host.supported) add("当前 DeepSeek 版本未经适配，部分功能可能失效")
        }
        StatusHeroCard(
            active = active,
            title = if (active) "模块已激活" else "模块未激活",
            lines = lines,
            modifier = Modifier.staggeredEntrance(index),
            onClick = { refreshTick++ },
        )
    }

    @Composable
    private fun HomeScreen(onOpen: (String) -> Unit) {
        PageColumn {
            StatusCard(0)
            SectionHeader("功能", Modifier.staggeredEntrance(1))
            LauncherCatalog.categories.forEachIndexed { i, category ->
                CategoryCard(
                    icon = category.icon,
                    title = category.title,
                    description = category.description,
                    modifier = Modifier.staggeredEntrance(i + 2),
                    onClick = { onOpen(category.id) },
                )
            }
            SectionHeader("数据", Modifier.staggeredEntrance(11))
            Row(
                modifier = Modifier.fillMaxWidth().staggeredEntrance(12),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionTile(Icons.Outlined.ArrowUpward, "导出模块数据", Modifier.weight(1f)) {
                    onOpen(LauncherCatalog.DATA_ID)
                }
                ActionTile(Icons.Outlined.ArrowDownward, "导入恢复", Modifier.weight(1f)) {
                    onOpen(LauncherCatalog.DATA_ID)
                }
            }
        }
    }

    @Composable
    private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
        val interaction = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            interactionSource = interaction,
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = modifier.pressScale(interaction),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }

    @Composable
    private fun FeaturesScreen(onOpen: (String) -> Unit) {
        PageColumn {
            NoteCard(
                "点按分类查看并修改对应设置。启动器通过配置桥读写 DeepSeek 内的模块设置，" +
                    "修改后需强制停止并重新打开 DeepSeek 才会生效。",
                Modifier.staggeredEntrance(0),
                icon = Icons.Filled.Info,
            )
            LauncherCatalog.categories.forEachIndexed { i, category ->
                val detail = when {
                    category.keys.isNotEmpty() -> "${category.description} · ${category.keys.size} 项设置"
                    else -> category.description
                }
                CategoryCard(
                    icon = category.icon,
                    title = category.title,
                    description = detail,
                    modifier = Modifier.staggeredEntrance(i + 1),
                    onClick = { onOpen(category.id) },
                )
            }
            OpenInDeepSeekButton(Modifier.staggeredEntrance(10), "打开 DeepSeek 模块设置")
        }
    }

    @Composable
    private fun SettingsScreen(onOpen: (String) -> Unit) {
        PageColumn {
            SectionHeader("模块", Modifier.staggeredEntrance(0))
            CategoryCard(Icons.Outlined.OpenInNew, "打开 DeepSeek 模块设置",
                "启动 DeepSeek 并直接进入模块设置页", Modifier.staggeredEntrance(1)) { openDeepSeekSettings() }
            CategoryCard(Icons.Filled.Refresh, "重新检测激活状态",
                "刷新激活状态与 DeepSeek 版本识别", Modifier.staggeredEntrance(2)) {
                refreshTick++
                toast("已刷新")
            }
            SectionHeader("数据", Modifier.staggeredEntrance(3))
            CategoryCard(Icons.Outlined.ArrowUpward, "导出与导入恢复",
                "ZIP 全量 / JSON 仅设置，跨大版本重装前使用", Modifier.staggeredEntrance(4)) {
                onOpen(LauncherCatalog.DATA_ID)
            }
            SectionHeader("关于", Modifier.staggeredEntrance(5))
            CategoryCard(Icons.Outlined.Gavel, "开源声明与关于",
                "GPL-3.0 · 原作者与维护者", Modifier.staggeredEntrance(6)) { onOpen(ROUTE_ABOUT) }
            CategoryCard(Icons.Outlined.Code, "源代码", REPOSITORY, Modifier.staggeredEntrance(7)) {
                openUrl(REPOSITORY)
            }
            Text(
                appVersionLine + " · " + BuildInfo.BUILD_EDITION,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).staggeredEntrance(8),
            )
        }
    }

    @Composable
    private fun OpenInDeepSeekButton(modifier: Modifier = Modifier, label: String = "在 DeepSeek 中打开此设置") {
        val interaction = remember { MutableInteractionSource() }
        Button(
            onClick = { openDeepSeekSettings() },
            interactionSource = interaction,
            modifier = modifier.fillMaxWidth().height(52.dp).pressScale(interaction),
        ) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(label)
        }
    }

    @Composable
    private fun CategoryHeader(category: FeatureCategory, index: Int) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().staggeredEntrance(index),
        ) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(category.icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(category.title, style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(category.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                }
            }
        }
    }

    @Composable
    private fun CategoryScreen(category: FeatureCategory) {
        LaunchedEffect(category.id) {
            if (category.keys.isNotEmpty() && bridgeStatus != BridgeStatus.LOADING) loadConfig()
        }
        PageColumn {
            CategoryHeader(category, 0)
            OpenInDeepSeekButton(Modifier.staggeredEntrance(1))
            NoteCard(
                "在启动器中修改的设置会写入 DeepSeek 内的模块配置，需强制停止并重新打开 DeepSeek 后生效。",
                Modifier.staggeredEntrance(2),
                icon = Icons.Filled.Info,
            )
            if (category.note.isNotEmpty()) {
                NoteCard(category.note, Modifier.staggeredEntrance(3), icon = Icons.Filled.Lock)
            }
            if (category.keys.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().staggeredEntrance(3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("当前设置", Modifier.weight(1f))
                    TextButton(onClick = { loadConfig() }, enabled = bridgeStatus != BridgeStatus.LOADING) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("刷新")
                    }
                }
                AnimatedContent(
                    targetState = bridgeStatus,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "bridge",
                ) { status ->
                    when (status) {
                        BridgeStatus.IDLE, BridgeStatus.LOADING -> Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                        BridgeStatus.UNAVAILABLE -> NoteCard(
                            "无法读取设置：DeepSeek 未响应。请先打开一次 DeepSeek（确认模块已在 LSPosed 中启用并勾选 DeepSeek），" +
                                "返回此页后点\u201c刷新\u201d。也可以直接点击上方按钮在 DeepSeek 中修改。",
                            icon = Icons.Filled.Info,
                        )
                        BridgeStatus.READY -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val files = configFiles
                            category.keys.forEach { key -> ConfigKeyRow(key, files[key.path]) }
                            if (category.keys.any { it.kind == ConfigKind.FLAG }) {
                                NoteCard(
                                    "开关类设置在启动器中只读：配置桥只能写入文件、无法删除开关文件，" +
                                        "且部分开关需要在 DeepSeek 内确认风险提示。请在 DeepSeek 中切换。",
                                    icon = Icons.Filled.Lock,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfigKeyRow(key: ConfigKey, current: ByteArray?) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                if (key.kind == ConfigKind.FLAG) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(key.label, style = MaterialTheme.typography.titleSmall)
                            Text(key.path, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(if (current != null) "已开启" else "未开启") },
                        )
                    }
                } else {
                    EditableValue(key, current)
                }
            }
        }
    }

    @Composable
    private fun EditableValue(key: ConfigKey, current: ByteArray?) {
        val original = remember(current) { current?.let { String(it, StandardCharsets.UTF_8) } ?: "" }
        var text by remember(original) { mutableStateOf(original) }
        var saving by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(key.kind == ConfigKind.VALUE) }
        val multiline = key.kind != ConfigKind.VALUE
        val error = remember(text, key.kind) { validate(key, text) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(key.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (current == null) "${key.path} · 未设置" else key.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (multiline) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "编辑") }
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = !multiline,
                    minLines = if (multiline) 4 else 1,
                    maxLines = if (multiline) 14 else 1,
                    textStyle = if (key.kind == ConfigKind.JSON) {
                        MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    } else MaterialTheme.typography.bodyMedium,
                    placeholder = { if (key.hint.isNotEmpty()) Text(key.hint) },
                    supportingText = { Text(error ?: key.hint) },
                    isError = error != null,
                    keyboardOptions = if (key.hint == "正整数") {
                        KeyboardOptions(keyboardType = KeyboardType.Number)
                    } else KeyboardOptions.Default,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        enabled = !saving && error == null && text != original,
                        onClick = {
                            saving = true
                            writeConfigValue(key.path, text) { saving = false }
                        },
                    ) { Text(if (saving) "写入中…" else "写入 DeepSeek") }
                    OutlinedButton(enabled = text != original, onClick = { text = original }) { Text("还原") }
                }
            }
        }
    }

    private fun validate(key: ConfigKey, text: String): String? = when {
        key.kind == ConfigKind.JSON && text.isNotBlank() -> runCatching {
            val value = JSONTokener(text).nextValue()
            require(value is JSONObject || value is JSONArray)
            null
        }.getOrElse { "JSON 格式错误" }
        key.hint == "正整数" && text.isNotBlank() && (text.trim().toIntOrNull()?.let { it <= 0 } != false) -> "请输入正整数"
        else -> null
    }

    @Composable
    private fun DataScreen() {
        var zip by rememberSaveable { mutableStateOf(true) }
        PageColumn {
            NoteCard(
                "用于跨大版本重装 YunSeek 或 DeepSeek：卸载前先导出，安装新版本并激活模块后再导入恢复。" +
                    "导出与导入都需要 DeepSeek 已打开过一次（模块已注入）。",
                Modifier.staggeredEntrance(0),
                icon = Icons.Filled.Info,
            )
            SectionHeader("导出模块数据", Modifier.staggeredEntrance(1))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().staggeredEntrance(2)) {
                SegmentedButton(
                    selected = zip,
                    onClick = { zip = true },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("ZIP 全量") }
                SegmentedButton(
                    selected = !zip,
                    onClick = { zip = false },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("JSON 仅设置") }
            }
            AnimatedContent(targetState = zip, label = "formatDesc",
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }) { z ->
                Text(
                    if (z) "包含：配置桥可读取的全部模块设置（聊天、外观、Agent、心跳计划、远程开关覆盖、语言等）" +
                        "以及 YunSeek 启动器自身的数据文件。"
                    else "仅包含模块设置，与旧版\u201c导出配置\u201d的 JSON 格式完全兼容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            val exportInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { launchExport(zip) },
                interactionSource = exportInteraction,
                modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(exportInteraction).staggeredEntrance(3),
            ) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (zip) "导出 ZIP" else "导出 JSON")
            }
            SectionHeader("导入恢复", Modifier.staggeredEntrance(4))
            Text(
                "选择之前导出的 ZIP 或 JSON（也兼容旧版 Deekseep 导出的配置 JSON）。导入会覆盖同名设置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp).staggeredEntrance(5),
            )
            val importInteraction = remember { MutableInteractionSource() }
            FilledTonalButton(
                onClick = { launchImport() },
                interactionSource = importInteraction,
                modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(importInteraction).staggeredEntrance(6),
            ) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("导入恢复")
            }
            NoteCard(
                "提示词库、置顶与会话记录保存在 DeepSeek 私有目录，启动器无法读取，不在此备份中；" +
                    "请在 DeepSeek 内的模块设置中处理。导入后需强制停止并重新打开 DeepSeek 才会生效。",
                Modifier.staggeredEntrance(7),
                icon = Icons.Filled.Lock,
            )
            if (BuildInfo.PROTECTED_BUILD && isFullBackupHostSupported()) {
                SectionHeader("完整数据（需要 Root）", Modifier.staggeredEntrance(8))
                CategoryCard(Icons.Outlined.ArrowUpward, "一键备份全部数据",
                    "加密保存模块配置、DeepSeek 数据、聊天记录与日志", Modifier.staggeredEntrance(9)) {
                    runCatching { ClosedFullDataBackupBridge.beginBackup(this@SettingsActivity) }
                        .onFailure { toast("备份失败：${it.message}") }
                }
                CategoryCard(Icons.Outlined.ArrowDownward, "一键恢复全部数据",
                    "校验备份后覆盖当前私有数据", Modifier.staggeredEntrance(10)) {
                    runCatching { ClosedFullDataBackupBridge.beginRestore(this@SettingsActivity) }
                        .onFailure { toast("恢复失败：${it.message}") }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isFullBackupHostSupported(): Boolean = runCatching {
        val info = packageManager.getPackageInfo(TARGET_PACKAGE, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        code == 249L || code == 257L || code == 268L
    }.getOrDefault(false)

    @Composable
    private fun HelpScreen(onAbout: () -> Unit) {
        val steps = listOf(
            "在 LSPosed 中启用 YunSeek，作用域勾选 DeepSeek（com.deepseek.chat）。",
            "强制停止并重新打开 DeepSeek，首页状态卡显示\u201c模块已激活\u201d即表示注入成功。",
            "完整的功能设置位于 DeepSeek 内的模块设置页，可在任意分类页点击\u201c在 DeepSeek 中打开此设置\u201d直接进入。",
            "在启动器中修改的设置需要重启 DeepSeek 后生效。",
            "跨大版本重装前，请先在\u201c数据\u201d中导出 ZIP，安装后再导入恢复。",
            "已适配：国内版 2.7.1 (287)、国内版 2.7.2 (289)、国际版 2.7.2 (290)。",
        )
        PageColumn {
            CategoryHeader(LauncherCatalog.find(LauncherCatalog.HELP_ID)!!, 0)
            SectionHeader("使用说明", Modifier.staggeredEntrance(1))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().staggeredEntrance(2),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps.forEachIndexed { i, step ->
                        Row {
                            Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(24.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            OpenInDeepSeekButton(Modifier.staggeredEntrance(3), "打开 DeepSeek 模块设置")
            CategoryCard(Icons.Outlined.Gavel, "开源声明与关于", "GPL-3.0 · 原作者与维护者",
                Modifier.staggeredEntrance(4), onAbout)
        }
    }

    @Composable
    private fun AboutScreen(onLicense: () -> Unit) {
        val rows = listOf(
            "应用" to appVersionLine,
            "版本类型" to BuildInfo.BUILD_EDITION,
            "构建时间" to BuildInfo.BUILD_DATE,
            "原作者" to "Lucian (lllucccian) · Deekseep",
            "YunSeek 维护者" to "张云安 (ZW-SYS)",
            "许可证" to "GNU GPL-3.0",
        )
        PageColumn {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().staggeredEntrance(0),
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(APP_NAME, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "适用于 DeepSeek 的 Xposed 模块，基于 Lucian 的开源项目 Deekseep 维护。" +
                            "本模块与 DeepSeek 官方无关。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().staggeredEntrance(1),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    rows.forEachIndexed { i, (k, v) ->
                        if (i > 0) HorizontalDivider(
                            Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(k, modifier = Modifier.weight(0.38f), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(v, modifier = Modifier.weight(0.62f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            CategoryCard(Icons.Outlined.Gavel, "开源许可证", "查看 GNU GPL-3.0 许可证",
                Modifier.staggeredEntrance(2), onLicense)
            CategoryCard(Icons.Outlined.Code, "源代码", REPOSITORY, Modifier.staggeredEntrance(3)) {
                openUrl(REPOSITORY)
            }
        }
    }
}
