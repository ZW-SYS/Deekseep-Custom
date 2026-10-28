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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Per-conversation tools added in the 1.8.0 port: copy a whole conversation, export one
 * conversation as Markdown or plain text, share it through the Android share sheet, and search
 * inside a single conversation.
 *
 * <p>Everything here reads DeepSeek's local WCDB database read-only through the platform SQLite
 * API (tables {@code chat_session_list} and {@code chat_session_messages_<id>}, re-verified on
 * 2.7.1/code287). No host classes are touched, so these tools do not depend on the R8 symbol
 * table and work on every supported host generation. Only conversations already downloaded to
 * the device are listed.</p>
 */
final class ConversationTools {
    /** Binder transactions are capped around 1 MB; stay well below it for clipboard/share. */
    private static final int MAX_CLIP_CHARS = 400_000;
    private static final int MAX_SHARE_CHARS = 100_000;

    static final class Session {
        final String id;
        final String title;
        final double updatedAt;

        Session(String id, String title, double updatedAt) {
            this.id = id;
            this.title = title == null || title.trim().length() == 0 ? id : title.trim();
            this.updatedAt = updatedAt;
        }

        @Override public String toString() {
            String when = updatedAt > 0
                    ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .format(new Date((long) (updatedAt < 1e11 ? updatedAt * 1000d : updatedAt)))
                    : "";
            return title + (when.length() > 0 ? "\n" + when : "");
        }
    }

    private ConversationTools() {}

    // ── entry ────────────────────────────────────────────────────────────────

    static void show(final Activity act) {
        if (act == null) return;
        new Thread(new Runnable() {
            @Override public void run() {
                final List<Session> sessions = loadSessions();
                act.runOnUiThread(new Runnable() {
                    @Override public void run() { showPicker(act, sessions); }
                });
            }
        }, "Deekseep-ConvTools").start();
    }

    private static void showPicker(final Activity act, final List<Session> sessions) {
        if (act.isFinishing()) return;
        if (sessions.isEmpty()) {
            toast(act, UiLanguage.text(act, "本机没有可用的会话记录",
                    "No conversations stored on this device"));
            return;
        }
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(act, 16);
        box.setPadding(pad, dp(act, 8), pad, 0);
        final EditText filter = new EditText(act);
        filter.setSingleLine(true);
        filter.setHint(UiLanguage.text(act, "按标题筛选", "Filter by title"));
        box.addView(filter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final ListView list = new ListView(act);
        final List<Session> shown = new ArrayList<>(sessions);
        final ArrayAdapter<Session> adapter = new ArrayAdapter<Session>(
                act, android.R.layout.simple_list_item_1, shown);
        list.setAdapter(adapter);
        box.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 380)));
        final AlertDialog dialog = new AlertDialog.Builder(act)
                .setTitle(UiLanguage.text(act, "会话工具：选择会话", "Conversation tools: pick one"))
                .setView(box)
                .setNegativeButton(UiLanguage.text(act, "关闭", "Close"), null)
                .create();
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                for (Session session : sessions) {
                    if (q.length() == 0
                            || session.title.toLowerCase(Locale.ROOT).contains(q)) {
                        shown.add(session);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                if (pos < 0 || pos >= shown.size()) return;
                showActions(act, shown.get(pos));
            }
        });
        dialog.show();
    }

    private static void showActions(final Activity act, final Session session) {
        String[] items = {
                UiLanguage.text(act, "复制整段对话", "Copy whole conversation"),
                UiLanguage.text(act, "导出为 Markdown (.md)", "Export as Markdown (.md)"),
                UiLanguage.text(act, "导出为纯文本 (.txt)", "Export as plain text (.txt)"),
                UiLanguage.text(act, "分享…", "Share…"),
                UiLanguage.text(act, "在此会话中搜索", "Search in this conversation"),
        };
        new AlertDialog.Builder(act)
                .setTitle(session.title)
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        switch (which) {
                            case 0: copyConversation(act, session); break;
                            case 1: exportConversation(act, session, true); break;
                            case 2: exportConversation(act, session, false); break;
                            case 3: shareConversation(act, session); break;
                            case 4: promptSearch(act, session); break;
                            default: break;
                        }
                    }
                })
                .setNegativeButton(UiLanguage.text(act, "取消", "Cancel"), null)
                .show();
    }

    // ── actions ──────────────────────────────────────────────────────────────

    private static void copyConversation(final Activity act, final Session session) {
        runBg(act, new Job() {
            @Override public String run() {
                String text = render(session, false);
                if (text == null) return UiLanguage.text("读取会话失败", "Could not read conversation");
                if (text.length() > MAX_CLIP_CHARS) {
                    return UiLanguage.text("对话过长（" + text.length() + " 字），请改用导出",
                            "Conversation too long (" + text.length() + " chars); use export");
                }
                final String clip = text;
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            ClipboardManager cm = (ClipboardManager)
                                    act.getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText(session.title, clip));
                        } catch (Throwable t) {
                            toast(act, "copy failed: " + t);
                        }
                    }
                });
                return UiLanguage.text("已复制 " + text.length() + " 字",
                        "Copied " + text.length() + " chars");
            }
        });
    }

    private static void exportConversation(final Activity act, final Session session,
                                           final boolean markdown) {
        runBg(act, new Job() {
            @Override public String run() throws Throwable {
                String text = render(session, markdown);
                if (text == null) return UiLanguage.text("读取会话失败", "Could not read conversation");
                File dir = new File(exportBase(act), "conversations");
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    return UiLanguage.text("无法创建导出目录", "Cannot create export folder");
                }
                File out = new File(dir, safeName(session.title) + "_" + shortId(session.id) + "_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
                        + (markdown ? ".md" : ".txt"));
                OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
                try { w.write(text); } finally { w.close(); }
                return UiLanguage.text("已导出：", "Exported: ") + out.getAbsolutePath();
            }
        });
    }

    private static void shareConversation(final Activity act, final Session session) {
        runBg(act, new Job() {
            @Override public String run() {
                String text = render(session, true);
                if (text == null) return UiLanguage.text("读取会话失败", "Could not read conversation");
                boolean truncated = text.length() > MAX_SHARE_CHARS;
                final String body = truncated ? text.substring(0, MAX_SHARE_CHARS)
                        + UiLanguage.text("\n\n…（内容过长已截断，完整内容请使用导出）",
                        "\n\n…(truncated; use export for the full text)") : text;
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            Intent send = new Intent(Intent.ACTION_SEND);
                            send.setType("text/plain");
                            send.putExtra(Intent.EXTRA_SUBJECT, session.title);
                            send.putExtra(Intent.EXTRA_TEXT, body);
                            act.startActivity(Intent.createChooser(send,
                                    UiLanguage.text(act, "分享对话", "Share conversation")));
                        } catch (Throwable t) {
                            toast(act, "share failed: " + t);
                        }
                    }
                });
                return truncated ? UiLanguage.text("内容较长，已截断分享",
                        "Long conversation: shared a truncated copy") : null;
            }
        });
    }

    private static void promptSearch(final Activity act, final Session session) {
        final EditText input = new EditText(act);
        input.setSingleLine(true);
        input.setHint(UiLanguage.text(act, "关键词", "Keyword"));
        int pad = dp(act, 20);
        LinearLayout box = new LinearLayout(act);
        box.setPadding(pad, dp(act, 8), pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(act)
                .setTitle(UiLanguage.text(act, "在会话中搜索", "Search in conversation"))
                .setView(box)
                .setNegativeButton(UiLanguage.text(act, "取消", "Cancel"), null)
                .setPositiveButton(UiLanguage.text(act, "搜索", "Search"),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                String kw = input.getText() == null ? ""
                                        : input.getText().toString().trim();
                                if (kw.length() > 0) runSearch(act, session, kw);
                            }
                        })
                .show();
    }

    static final class Hit {
        final int index;
        final String role;
        final String snippet;
        final String full;

        Hit(int index, String role, String snippet, String full) {
            this.index = index;
            this.role = role;
            this.snippet = snippet;
            this.full = full;
        }

        @Override public String toString() {
            return "#" + index + " " + role + "\n" + snippet;
        }
    }

    private static void runSearch(final Activity act, final Session session, final String kw) {
        new Thread(new Runnable() {
            @Override public void run() {
                final List<Hit> hits = new ArrayList<>();
                List<ChatEditorUi.Msg> thread = loadThread(session.id);
                if (thread != null) hits.addAll(search(thread, kw));
                act.runOnUiThread(new Runnable() {
                    @Override public void run() { showHits(act, session, kw, hits); }
                });
            }
        }, "Deekseep-ConvSearch").start();
    }

    /** Case-insensitive search over message bodies and reasoning; pure function for tests. */
    static List<Hit> search(List<ChatEditorUi.Msg> thread, String kw) {
        List<Hit> hits = new ArrayList<>();
        if (thread == null || kw == null || kw.length() == 0) return hits;
        String needle = kw.toLowerCase(Locale.ROOT);
        int index = 0;
        for (ChatEditorUi.Msg m : thread) {
            index++;
            String body = visibleBody(m);
            String think = m.think == null ? "" : m.think;
            String hay = body.toLowerCase(Locale.ROOT);
            int at = hay.indexOf(needle);
            String source = body;
            if (at < 0) {
                at = think.toLowerCase(Locale.ROOT).indexOf(needle);
                source = think;
            }
            if (at < 0) continue;
            int from = Math.max(0, at - 40);
            int to = Math.min(source.length(), at + needle.length() + 60);
            String snippet = (from > 0 ? "…" : "") + source.substring(from, to)
                    .replace('\n', ' ') + (to < source.length() ? "…" : "");
            hits.add(new Hit(index, roleLabel(m.role), snippet, body));
        }
        return hits;
    }

    private static void showHits(final Activity act, Session session, String kw,
                                 final List<Hit> hits) {
        if (act.isFinishing()) return;
        if (hits.isEmpty()) {
            toast(act, UiLanguage.text(act, "未找到“" + kw + "”", "No match for \"" + kw + "\""));
            return;
        }
        ListView list = new ListView(act);
        list.setAdapter(new ArrayAdapter<Hit>(act, android.R.layout.simple_list_item_1, hits));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                try {
                    ClipboardManager cm = (ClipboardManager)
                            act.getSystemService(Context.CLIPBOARD_SERVICE);
                    String full = hits.get(pos).full;
                    if (full.length() > MAX_CLIP_CHARS) full = full.substring(0, MAX_CLIP_CHARS);
                    cm.setPrimaryClip(ClipData.newPlainText("message", full));
                    toast(act, UiLanguage.text(act, "已复制该条消息", "Message copied"));
                } catch (Throwable ignored) {}
            }
        });
        new AlertDialog.Builder(act)
                .setTitle(UiLanguage.text(act, hits.size() + " 处匹配（点按复制整条）",
                        hits.size() + " matches (tap to copy message)"))
                .setView(list)
                .setPositiveButton(UiLanguage.text(act, "关闭", "Close"), null)
                .show();
    }

    // ── data ─────────────────────────────────────────────────────────────────

    static List<Session> loadSessions() {
        List<Session> out = new ArrayList<>();
        File db = currentDb();
        if (db == null) return out;
        SQLiteDatabase d = null;
        Cursor c = null;
        try {
            d = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            c = d.rawQuery("SELECT id,title,updated_at FROM chat_session_list"
                    + " ORDER BY updated_at DESC", null);
            while (c.moveToNext()) {
                String id = c.getString(0);
                if (id == null || id.length() == 0) continue;
                out.add(new Session(id, c.getString(1), c.isNull(2) ? 0d : c.getDouble(2)));
            }
        } catch (Throwable t) {
            Main.log("conversation tools: list failed: " + t);
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
            if (d != null) try { d.close(); } catch (Throwable ignored) {}
        }
        return out;
    }

    private static List<ChatEditorUi.Msg> loadThread(String sid) {
        File db = currentDb();
        if (db == null) return null;
        SQLiteDatabase d = null;
        try {
            d = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            return ChatEditorUi.loadThread(d, sid);
        } catch (Throwable t) {
            Main.log("conversation tools: thread failed: " + t);
            return null;
        } finally {
            if (d != null) try { d.close(); } catch (Throwable ignored) {}
        }
    }

    private static String render(Session session, boolean markdown) {
        List<ChatEditorUi.Msg> thread = loadThread(session.id);
        if (thread == null) return null;
        return format(session.title, thread, markdown);
    }

    /** Pure formatter, shared by copy/export/share; kept static for unit tests. */
    static String format(String title, List<ChatEditorUi.Msg> thread, boolean markdown) {
        StringBuilder sb = new StringBuilder();
        if (markdown) sb.append("# ").append(title).append("\n\n");
        else sb.append(title).append("\n").append(repeat('=', Math.min(40, Math.max(4,
                title.length())))).append("\n\n");
        for (ChatEditorUi.Msg m : thread) {
            String role = roleLabel(m.role);
            String body = visibleBody(m);
            String think = m.think == null ? "" : m.think.trim();
            if (markdown) {
                sb.append("**").append(role).append("**\n\n");
                if (think.length() > 0) {
                    for (String line : think.split("\n")) sb.append("> ").append(line).append('\n');
                    sb.append('\n');
                }
                sb.append(body).append("\n\n---\n\n");
            } else {
                sb.append('[').append(role).append("]\n");
                if (think.length() > 0) {
                    sb.append(UiLanguage.text("（思考）", "(reasoning)")).append('\n')
                            .append(think).append("\n\n");
                }
                sb.append(body).append("\n\n");
            }
        }
        return sb.toString();
    }

    private static String visibleBody(ChatEditorUi.Msg m) {
        String body = m.body == null ? "" : m.body;
        if ("USER".equals(m.role)) body = HistoryBridge.stripInjectedSystemPrompts(body);
        return body.trim();
    }

    private static String roleLabel(String role) {
        return "USER".equals(role) ? UiLanguage.text("用户", "User")
                : UiLanguage.text("助手", "Assistant");
    }

    private static File currentDb() {
        try {
            ClassLoader cl = Main.hostClassLoader;
            return cl != null ? ChatEditorUi.currentDb(cl) : ChatEditorUi.currentDb();
        } catch (Throwable t) {
            return null;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    interface Job { String run() throws Throwable; }

    private static void runBg(final Activity act, final Job job) {
        new Thread(new Runnable() {
            @Override public void run() {
                String msg;
                try { msg = job.run(); } catch (Throwable t) { msg = "Error: " + t; }
                if (msg == null) return;
                final String text = msg;
                act.runOnUiThread(new Runnable() {
                    @Override public void run() { toast(act, text); }
                });
            }
        }, "Deekseep-ConvJob").start();
    }

    private static File exportBase(Activity act) {
        File ext = null;
        try { ext = act.getExternalFilesDir(null); } catch (Throwable ignored) {}
        File base = new File(ext != null ? ext : new File("/data/data/com.deepseek.chat/files"),
                "deekseep");
        base.mkdirs();
        return base;
    }

    static String safeName(String s) {
        if (s == null) return "untitled";
        String r = s.replaceAll("[/\\\\:*?\"<>|\\r\\n\\t]", "_").trim();
        if (r.length() == 0) r = "untitled";
        if (r.length() > 60) r = r.substring(0, 60);
        return r;
    }

    private static String shortId(String sid) {
        if (sid == null) return "x";
        return sid.length() > 8 ? sid.substring(0, 8) : sid;
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    private static void toast(Context c, String text) {
        try { UiLanguage.toast(c, text, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) {}
    }
}
