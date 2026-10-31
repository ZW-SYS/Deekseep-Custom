package com.dsmod.probe;

import android.os.SystemClock;

import org.json.JSONObject;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "模型显示名": user-editable, display-only renaming of mode labels DeepSeek shows in its UI.
 *
 * <p>DeepSeek 2.7.x merged the three modes and has no deep-think toggle, so a mapping only takes
 * effect when its exact source text is actually rendered. Mapping is applied exclusively inside
 * the module's existing text-display hooks (the Compose {@code Text(String, …)} composable and
 * the Compose {@code stringResource} helpers installed by {@code HookUiSurface.hookHomeGreeting}).
 * It is never applied to {@code Resources.getString}, request bodies, model ids or any other
 * value that can reach the server.</p>
 *
 * <p>Matching is exact on the trimmed text so ordinary message content that merely contains the
 * word is not touched. Stored next to the module's other local files.</p>
 */
final class ModelDisplayNames {

    static final String FILE = new File(new File(Main.PROMPT_FILE).getParentFile(),
            "deekseep_model_display_names.json").getAbsolutePath();

    /** Defaults offered in the editor; they apply only if such text is shown by the host. */
    static Map<String, String> defaults() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("快速", "闪电");
        map.put("深度思考", "深思");
        return map;
    }

    static final class Config {
        boolean enabled;
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
    }

    private static final Object LOCK = new Object();
    private static volatile Config cached;
    private static volatile long cachedModified = Long.MIN_VALUE;
    private static volatile long lastCheckAt;
    private static volatile boolean fastEnabled;
    private static volatile Map<String, String> fastMap = Collections.emptyMap();

    private ModelDisplayNames() {}

    static Config load() {
        synchronized (LOCK) {
            File file = new File(FILE);
            long modified = file.isFile() ? file.lastModified() : -1L;
            if (cached != null && modified == cachedModified) return cached;
            Config config = new Config();
            if (modified < 0) {
                config.enabled = true;
                config.map.putAll(defaults());
            } else {
                try {
                    String raw = Main.readSmallText(FILE);
                    JSONObject root = new JSONObject(raw == null ? "{}" : raw);
                    config.enabled = root.optBoolean("enabled", true);
                    JSONObject map = root.optJSONObject("map");
                    if (map != null) {
                        Iterator<String> keys = map.keys();
                        while (keys.hasNext()) {
                            String from = clean(keys.next());
                            String to = clean(map.optString(from, ""));
                            if (from.length() > 0 && to.length() > 0) config.map.put(from, to);
                        }
                    } else {
                        config.map.putAll(defaults());
                    }
                } catch (Throwable t) {
                    config.enabled = false;
                    config.map.clear();
                    config.map.putAll(defaults());
                }
            }
            cached = config;
            cachedModified = modified;
            fastEnabled = config.enabled && !config.map.isEmpty();
            fastMap = Collections.unmodifiableMap(new LinkedHashMap<>(config.map));
            return config;
        }
    }

    static boolean save(boolean enabled, Map<String, String> map) {
        try {
            JSONObject root = new JSONObject();
            root.put("enabled", enabled);
            JSONObject m = new JSONObject();
            if (map != null) {
                for (Map.Entry<String, String> e : map.entrySet()) {
                    String from = clean(e.getKey());
                    String to = clean(e.getValue());
                    if (from.length() > 0 && to.length() > 0) m.put(from, to);
                }
            }
            root.put("map", m);
            Main.overwriteTextFile(FILE, root.toString(2));
            synchronized (LOCK) { cachedModified = Long.MIN_VALUE; }
            lastCheckAt = 0L;
            load();
            return true;
        } catch (Throwable t) {
            Main.log("model display names save failed: " + t);
            return false;
        }
    }

    static String clean(String value) {
        if (value == null) return "";
        String s = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    /** Display-only mapping for a text about to be rendered. Returns null when unchanged. */
    static String mapDisplay(Object text) {
        if (!(text instanceof String)) return null;
        String value = (String) text;
        if (value.length() == 0 || value.length() > 40) return null;
        long now = SystemClock.uptimeMillis();
        if (now - lastCheckAt > 1000L) {
            lastCheckAt = now;
            try { load(); } catch (Throwable ignored) { return null; }
        }
        if (!fastEnabled) return null;
        String mapped = fastMap.get(value.trim());
        return mapped == null || mapped.equals(value) ? null : mapped;
    }
}
