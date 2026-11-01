package com.dsmod.probe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Static description of the launcher's feature categories and the settings files each one owns.
 *
 * The paths are exactly the allow-list of [ModuleConfigBridge] (files under DeepSeek's private
 * files directory). The launcher process cannot read that directory itself; it only sees these
 * files through the authenticated export/import broadcast handled inside the injected DeepSeek.
 */
internal enum class ConfigKind {
    /** Presence-based switch: the feature is on while the file exists. Read-only in launcher. */
    FLAG,
    /** Short plain-text value, editable and written back through the import bridge. */
    VALUE,
    /** Multi-line text / JSON document, editable and written back through the import bridge. */
    TEXT,
    JSON,
}

internal data class ConfigKey(
    val path: String,
    val label: String,
    val kind: ConfigKind,
    val hint: String = "",
)

internal data class FeatureCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val keys: List<ConfigKey> = emptyList(),
    val note: String = "",
)

internal object LauncherCatalog {
    const val DATA_ID = "data"
    const val HELP_ID = "help"

    val categories: List<FeatureCategory> = listOf(
        FeatureCategory(
            id = "chat",
            title = "聊天",
            description = "防撤回、消息详情与本地禁言",
            icon = Icons.Outlined.ChatBubbleOutline,
            keys = listOf(
                ConfigKey("deekseep_nocensor", "防撤回（保留被替换前的回答）", ConfigKind.FLAG),
                ConfigKey("deekseep_message_details", "消息时间与详情", ConfigKind.FLAG),
                ConfigKey("deekseep_fake_mute_enabled", "本地禁言", ConfigKind.FLAG),
                ConfigKey(
                    "deekseep_fake_mute_until", "本地禁言截止时间", ConfigKind.VALUE,
                    "毫秒时间戳；留空表示未设置",
                ),
            ),
        ),
        FeatureCategory(
            id = "account",
            title = "账号与隐私",
            description = "数据使用与登录入口显示",
            icon = Icons.Outlined.Lock,
            keys = listOf(
                ConfigKey("deekseep_force_training_disabled", "禁用数据用于优化体验", ConfigKind.FLAG),
                ConfigKey("deekseep_google_login_unlock", "显示 Google 登录入口", ConfigKind.FLAG),
                ConfigKey("deekseep_password_login_unlock", "显示密码登录入口", ConfigKind.FLAG),
                ConfigKey("deekseep_wechat_mobile_login_unlock", "显示微信 / 手机号登录入口", ConfigKind.FLAG),
            ),
        ),
        FeatureCategory(
            id = "appearance",
            title = "界面",
            description = "聊天外观、字体、导航与界面语言",
            icon = Icons.Outlined.Palette,
            keys = listOf(
                ConfigKey("deekseep_appearance/config.json", "聊天外观配置", ConfigKind.JSON),
                ConfigKey(
                    "deekseep_language", "模块界面语言", ConfigKind.VALUE,
                    "auto / chinese / english",
                ),
            ),
        ),
        FeatureCategory(
            id = "session",
            title = "会话工具",
            description = "多选、搜索、导出与缓存清理",
            icon = Icons.Outlined.Forum,
            keys = listOf(
                ConfigKey("deekseep_chat_multiselect", "聊天多选", ConfigKind.FLAG),
                ConfigKey("deekseep_auto_cache_clean", "自动清理缓存", ConfigKind.FLAG),
                ConfigKey("deekseep_auto_cache_days", "缓存保留天数", ConfigKind.VALUE, "正整数"),
            ),
        ),
        FeatureCategory(
            id = "prompts",
            title = "提示词库",
            description = "管理、搜索并快速插入提示词",
            icon = Icons.Outlined.Bookmarks,
            note = "提示词库保存在 DeepSeek 私有目录中。为保护隐私，配置桥不会传输提示词内容，" +
                "启动器无法直接读取或修改，请在 DeepSeek 内的模块设置中管理。",
        ),
        FeatureCategory(
            id = "experimental",
            title = "实验性",
            description = "Agent 与 AI 心跳，默认关闭",
            icon = Icons.Outlined.Science,
            keys = listOf(
                ConfigKey("deekseep_agent/settings.json", "Agent 设置", ConfigKind.JSON),
                ConfigKey(
                    "deekseep_proactive_interval_minutes", "AI 心跳间隔（分钟）",
                    ConfigKind.VALUE, "正整数（分钟），最少 15",
                ),
                ConfigKey("deekseep_proactive_plan.txt", "AI 心跳计划", ConfigKind.TEXT),
                ConfigKey("deekseep_proactive_binding.json", "AI 心跳会话绑定", ConfigKind.JSON),
            ),
        ),
        FeatureCategory(
            id = "debug",
            title = "调试",
            description = "Hook 日志、远程开关与更新控制",
            icon = Icons.Outlined.BugReport,
            keys = listOf(
                ConfigKey("deekseep_enabled", "模块功能总开关", ConfigKind.FLAG),
                ConfigKey("deekseep_hook_overlay", "Hook 日志浮层", ConfigKind.FLAG),
                ConfigKey("deekseep_hot_update_disabled", "禁用热更新", ConfigKind.FLAG),
                ConfigKey(
                    "deekseep_remote_feature_overrides.json", "远程功能开关覆盖", ConfigKind.JSON,
                ),
            ),
        ),
        FeatureCategory(
            id = DATA_ID,
            title = "数据",
            description = "导出模块数据、导入恢复",
            icon = Icons.Outlined.SaveAlt,
        ),
        FeatureCategory(
            id = HELP_ID,
            title = "帮助",
            description = "使用说明、开源声明与关于",
            icon = Icons.Outlined.HelpOutline,
        ),
    )

    /** Ids shown in the "功能" group; the rest go under "帮助". */
    val featureIds = listOf("chat", "account", "appearance", "session", "prompts", "experimental", "debug")

    fun find(id: String): FeatureCategory? = categories.firstOrNull { it.id == id }
}
