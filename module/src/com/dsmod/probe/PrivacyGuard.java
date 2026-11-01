package com.dsmod.probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * YunSeek privacy features, all local: 后台防窥, 防截屏, 应用锁 (biometric/device credential with
 * a salted SHA-256 PIN fallback), 纯黑 AMOLED and the hidden-conversation list (隐藏对话).
 * Every entry point is fail-safe: any exception leaves the host untouched and never locks the
 * user out (the lock always offers the PIN set when it was enabled).
 */
final class PrivacyGuard {

    static final String FILE = "/data/data/com.deepseek.chat/files/yunseek_privacy.json";
    private static final String COVER_TAG = "yunseek_privacy_cover";
    private static final String LOCK_TAG = "yunseek_app_lock";

    private static final Object LOCK = new Object();
    private static volatile JSONObject cached;
    private static volatile long cachedModified = Long.MIN_VALUE;

    private static volatile long lastPauseAt;      // elapsedRealtime of the last host pause
    private static volatile boolean locked = true; // cold start counts as "returning"
    private static volatile boolean authInProgress;
    private static volatile boolean showHiddenUnlocked;
    private static volatile boolean autoPrompted;

    private PrivacyGuard() {}

    // ── settings ────────────────────────────────────────────────────────────

    static JSONObject cfg() {
        synchronized (LOCK) {
            File f = new File(FILE);
            long m = f.isFile() ? f.lastModified() : -1L;
            if (cached != null && m == cachedModified) return cached;
            JSONObject o;
            try {
                String raw = m < 0 ? null : Main.readSmallText(FILE);
                o = new JSONObject(raw == null || raw.trim().isEmpty() ? "{}" : raw);
            } catch (Throwable t) { o = new JSONObject(); }
            cached = o;
            cachedModified = m;
            return o;
        }
    }

    static boolean bool(String key) { return cfg().optBoolean(key, false); }

    static void put(String key, Object value) {
        synchronized (LOCK) {
            try {
                JSONObject o = new JSONObject(cfg().toString());
                o.put(key, value);
                Main.overwriteTextFile(FILE, o.toString());
                cached = o;
                cachedModified = new File(FILE).lastModified();
            } catch (Throwable t) { Main.log("privacy save failed: " + t); }
        }
    }

    static boolean recentsGuardEnabled() { return bool("recentsGuard"); }
    static boolean noScreenshotEnabled() { return bool("noScreenshot"); }
    static boolean appLockEnabled() { return bool("appLock") && hasPin(); }
    static boolean amoledEnabled() { return bool("amoled"); }
    static int lockMinutes() { return Math.max(0, cfg().optInt("lockMinutes", 0)); }

    // ── PIN (salted SHA-256) ─────────────────────────────────────────────────

    static boolean hasPin() {
        return cfg().optString("pinHash", "").length() > 0;
    }

    private static String sha256(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((salt + ":" + pin).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Throwable t) { return ""; }
    }

    static boolean setPin(String pin) {
        if (pin == null || !pin.matches("\\d{4,6}")) return false;
        byte[] raw = new byte[16];
        new SecureRandom().nextBytes(raw);
        StringBuilder salt = new StringBuilder();
        for (byte b : raw) salt.append(String.format("%02x", b & 0xff));
        put("pinSalt", salt.toString());
        put("pinHash", sha256(salt.toString(), pin));
        return hasPin();
    }

    static boolean checkPin(String pin) {
        String hash = cfg().optString("pinHash", "");
        return hash.length() > 0 && pin != null
                && hash.equals(sha256(cfg().optString("pinSalt", ""), pin));
    }

    // ── hidden conversations (item 8) ────────────────────────────────────────

    static Set<String> hiddenConversations() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        JSONArray a = cfg().optJSONArray("hidden");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            String v = a.optString(i, "").trim();
            if (v.length() > 0) out.add(v);
        }
        return out;
    }

    static void setHidden(String key, boolean hidden) {
        if (key == null || key.trim().isEmpty()) return;
        Set<String> s = hiddenConversations();
        if (hidden) s.add(key.trim()); else s.remove(key.trim());
        put("hidden", new JSONArray(s));
    }

    static boolean isHidden(String id, String title) {
        if (showHiddenUnlocked && bool("showHidden")) return false;
        Set<String> s = hiddenConversations();
        return (id != null && s.contains(id.trim()))
                || (title != null && s.contains(title.trim()));
    }

    // ── lifecycle ────────────────────────────────────────────────────────────

    static void onPaused(Activity a) {
        if (a == null) return;
        try {
            if (!authInProgress) lastPauseAt = SystemClock.elapsedRealtime();
            if (recentsGuardEnabled()) {
                if (Build.VERSION.SDK_INT < 33) {
                    a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
                addCover(a);
            }
        } catch (Throwable ignored) {}
    }

    static void onResumed(Activity a) {
        if (a == null) return;
        try {
            Window w = a.getWindow();
            boolean recents = recentsGuardEnabled();
            if (Build.VERSION.SDK_INT >= 33) {
                try { a.setRecentsScreenshotEnabled(!recents); } catch (Throwable ignored) {}
            }
            removeTagged(a, COVER_TAG);
            if (noScreenshotEnabled()) {
                w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
            applyAmoled(a);
            if (!authInProgress && appLockEnabled()) {
                long since = SystemClock.elapsedRealtime() - lastPauseAt;
                long need = Math.max(lockMinutes() * 60_000L, 2_000L);
                if (lastPauseAt > 0 && since >= need && !locked) { locked = true; autoPrompted = false; }
                if (locked) showLock(a); else removeTagged(a, LOCK_TAG);
            } else if (!appLockEnabled()) {
                locked = false;
                removeTagged(a, LOCK_TAG);
            }
        } catch (Throwable t) { Main.log("privacy resume skipped: " + t); }
    }

    private static void applyAmoled(Activity a) {
        if (!amoledEnabled()) return;
        boolean dark = (a.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (!dark) return;
        try {
            Window w = a.getWindow();
            w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            w.getDecorView().setBackgroundColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= 21) {
                w.setNavigationBarColor(Color.BLACK);
            }
        } catch (Throwable ignored) {}
    }

    private static ViewGroup decor(Activity a) {
        View d = a.getWindow().getDecorView();
        return d instanceof ViewGroup ? (ViewGroup) d : null;
    }

    private static void removeTagged(Activity a, String tag) {
        ViewGroup d = decor(a);
        if (d == null) return;
        for (int i = d.getChildCount() - 1; i >= 0; i--) {
            if (tag.equals(d.getChildAt(i).getTag())) d.removeViewAt(i);
        }
    }

    private static boolean hasTagged(Activity a, String tag) {
        ViewGroup d = decor(a);
        if (d == null) return false;
        for (int i = 0; i < d.getChildCount(); i++) {
            if (tag.equals(d.getChildAt(i).getTag())) return true;
        }
        return false;
    }

    private static void addCover(Activity a) {
        if (hasTagged(a, COVER_TAG)) return;
        ViewGroup d = decor(a);
        if (d == null) return;
        M3Theme.Palette p = M3Theme.palette(a);
        FrameLayout cover = new FrameLayout(a);
        cover.setTag(COVER_TAG);
        cover.setBackgroundColor(p.surface | 0xFF000000);
        TextView logo = new TextView(a);
        logo.setText("YunSeek");
        logo.setTextSize(28);
        logo.setTextColor(p.primary);
        cover.addView(logo, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        d.addView(cover, new ViewGroup.LayoutParams(-1, -1));
    }

    // ── app lock ─────────────────────────────────────────────────────────────

    private static void showLock(final Activity a) {
        if (!hasTagged(a, LOCK_TAG)) {
            ViewGroup d = decor(a);
            if (d == null) return;
            M3Theme.Palette p = M3Theme.palette(a);
            LinearLayout box = new LinearLayout(a);
            box.setTag(LOCK_TAG);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setClickable(true);
            box.setFocusable(true);
            box.setBackgroundColor(p.background | 0xFF000000);
            TextView title = new TextView(a);
            title.setText("YunSeek 已锁定");
            title.setTextSize(24);
            title.setTextColor(p.onSurface);
            box.addView(title);
            TextView sub = new TextView(a);
            sub.setText("验证身份以继续使用");
            sub.setTextColor(p.onSurfaceVariant);
            sub.setPadding(0, M3Theme.dp(a, 8), 0, M3Theme.dp(a, 24));
            box.addView(sub);
            Button unlock = m3Button(a, p, "解锁");
            unlock.setOnClickListener(v -> authenticate(a, "解锁 YunSeek", () -> unlockNow(a)));
            box.addView(unlock);
            Button pin = m3Button(a, p, "使用 PIN");
            pin.setOnClickListener(v -> pinPrompt(a, "输入 PIN", () -> unlockNow(a)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = M3Theme.dp(a, 12);
            box.addView(pin, lp);
            d.addView(box, new ViewGroup.LayoutParams(-1, -1));
        }
        if (!authInProgress && !autoPrompted) {
            autoPrompted = true;
            a.getWindow().getDecorView().post(
                    () -> authenticate(a, "解锁 YunSeek", () -> unlockNow(a)));
        }
    }

    private static void unlockNow(Activity a) {
        locked = false;
        lastPauseAt = 0;
        removeTagged(a, LOCK_TAG);
    }

    private static Button m3Button(Activity a, M3Theme.Palette p, String text) {
        Button b = new Button(a);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(p.onPrimary);
        b.setBackground(M3Theme.rounded(p.primary, M3Theme.dp(a, 20)));
        b.setPadding(M3Theme.dp(a, 24), 0, M3Theme.dp(a, 24), 0);
        b.setMinWidth(M3Theme.dp(a, 160));
        return b;
    }

    /** Device credential / biometric first, PIN fallback. Used by 应用锁 and 显示隐藏对话. */
    static void authenticate(final Activity a, final String title, final Runnable onSuccess) {
        if (authInProgress) return;
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                android.hardware.biometrics.BiometricPrompt.Builder b =
                        new android.hardware.biometrics.BiometricPrompt.Builder(a)
                                .setTitle(title);
                if (Build.VERSION.SDK_INT >= 30) {
                    b.setAllowedAuthenticators(
                            android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
                            | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
                } else if (Build.VERSION.SDK_INT >= 29) {
                    b.setDeviceCredentialAllowed(true);
                } else {
                    b.setNegativeButton("使用 PIN", a.getMainExecutor(),
                            (dlg, which) -> pinPrompt(a, title, onSuccess));
                }
                authInProgress = true;
                b.build().authenticate(new CancellationSignal(), a.getMainExecutor(),
                        new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                            @Override public void onAuthenticationSucceeded(
                                    android.hardware.biometrics.BiometricPrompt.AuthenticationResult r) {
                                authInProgress = false;
                                onSuccess.run();
                            }
                            @Override public void onAuthenticationError(int code, CharSequence msg) {
                                authInProgress = false;
                            }
                        });
                return;
            } catch (Throwable t) {
                authInProgress = false;
                Main.log("biometric unavailable, using PIN: " + t);
            }
        }
        pinPrompt(a, title, onSuccess);
    }

    static void pinPrompt(final Activity a, String title, final Runnable onSuccess) {
        if (!hasPin()) {
            Toast.makeText(a, "尚未设置 PIN", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = pinInput(a);
        final AlertDialog dlg = new AlertDialog.Builder(a).setTitle(title).setView(wrap(a, input))
                .setPositiveButton("确定", null).setNegativeButton("取消", null).create();
        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (checkPin(input.getText().toString())) { dlg.dismiss(); onSuccess.run(); }
            else { input.setText(""); input.setError("PIN 不正确"); }
        }));
        dlg.show();
        M3Theme.styleDialog(dlg, a);
    }

    static void pinSetup(final Activity a, final Runnable onDone) {
        final EditText input = pinInput(a);
        final AlertDialog dlg = new AlertDialog.Builder(a).setTitle("设置 4–6 位 PIN")
                .setMessage("用于应用锁和显示隐藏对话，仅以加盐 SHA-256 保存在本机。")
                .setView(wrap(a, input))
                .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (setPin(input.getText().toString())) {
                dlg.dismiss();
                if (onDone != null) onDone.run();
            } else input.setError("请输入 4–6 位数字");
        }));
        dlg.show();
        M3Theme.styleDialog(dlg, a);
    }

    private static EditText pinInput(Activity a) {
        EditText e = new EditText(a);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        e.setGravity(Gravity.CENTER);
        return e;
    }

    private static View wrap(Activity a, View v) {
        FrameLayout f = new FrameLayout(a);
        int pad = M3Theme.dp(a, 24);
        f.setPadding(pad, M3Theme.dp(a, 8), pad, 0);
        f.addView(v);
        return f;
    }

    // ── settings rows (账号与隐私) ─────────────────────────────────────────────

    interface RowFactory { View divider(); }

    static void addSettingsRows(final Activity act, LinearLayout card, int textColor,
                                int subColor, boolean dark, RowFactory rows) {
        card.addView(DeekseepUi.simpleSwitchRow(act, "后台防窥",
                "最近任务中隐藏 DeepSeek 内容（Android 13+ 关闭任务快照，旧版本暂停时遮罩）。",
                recentsGuardEnabled(), textColor, subColor, dark,
                (b, c) -> put("recentsGuard", c)));
        card.addView(rows.divider());
        card.addView(DeekseepUi.simpleSwitchRow(act, "防截屏",
                "开启后 DeepSeek 窗口禁止截屏和录屏（FLAG_SECURE）。",
                noScreenshotEnabled(), textColor, subColor, dark,
                (b, c) -> {
                    put("noScreenshot", c);
                    try { onResumed(act); } catch (Throwable ignored) {}
                }));
        card.addView(rows.divider());
        card.addView(DeekseepUi.simpleSwitchRow(act, "应用锁",
                "离开超过设定时间后回到 DeepSeek 需验证指纹/锁屏密码或 PIN。点此行下方可调整时间。",
                appLockEnabled(), textColor, subColor, dark,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(final CompoundButton b, boolean c) {
                        if (!c) { put("appLock", false); locked = false; return; }
                        if (hasPin()) { put("appLock", true); locked = false; return; }
                        b.setChecked(false);
                        pinSetup(act, () -> { put("appLock", true); locked = false; b.setChecked(true); });
                    }
                }));
        final TextView minutes = new TextView(act);
        minutes.setTextColor(subColor);
        minutes.setPadding(DeekseepUi.dp(act, 16), 0, DeekseepUi.dp(act, 16), DeekseepUi.dp(act, 12));
        final int[] options = {0, 1, 5};
        minutes.setText(lockLabel());
        minutes.setOnClickListener(v -> {
            int cur = lockMinutes(), next = 0;
            for (int i = 0; i < options.length; i++) {
                if (options[i] == cur) { next = options[(i + 1) % options.length]; break; }
            }
            put("lockMinutes", next);
            minutes.setText(lockLabel());
        });
        card.addView(minutes);
        TextView changePin = new TextView(act);
        changePin.setTextColor(subColor);
        changePin.setText("修改 PIN ›");
        changePin.setPadding(DeekseepUi.dp(act, 16), 0, DeekseepUi.dp(act, 16), DeekseepUi.dp(act, 12));
        changePin.setOnClickListener(v -> {
            if (hasPin()) authenticate(act, "验证当前身份", () -> pinSetup(act, null));
            else pinSetup(act, null);
        });
        card.addView(changePin);
        card.addView(rows.divider());
        card.addView(DeekseepUi.simpleSwitchRow(act, "纯黑 AMOLED",
                "深色模式下将窗口背景和模块页面设为纯黑 #000000（尽力而为）。",
                amoledEnabled(), textColor, subColor, dark,
                (b, c) -> put("amoled", c)));
    }

    private static String lockLabel() {
        int m = lockMinutes();
        return "锁定时间：" + (m == 0 ? "立即" : m + " 分钟") + "（点按切换 立即/1/5 分钟）";
    }

    /** 显示隐藏对话: turning it on requires the same PIN/biometric as 应用锁. */
    static void setShowHidden(final Activity act, boolean show, final Runnable after) {
        if (!show) {
            put("showHidden", false);
            showHiddenUnlocked = false;
            if (after != null) after.run();
            return;
        }
        Runnable grant = () -> {
            put("showHidden", true);
            showHiddenUnlocked = true;
            if (after != null) after.run();
        };
        if (hasPin()) authenticate(act, "显示隐藏对话", grant);
        else pinSetup(act, grant);
    }

    static boolean showHidden() { return showHiddenUnlocked && bool("showHidden"); }
}
