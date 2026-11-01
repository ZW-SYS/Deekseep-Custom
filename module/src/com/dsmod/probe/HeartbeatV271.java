package com.dsmod.probe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.File;

/**
 * AI 心跳 on DeepSeek 2.7.x (YunSeek 1.8.1.1, experimental, default off).
 *
 * <p>Open builds have no Local API, so a heartbeat can only speak through DeepSeek's own chat
 * screen (the same native send bridge the Agent uses). When the alarm fires while the bound chat
 * is not open, we post one quiet notification instead; tapping it opens DeepSeek on that chat and
 * the heartbeat is sent there, visibly, as a normal message.</p>
 *
 * <p>Rate limit (same idea as the Agent limiter, to stay far from abnormal-traffic patterns):
 * at least {@link #MIN_GAP_MS} between two heartbeats and at most {@link #MAX_PER_DAY} per
 * calendar day. Reminders the user set explicitly are not counted.</p>
 */
final class HeartbeatV271 {
    private HeartbeatV271() {}

    static final long MIN_GAP_MS = 15L * 60L * 1000L;
    static final int MAX_PER_DAY = 12;
    private static final String RATE_FILE =
            "/data/data/com.deepseek.chat/files/deekseep_heartbeat_rate.txt";
    private static final String CHANNEL = "yunseek_heartbeat_invite";
    static final String EXTRA_PENDING_HEARTBEAT = "yunseek_pending_heartbeat";

    /** Pending heartbeat waiting for its chat to be opened (sid), or null. */
    static volatile String pendingSid;

    static boolean applies() {
        return HostCompat.isV271() && !BuildInfo.LOCAL_API_INCLUDED;
    }

    /** True and records the attempt when a heartbeat may run now. */
    static synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long last = 0L;
        String day = "";
        int count = 0;
        String stored = Main.readSmallText(RATE_FILE);
        if (stored != null) {
            String[] parts = stored.trim().split(",");
            try {
                if (parts.length >= 3) {
                    last = Long.parseLong(parts[0]);
                    day = parts[1];
                    count = Integer.parseInt(parts[2]);
                }
            } catch (Throwable ignored) {}
        }
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ROOT)
                .format(new java.util.Date(now));
        if (!today.equals(day)) { day = today; count = 0; }
        if (last > 0 && now - last >= 0 && now - last < MIN_GAP_MS) {
            Main.log("heartbeat rate limit: too soon since last");
            return false;
        }
        if (count >= MAX_PER_DAY) {
            Main.log("heartbeat rate limit: daily cap reached");
            return false;
        }
        try {
            Main.overwriteTextFile(RATE_FILE, now + "," + day + "," + (count + 1));
        } catch (Throwable ignored) {}
        return true;
    }

    /** Posts one notification that opens DeepSeek on the bound chat. */
    static void postInvite(Context context, String sid) {
        if (context == null || sid == null || sid.length() == 0) return;
        try {
            pendingSid = sid;
            try { Main.overwriteTextFile(pendingFile(), sid); } catch (Throwable ignored) {}
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(CHANNEL,
                        "YunSeek AI 心跳", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("AI 心跳到点时提醒你打开对话");
                nm.createNotificationChannel(channel);
            }
            Intent open = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            if (open == null) return;
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra(ProactiveHeartbeatReceiver.EXTRA_CONVERSATION_ID, sid);
            open.putExtra(EXTRA_PENDING_HEARTBEAT, true);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getActivity(context, 0x59534B, open, flags);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL)
                    : new Notification.Builder(context);
            b.setSmallIcon(context.getApplicationInfo().icon)
                    .setContentTitle("DeepSeek 想和你聊聊")
                    .setContentText("点此打开对话（AI 心跳 · 实验性）")
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            nm.notify(0x59534B, b.build());
            Main.log("heartbeat invite posted sid=" + sid);
        } catch (Throwable t) {
            Main.log("heartbeat invite failed: " + Main.safeThrowableMessage(t));
        }
    }

    private static String pendingFile() {
        return "/data/data/com.deepseek.chat/files/deekseep_heartbeat_pending.txt";
    }

    /** Called after the chat for {@code sid} has been opened from the invite. */
    static void onChatOpened(final Context context, final String sid) {
        String wanted = pendingSid;
        if (wanted == null) wanted = Main.readSmallText(pendingFile());
        if (wanted == null || !wanted.trim().equals(sid)) return;
        pendingSid = null;
        try { new File(pendingFile()).delete(); } catch (Throwable ignored) {}
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                HookAccountLoginSecurity.runPendingHeartbeat(context, sid);
            }
        }, 1500L);
    }
}
