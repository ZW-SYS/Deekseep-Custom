package com.dsmod.probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Semi-automatic Agent delivery (default).
 *
 * <p>Every Agent step is a real message sent from the user's account. In manual mode a tool
 * result is never sent on its own: a confirmation card shows a preview and the user taps
 * “发送” for each step, so every turn is the user's decision at the user's pace. Full-auto
 * delivery is off by default and can only be enabled after reading a risk notice and ticking
 * an acknowledgement box. Nothing here disguises or hides the requests; the server receives
 * exactly the same message either way.</p>
 */
final class AgentManualGate {
    private AgentManualGate() {}

    static final int ALLOW = 1;
    static final int WAIT = 0;
    static final int DECLINED = -1;

    private static final String FILE_PATH =
            "/data/data/com.deepseek.chat/files/deekseep_agent/delivery_mode.txt";
    private static final String MODE_AUTO = "auto_ack_v1";

    private static final Set<String> PENDING = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> APPROVED = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> REFUSED = Collections.synchronizedSet(new HashSet<String>());

    private static volatile Boolean cachedAuto;

    /** True only when the user enabled full-auto after acknowledging the risk notice. */
    static boolean isAuto() {
        Boolean present = cachedAuto;
        if (present != null) return present.booleanValue();
        boolean auto = false;
        FileInputStream in = null;
        try {
            File file = new File(FILE_PATH);
            if (file.isFile() && file.length() < 256) {
                in = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                int read = in.read(data);
                auto = read > 0 && MODE_AUTO.equals(new String(data, 0, read, "UTF-8").trim());
            }
        } catch (Throwable ignored) {
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
        }
        cachedAuto = Boolean.valueOf(auto);
        return auto;
    }

    static boolean setAuto(boolean auto) {
        FileOutputStream out = null;
        try {
            File file = new File(FILE_PATH);
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return false;
            out = new FileOutputStream(file, false);
            out.write((auto ? MODE_AUTO : "manual").getBytes("UTF-8"));
            out.flush();
            cachedAuto = Boolean.valueOf(auto);
            return true;
        } catch (Throwable error) {
            Main.log("agent delivery mode save failed: " + error);
            return false;
        } finally {
            if (out != null) try { out.close(); } catch (Throwable ignored) {}
        }
    }

    /**
     * Asks whether the Agent turn identified by {@code key} may be sent now. In manual mode the
     * first call shows the confirmation card and returns WAIT; callers poll again later.
     */
    static int check(Context context, final String key, final String preview) {
        if (isAuto()) return ALLOW;
        if (APPROVED.remove(key)) return ALLOW;
        if (REFUSED.remove(key)) return DECLINED;
        if (PENDING.contains(key)) return WAIT;
        final Activity activity = Main.currentHostActivityForAgent();
        if (activity == null || activity.isFinishing()) return WAIT;
        PENDING.add(key);
        Runnable show = new Runnable() {
            @Override public void run() {
                try {
                    showCard(activity, key, preview);
                } catch (Throwable error) {
                    PENDING.remove(key);
                    Main.log("agent manual card failed: " + error);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) show.run();
        else new Handler(Looper.getMainLooper()).post(show);
        return WAIT;
    }

    private static void showCard(final Activity activity, final String key, String preview) {
        String body = preview == null ? "" : preview.trim();
        if (body.length() > 600) body = body.substring(0, 600) + "\n…";
        final String fullText = preview == null ? "" : preview;
        String message = UiLanguage.text(activity,
                "Agent 想把下面的工具结果发回 DeepSeek，让模型继续下一步。\n"
                        + "点“发送”才会发出；点“稍后”会留在 Agent 运行记录里，随时可重试。\n\n",
                "The Agent wants to send this tool result back to DeepSeek so the model can "
                        + "continue. Nothing is sent until you tap Send; Later keeps it in "
                        + "Agent runs for retry.\n\n") + body;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(UiLanguage.text(activity, "发送 Agent 结果？", "Send Agent result?"))
                .setMessage(message)
                .setPositiveButton(UiLanguage.text(activity, "发送", "Send"),
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                APPROVED.add(key);
                                PENDING.remove(key);
                            }
                        })
                .setNeutralButton(UiLanguage.text(activity, "复制并稍后", "Copy, later"),
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                try {
                                    ClipboardManager clipboard = (ClipboardManager)
                                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(
                                            ClipData.newPlainText("agent_result", fullText));
                                    Toast.makeText(activity, UiLanguage.text(activity,
                                            "已复制工具结果", "Tool result copied"),
                                            Toast.LENGTH_SHORT).show();
                                } catch (Throwable ignored) {}
                                REFUSED.add(key);
                                PENDING.remove(key);
                            }
                        })
                .setNegativeButton(UiLanguage.text(activity, "稍后", "Later"),
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                REFUSED.add(key);
                                PENDING.remove(key);
                            }
                        })
                .create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel(DialogInterface d) {
                REFUSED.add(key);
                PENDING.remove(key);
            }
        });
        dialog.show();
    }

    /** Risk notice shown before full-auto can be switched on. Calls back with the outcome. */
    static void confirmEnableAuto(final Activity activity, final Runnable onEnabled,
                                  final Runnable onCancelled) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView text = new TextView(activity);
        text.setTextSize(14);
        text.setLineSpacing(0f, 1.15f);
        text.setText(UiLanguage.text(activity,
                "⚠ 高风险，请完整阅读\n\n"
                        + "全自动模式下，Agent 每执行一步工具，就会不经你确认、自动用你的账号向 "
                        + "DeepSeek 发一条消息。\n\n"
                        + "已有用户反馈：DeepSeek 2.4.1 及以后版本，使用全自动 Agent 后账号很快被强制"
                        + "退出登录、要求重新验证，甚至被封禁数天。原版模块同样如此。\n\n"
                        + "本模块不会、也不打算伪装或绕过 DeepSeek 的任何检测。开启后产生的一切后果"
                        + "（掉线、验证、封号）由你自行承担。重要账号请勿开启。\n\n"
                        + "默认的半自动模式：每一步结果都弹窗让你点“发送”。",
                "⚠ High risk — read fully\n\n"
                        + "In full-auto mode every Agent step sends a message from your account "
                        + "without asking you.\n\nUsers report that on DeepSeek 2.4.1+ full-auto "
                        + "use quickly logs the account out, requires re-verification or gets it "
                        + "suspended for days — the original module too.\n\nThis module does not "
                        + "and will not disguise or bypass DeepSeek's checks. You bear all "
                        + "consequences. Do not enable it on accounts that matter.\n\n"
                        + "Default semi-auto mode asks you to tap Send for every step."));
        box.addView(text);
        final CheckBox ack = new CheckBox(activity);
        ack.setText(UiLanguage.text(activity,
                "我已阅读并理解上述风险，自愿开启", "I have read and accept these risks"));
        box.addView(ack);
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.addView(box);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(UiLanguage.text(activity, "开启全自动 Agent？", "Enable full-auto Agent?"))
                .setView(scroll)
                .setPositiveButton(UiLanguage.text(activity, "开启", "Enable"), null)
                .setNegativeButton(UiLanguage.text(activity, "取消", "Cancel"), null)
                .create();
        final boolean[] decided = {false};
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) {
                if (!decided[0] && onCancelled != null) onCancelled.run();
            }
        });
        dialog.show();
        final android.widget.Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setEnabled(false);
        ack.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean c) {
                positive.setEnabled(c);
            }
        });
        positive.setOnClickListener(new android.view.View.OnClickListener() {
            @Override public void onClick(android.view.View v) {
                if (!ack.isChecked()) return;
                if (setAuto(true)) {
                    decided[0] = true;
                    if (onEnabled != null) onEnabled.run();
                }
                dialog.dismiss();
            }
        });
    }
}
