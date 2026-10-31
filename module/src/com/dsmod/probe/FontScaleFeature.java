/*
 * Deekseep - LSPosed module for the DeepSeek Android app.
 * Copyright (C) the Deekseep authors (original author: lllucccian).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.dsmod.probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dsmod.probe.LegacyXposedModule.Chain;
import com.dsmod.probe.LegacyXposedModule.Hooker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DeepSeek-only text size (added in the 1.8.0 port).
 *
 * <p>DeepSeek is a Jetpack Compose app, and Compose derives {@code Density.fontScale} from the
 * Activity's resource configuration. The module therefore wraps MainActivity's base context in a
 * sparse configuration override that only sets {@code fontScale}. This uses public framework API
 * only and touches no obfuscated DeepSeek class, so it is independent of the host's R8 symbol
 * table. It scales all {@code sp} text in DeepSeek (chat, input, menus) but not dp-sized layout.
 * The system-wide font size setting is still honoured and multiplied.</p>
 */
final class FontScaleFeature {
    static final String FILE = "/data/data/com.deepseek.chat/files/deekseep_font_scale";
    static final float MIN = 0.80f;
    static final float MAX = 1.60f;
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static volatile float cached = Float.NaN;

    private FontScaleFeature() {}

    /** Must run from handleLoadPackage, before MainActivity is attached. */
    static void install(Main module) {
        if (module == null || !INSTALLED.compareAndSet(false, true)) return;
        try {
            Method attach = findAttachBaseContext();
            if (attach == null) {
                Main.log("font scale: attachBaseContext not found");
                return;
            }
            module.hook(attach).intercept(new Hooker() {
                @Override public Object intercept(Chain chain) throws Throwable {
                    try {
                        Object self = chain.getThisObject();
                        Object base = chain.getArg(0);
                        if (self instanceof Activity && base instanceof Context
                                && "com.deepseek.chat.MainActivity".equals(
                                HostRemap.className(self.getClass()))) {
                            float scale = readScale();
                            if (Math.abs(scale - 1f) > 0.001f) {
                                Context wrapped = wrap((Context) base, scale);
                                if (wrapped != null) return chain.proceed(wrapped);
                            }
                        }
                    } catch (Throwable t) {
                        Main.log("font scale apply failed: " + t);
                    }
                    return chain.proceed();
                }
            });
            Main.log("font scale hook installed on " + HostRemap.className(attach.getDeclaringClass()));
        } catch (Throwable t) {
            Main.log("font scale hook failed: " + t);
        }
    }

    private static Method findAttachBaseContext() {
        Class<?>[] owners = {Activity.class, ContextThemeWrapper.class,
                android.content.ContextWrapper.class};
        for (Class<?> owner : owners) {
            try {
                Method m = HostRemap.declaredMethod(owner, "attachBaseContext", Context.class);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    static Context wrap(Context base, float scale) {
        Configuration current = base.getResources().getConfiguration();
        // Sparse override: only fontScale is defined, so later orientation/locale/night-mode
        // changes still come from the system configuration.
        Configuration override = new Configuration();
        override.fontScale = clamp(current.fontScale * scale, 0.5f, 3.0f);
        return base.createConfigurationContext(override);
    }

    static float readScale() {
        float value = cached;
        if (!Float.isNaN(value)) return value;
        value = 1f;
        File f = new File(FILE);
        if (f.isFile()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(f);
                byte[] buf = new byte[32];
                int n = in.read(buf);
                if (n > 0) value = parse(new String(buf, 0, n, "UTF-8"));
            } catch (Throwable ignored) {
            } finally {
                if (in != null) try { in.close(); } catch (Throwable ignored) {}
            }
        }
        cached = value;
        return value;
    }

    /** Parses and clamps a stored scale; invalid input means "unchanged" (1.0). */
    static float parse(String text) {
        try {
            float v = Float.parseFloat(text.trim());
            if (Float.isNaN(v) || Float.isInfinite(v)) return 1f;
            return clamp(v, MIN, MAX);
        } catch (Throwable ignored) {
            return 1f;
        }
    }

    static boolean save(float scale) {
        float v = clamp(scale, MIN, MAX);
        File f = new File(FILE);
        try {
            File dir = f.getParentFile();
            if (dir != null && !dir.isDirectory()) dir.mkdirs();
            if (Math.abs(v - 1f) < 0.001f) {
                cached = 1f;
                return !f.exists() || f.delete();
            }
            FileOutputStream out = new FileOutputStream(f, false);
            try { out.write(String.format(Locale.US, "%.2f", v).getBytes("UTF-8")); }
            finally { out.close(); }
            cached = v;
            return true;
        } catch (Throwable t) {
            Main.log("font scale save failed: " + t);
            return false;
        }
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Settings dialog: 80%-160% in 5% steps; applies by recreating DeepSeek's activity. */
    static void showDialog(final Activity act) {
        if (act == null) return;
        final int steps = Math.round((MAX - MIN) / 0.05f);
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(act, 20);
        box.setPadding(pad, dp(act, 12), pad, 0);
        final M3Theme.Palette pal = DeekseepUi.palette(act);
        final TextView label = new TextView(act);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setTextColor(pal.onSurface);
        final TextView sample = new TextView(act);
        sample.setText(UiLanguage.text(act, "预览：你好，DeepSeek！", "Preview: Hello, DeepSeek!"));
        sample.setTextColor(pal.onSurface);
        sample.setPadding(dp(act, 16), dp(act, 14), dp(act, 16), dp(act, 14));
        sample.setBackground(M3Theme.rounded(pal.surfaceContainerHighest, dp(act, 16)));
        final SeekBar bar = new SeekBar(act);
        try {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(pal.primary));
            bar.setThumbTintList(android.content.res.ColorStateList.valueOf(pal.primary));
            bar.setProgressBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(pal.surfaceContainerHighest));
        } catch (Throwable ignored) {}
        bar.setMax(steps);
        float current = readScale();
        bar.setProgress(Math.round((current - MIN) / 0.05f));
        final Runnable refresh = new Runnable() {
            @Override public void run() {
                float v = MIN + bar.getProgress() * 0.05f;
                label.setText(UiLanguage.text(act, "DeepSeek 文字大小：", "DeepSeek text size: ")
                        + Math.round(v * 100) + "%");
                sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * v);
            }
        };
        refresh.run();
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) { refresh.run(); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(label);
        box.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams sampleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sampleLp.topMargin = dp(act, 8);
        box.addView(sample, sampleLp);
        TextView hint = new TextView(act);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hint.setTextColor(pal.onSurfaceVariant);
        hint.setText(UiLanguage.text(act,
                "只影响 DeepSeek，会与系统字体大小叠加。保存后界面会重新加载一次。",
                "Affects DeepSeek only and multiplies the system font size. "
                        + "The screen reloads once after saving."));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(act, 12);
        box.addView(hint, hintLp);
        AlertDialog dialog = new AlertDialog.Builder(act)
                .setTitle(UiLanguage.text(act, "字体大小", "Text size"))
                .setView(box)
                .setNegativeButton(UiLanguage.text(act, "取消", "Cancel"), null)
                .setNeutralButton(UiLanguage.text(act, "恢复默认", "Reset"),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                apply(act, 1f);
                            }
                        })
                .setPositiveButton(UiLanguage.text(act, "保存", "Save"),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                apply(act, MIN + bar.getProgress() * 0.05f);
                            }
                        })
                .create();
        M3Theme.styleDialog(dialog, act);
        dialog.show();
    }

    private static void apply(final Activity act, float scale) {
        if (!save(scale)) {
            UiLanguage.toast(act, UiLanguage.text(act, "保存失败", "Save failed"),
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (!INSTALLED.get()) {
            UiLanguage.toast(act, UiLanguage.text(act, "已保存，重启 DeepSeek 后生效",
                    "Saved; restart DeepSeek to apply"), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        try {
            act.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override public void run() {
                    try { act.recreate(); } catch (Throwable ignored) {}
                }
            }, 200);
        } catch (Throwable ignored) {}
    }

    private static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }
}
