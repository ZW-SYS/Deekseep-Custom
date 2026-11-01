package com.dsmod.probe;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * 草稿自动保存与恢复 (YunSeek 1.8.1.1). Purely local.
 *
 * <p>DeepSeek 2.7.x draws its input box with Jetpack Compose, so there is no EditText to read.
 * Compose exposes the same semantics tree that accessibility uses through the root view's
 * {@link AccessibilityNodeProvider}; we read the editable node's text from there every few
 * seconds and write it back with the standard {@code ACTION_SET_TEXT} semantics action (cursor
 * moved to the end with {@code ACTION_SET_SELECTION}). No host class is touched.</p>
 *
 * <p>Drafts are keyed by conversation id ("new" for a not-yet-created chat). A draft is removed
 * once the box becomes empty after holding text, which is what a successful send does.</p>
 */
final class DraftKeeper {
    private DraftKeeper() {}

    private static final String FILE = "/data/data/com.deepseek.chat/files/yunseek_drafts.json";
    private static final String FLAG = "/data/data/com.deepseek.chat/files/yunseek_draft_disabled";
    private static final long PERIOD_MS = 3000L;
    private static final int MAX_DRAFT = 20000;
    private static final int MAX_DRAFTS = 200;

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static Activity current;
    private static String lastSid;
    private static String lastText = "";
    private static boolean restoredForSid;
    private static JSONObject drafts;

    static boolean enabled() {
        return !new java.io.File(FLAG).exists();
    }

    static boolean setEnabled(boolean on) {
        try {
            if (on) new java.io.File(FLAG).delete();
            else Main.overwriteTextFile(FLAG, "1");
        } catch (Throwable ignored) {}
        return enabled() == on;
    }

    private static final Runnable TICK = new Runnable() {
        @Override public void run() {
            try { tick(); } catch (Throwable t) { Main.log("draft tick failed: " + t); }
            if (current != null) H.postDelayed(this, PERIOD_MS);
        }
    };

    static void onResumed(Activity act) {
        if (act == null || !enabled()) return;
        if (!act.getClass().getName().endsWith("MainActivity")) return;
        current = act;
        H.removeCallbacks(TICK);
        H.postDelayed(TICK, 1200L);
    }

    static void onPaused(Activity act) {
        if (act != null && act == current) {
            try { tick(); } catch (Throwable ignored) {}
            current = null;
            H.removeCallbacks(TICK);
        }
    }

    private static String currentSid() {
        String sid = HookSessionManagement.sidebarCurrentSid;
        if (sid == null || sid.length() == 0) sid = HookChatPipeline.lastInteractiveConversationId;
        return sid == null || sid.length() == 0 ? "new" : sid;
    }

    private static void tick() {
        Activity act = current;
        if (act == null || act.isFinishing()) return;
        Target input = findInput(act.getWindow().getDecorView());
        if (input == null) return;
        String sid = currentSid();
        String text = input.text;
        load();
        if (!sid.equals(lastSid)) {
            // Conversation switched: never save on the switch tick (the box may still hold the
            // previous chat's text); restore once if the new chat's box is empty.
            lastSid = sid;
            restoredForSid = false;
            lastText = text;
            String saved = drafts.optString(sid, "");
            if (text.length() == 0 && saved.length() > 0) {
                if (input.setText(saved)) {
                    restoredForSid = true;
                    lastText = saved;
                    Main.log("draft restored sid=" + sid + " len=" + saved.length());
                }
            }
            return;
        }
        if (text.equals(lastText)) return;
        boolean wasNonEmpty = lastText.length() > 0;
        lastText = text;
        if (text.trim().length() == 0) {
            if (wasNonEmpty && drafts.has(sid)) {
                drafts.remove(sid);
                save();
            }
            return;
        }
        if (text.length() > MAX_DRAFT) text = text.substring(0, MAX_DRAFT);
        try {
            drafts.put(sid, text);
            trim();
            save();
        } catch (Throwable ignored) {}
    }

    private static void trim() {
        if (drafts.length() <= MAX_DRAFTS) return;
        Iterator<String> keys = drafts.keys();
        int extra = drafts.length() - MAX_DRAFTS;
        java.util.List<String> drop = new java.util.ArrayList<String>();
        while (keys.hasNext() && drop.size() < extra) drop.add(keys.next());
        for (String k : drop) drafts.remove(k);
    }

    private static void load() {
        if (drafts != null) return;
        try {
            String raw = Main.readSmallText(FILE);
            drafts = raw == null || raw.trim().length() == 0 ? new JSONObject() : new JSONObject(raw);
        } catch (Throwable t) {
            drafts = new JSONObject();
        }
    }

    private static void save() {
        try { Main.overwriteTextFile(FILE, drafts.toString()); } catch (Throwable ignored) {}
    }

    // ── Compose semantics access ────────────────────────────────────────────

    private static final class Target {
        final AccessibilityNodeProvider provider;
        final int virtualId;
        final String text;

        Target(AccessibilityNodeProvider provider, int virtualId, String text) {
            this.provider = provider;
            this.virtualId = virtualId;
            this.text = text == null ? "" : text;
        }

        boolean setText(String value) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
            boolean ok = provider.performAction(virtualId, AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (ok) {
                Bundle sel = new Bundle();
                sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, value.length());
                sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, value.length());
                try {
                    provider.performAction(virtualId, AccessibilityNodeInfo.ACTION_SET_SELECTION, sel);
                } catch (Throwable ignored) {}
            }
            return ok;
        }
    }

    private static Target findInput(View root) {
        View compose = findComposeView(root, 0);
        if (compose == null) return null;
        AccessibilityNodeProvider provider = compose.getAccessibilityNodeProvider();
        if (provider == null) return null;
        // Fast path: the input keeps its semantics id while its screen stays composed.
        if (cachedInputId != Integer.MIN_VALUE) {
            Target cached = editableAt(provider, cachedInputId);
            if (cached != null) return cached;
            cachedInputId = Integer.MIN_VALUE;
        }
        AccessibilityNodeInfo rootNode = provider.createAccessibilityNodeInfo(View.NO_ID);
        if (rootNode == null) return null;
        if (!childIdsReadable(rootNode)) return scan(provider);
        ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        enqueueChildren(rootNode, queue, 0);
        int visited = 0;
        while (!queue.isEmpty() && visited < 600) {
            int[] item = queue.poll();
            visited++;
            AccessibilityNodeInfo node = provider.createAccessibilityNodeInfo(item[0]);
            if (node == null) continue;
            if (node.isEditable() && node.isVisibleToUser()) {
                cachedInputId = item[0];
                return new Target(provider, item[0], textOf(node));
            }
            if (item[1] < 40) enqueueChildren(node, queue, item[1] + 1);
        }
        return null;
    }

    private static int cachedInputId = Integer.MIN_VALUE;
    private static long lastScanAt;

    private static String textOf(AccessibilityNodeInfo node) {
        CharSequence t = node.getText();
        // An empty field reports its placeholder as hint (API 26+), not as text.
        String value = t == null ? "" : t.toString();
        if (android.os.Build.VERSION.SDK_INT >= 26 && node.isShowingHintText()) value = "";
        return value;
    }

    private static Target editableAt(AccessibilityNodeProvider provider, int id) {
        try {
            AccessibilityNodeInfo node = provider.createAccessibilityNodeInfo(id);
            if (node != null && node.isEditable() && node.isVisibleToUser()) {
                return new Target(provider, id, textOf(node));
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean childIdsReadable(AccessibilityNodeInfo node) {
        if (node.getChildCount() == 0) return true;
        ArrayDeque<int[]> probe = new ArrayDeque<int[]>();
        enqueueChildren(node, probe, 0);
        return !probe.isEmpty();
    }

    /** Fallback when child ids are unreadable: scan semantics ids, at most every 15 s. */
    private static Target scan(AccessibilityNodeProvider provider) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastScanAt < 15000L) return null;
        lastScanAt = now;
        for (int id = 1; id <= 4000; id++) {
            Target t = editableAt(provider, id);
            if (t != null) { cachedInputId = id; return t; }
        }
        return null;
    }

    /** Virtual child ids are not public API; read them through the documented string form. */
    private static void enqueueChildren(AccessibilityNodeInfo node, ArrayDeque<int[]> queue, int depth) {
        try {
            java.lang.reflect.Method m = AccessibilityNodeInfo.class.getDeclaredMethod("getChildId", int.class);
            m.setAccessible(true);
            for (int i = 0; i < node.getChildCount(); i++) {
                long id = (Long) m.invoke(node, i);
                int virtual = (int) (id >> 32);
                queue.add(new int[]{virtual, depth});
            }
        } catch (Throwable t) {
            // Hidden API blocked: no traversal possible on this device.
        }
    }

    private static View findComposeView(View v, int depth) {
        if (v == null || depth > 12) return null;
        if (v.getClass().getName().contains("AndroidComposeView")) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = findComposeView(g.getChildAt(i), depth + 1);
                if (f != null) return f;
            }
        }
        return null;
    }
}
