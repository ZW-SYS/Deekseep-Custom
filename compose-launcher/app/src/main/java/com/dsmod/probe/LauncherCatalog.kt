package com.dsmod.probe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
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
            description = "去审查、专家模式、消息详情与假静音",
            icon = Icons.Outlined.Forum,
            keys = listOf(
                ConfigKey("deekseep_nocensor", "去审查", ConfigKind.FLAG),
                ConfigKey("deekseep_expert_unlock", "专家模式解锁", ConfigKind.FLAG),
                ConfigKey("deekseep_message_details", "消息详情", ConfigKind.FLAG),
                ConfigKey("deekseep_fake_mute_enabled", "假静音", ConfigKind.FLAG),
                ConfigKey(
                    "deekseep_fake_mute_until", "假静音截止时间", ConfigKind.VALUE,
                    "毫秒时间戳；留空表示未设置",
                ),
            ),
        ),
        FeatureCategory(
            id = "appearance",
            title = "界面美化",
            description = "聊天外观、配色与界面语言",
            icon = Icons.Filled.Star,
            keys = listOf(
                ConfigKey("deekseep_appearance/config.json", "聊天外观配置", ConfigKind.JSON),
                ConfigKey(
                    "deekseep_language", "模块界面语言", ConfigKind.VALUE,
                    "auto / chinese / english",
                ),
            ),
        ),
        FeatureCategory(
            id = "agent",
            title = "Agent",
            description = "工具调用、MCP 与主动心跳计划",
            icon = Icons.Filled.Build,
            keys = listOf(
                ConfigKey("deekseep_agent/settings.json", "Agent 设置", ConfigKind.JSON),
                ConfigKey(
                    "deekseep_proactive_interval_minutes", "主动心跳间隔（分钟）",
                    ConfigKind.VALUE, "正整数",
                ),
                ConfigKey("deekseep_proactive_plan.txt", "主动心跳计划", ConfigKind.TEXT),
                ConfigKey("deekseep_proactive_binding.json", "主动心跳会话绑定", ConfigKind.JSON),
            ),
        ),
        FeatureCategory(
            id = "prompts",
            title = "提示词库",
            description = "管理、搜索并快速插入提示词",
            icon = Icons.AutoMirrored.Filled.List,
            note = "提示词库保存在 DeepSeek 私有目录中。为保护隐私，配置桥不会传输提示词内容，" +
                "启动器无法直接读取或修改，请在 DeepSeek 内的模块设置中管理。",
        ),
        FeatureCategory(
            id = "session",
            title = "会话工具",
            description = "多选、搜索、编辑与缓存清理",
            icon = Icons.Filled.Edit,
            keys = listOf(
                ConfigKey("deekseep_chat_multiselect", "聊天多选", ConfigKind.FLAG),
                ConfigKey("deekseep_auto_cache_clean", "自动清理缓存", ConfigKind.FLAG),
                ConfigKey("deekseep_auto_cache_days", "缓存保留天数", ConfigKind.VALUE, "正整数"),
            ),
        ),
        FeatureCategory(
            id = DATA_ID,
            title = "数据",
            description = "导出模块数据、导入恢复（跨大版本重装）",
            icon = Icons.Outlined.ArrowDownward,
        ),
        FeatureCategory(
            id = "debug",
            title = "调试",
            description = "Hook 日志浮层、远程开关覆盖与更新控制",
            icon = Icons.Outlined.Code,
            keys = listOf(
                ConfigKey("deekseep_enabled", "模块功能总开关", ConfigKind.FLAG),
                ConfigKey("deekseep_hook_overlay", "Hook 日志浮层", ConfigKind.FLAG),
                ConfigKey("deekseep_force_training_disabled", "强制关闭训练数据使用", ConfigKind.FLAG),
                ConfigKey("deekseep_hot_update_disabled", "禁用热更新", ConfigKind.FLAG),
                ConfigKey("deekseep_google_login_unlock", "Google 登录入口", ConfigKind.FLAG),
                ConfigKey("deekseep_password_login_unlock", "密码登录入口", ConfigKind.FLAG),
                ConfigKey("deekseep_wechat_mobile_login_unlock", "微信 / 手机号登录入口", ConfigKind.FLAG),
                ConfigKey(
                    "deekseep_remote_feature_overrides.json", "远程功能开关覆盖", ConfigKind.JSON,
                ),
            ),
        ),
        FeatureCategory(
            id = HELP_ID,
            title = "帮助",
            description = "使用说明、开源声明与关于",
            icon = Icons.Filled.Info,
        ),
    )

    fun find(id: String): FeatureCategory? = categories.firstOrNull { it.id == id }
}
