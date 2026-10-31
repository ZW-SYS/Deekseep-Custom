package com.dsmod.probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地提示词库 page. Pure local: browse built-in categories (通用 / 代码审查 / 写作 / 翻译) and the
 * user's own prompts, add / edit / delete custom prompts, and "一键写入" a prompt as the active
 * injected system prompt (which also turns prompt injection on) through the existing feature.
 */
final class PromptLibraryUi {

    private PromptLibraryUi() {}

    static void show(final Activity act) {
        if (act == null || act.isFinishing()) return;
        UiLanguage.refreshHost(act);
        new Page(act).open();
    }

    private static final class Page {
        final Activity act;
        final M3Theme.Palette pal;
        final Dialog dialog;
        final LinearLayout root;
        final LinearLayout chips;
        final LinearLayout list;
        final TextView status;
        String filter = "全部";

        Page(Activity act) {
            this.act = act;
            this.pal = DeekseepUi.palette(act);
            dialog = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            root = new LinearLayout(act);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(pal.background);

            // top app bar
            LinearLayout bar = new LinearLayout(act);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            int statusTop = DeekseepUi.statusBarHeight(act);
            bar.setPadding(dp(8), statusTop, dp(16), 0);
            TextView back = new TextView(act);
            back.setText("\u2039");
            back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            back.setTextColor(pal.onSurface);
            back.setGravity(Gravity.CENTER);
            back.setPadding(dp(8), 0, dp(8), 0);
            back.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { close(); }
            });
            bar.addView(back, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
            TextView title = new TextView(act);
            title.setText(UiLanguage.text(act, "本地提示词库", "Local prompt library"));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setTextColor(pal.onSurface);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tlp.leftMargin = dp(8);
            bar.addView(title, tlp);
            root.addView(bar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(64) + statusTop));

            FrameLayout body = new FrameLayout(act);
            root.addView(body, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            ScrollView scroll = new ScrollView(act);
            scroll.setClipToPadding(false);
            scroll.setPadding(0, 0, 0, dp(96));
            body.addView(scroll, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout content = new LinearLayout(act);
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(content, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // status card: what is currently injected
            status = new TextView(act);
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            status.setTextColor(pal.onSecondaryContainer);
            status.setLineSpacing(dp(2), 1f);
            status.setPadding(dp(18), dp(14), dp(18), dp(14));
            status.setBackground(M3Theme.rounded(pal.secondaryContainer, dp(20)));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMargins(dp(16), dp(4), dp(16), dp(8));
            content.addView(status, slp);

            // category chips
            HorizontalScrollView chipScroll = new HorizontalScrollView(act);
            chipScroll.setHorizontalScrollBarEnabled(false);
            chips = new LinearLayout(act);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            chips.setPadding(dp(12), dp(4), dp(12), dp(4));
            chipScroll.addView(chips);
            content.addView(chipScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            list = new LinearLayout(act);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(16), dp(4), dp(16), dp(8));
            content.addView(list, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // extended FAB: new prompt
            TextView fab = new TextView(act);
            fab.setText(UiLanguage.text(act, "＋  新建提示词", "+  New prompt"));
            fab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            fab.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            fab.setTextColor(pal.onPrimaryContainer);
            fab.setGravity(Gravity.CENTER);
            fab.setPadding(dp(20), 0, dp(24), 0);
            fab.setBackground(M3Theme.rounded(pal.primaryContainer, dp(16)));
            fab.setElevation(dp(6));
            fab.setClickable(true);
            M3Theme.attachPress(fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { edit(null); }
            });
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(56), Gravity.BOTTOM | Gravity.END);
            flp.setMargins(0, 0, dp(16), dp(20));
            body.addView(fab, flp);
        }

        void open() {
            refresh();
            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                w.setBackgroundDrawable(new ColorDrawable(pal.background));
            }
            DeekseepUi.trackChildDialog(dialog);
            DeekseepUi.openWithSlide(dialog, root);
            dialog.setOnKeyListener(new Dialog.OnKeyListener() {
                @Override public boolean onKey(android.content.DialogInterface d, int code, KeyEvent e) {
                    if (code == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) {
                        close();
                        return true;
                    }
                    return false;
                }
            });
        }

        void close() { DeekseepUi.slideOutAndDismiss(dialog, root); }

        void refresh() {
            refreshStatus();
            refreshChips();
            refreshList();
        }

        void refreshStatus() {
            String active = Main.activeLibraryPromptTitle();
            String text;
            if (Main.isEmbeddedPromptEnabled()) {
                text = UiLanguage.text(act, "当前使用内置隐藏提示词（一键破甲）。如需写入提示词库内容，请先关闭它。",
                        "The bundled hidden prompt is active. Turn it off before applying a library prompt.");
            } else if (active != null) {
                text = UiLanguage.text(act, "提示词注入：已开启\n当前写入：", "Prompt injection: on\nActive: ") + active;
            } else if (Main.isEnabled()) {
                text = UiLanguage.text(act, "提示词注入：已开启（来源：导入的文件）\n点按任一提示词的“一键写入”即可替换。",
                        "Prompt injection: on (imported file)\nTap “Apply” on any prompt to replace it.");
            } else {
                text = UiLanguage.text(act, "提示词注入：未开启\n点按“一键写入”会写入该提示词并自动开启注入。纯本地保存，不联网。",
                        "Prompt injection: off\n“Apply” writes the prompt and turns injection on. Stored locally only.");
            }
            status.setText(text);
        }

        void refreshChips() {
            chips.removeAllViews();
            List<String> names = new ArrayList<>();
            names.add("全部");
            for (String c : PromptLibrary.CATEGORIES) names.add(c);
            names.add(PromptLibrary.CATEGORY_CUSTOM);
            for (final String name : names) {
                boolean selected = name.equals(filter);
                TextView chip = new TextView(act);
                chip.setText(UiLanguage.dynamic(act, name));
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                chip.setGravity(Gravity.CENTER);
                chip.setPadding(dp(16), 0, dp(16), 0);
                chip.setTextColor(selected ? pal.onSecondaryContainer : pal.onSurfaceVariant);
                chip.setBackground(selected
                        ? M3Theme.rounded(pal.secondaryContainer, dp(8))
                        : M3Theme.roundedStroke(0x00000000, dp(8), dp(1), pal.outline));
                chip.setClickable(true);
                M3Theme.attachPress(chip);
                chip.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        filter = name;
                        refreshChips();
                        refreshList();
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                chips.addView(chip, lp);
            }
        }

        void refreshList() {
            list.removeAllViews();
            String active = Main.activeLibraryPromptTitle();
            int shown = 0;
            for (PromptLibrary.Prompt p : PromptLibrary.all()) {
                if (!matches(p)) continue;
                View card = promptCard(p, active != null && active.equals(p.title));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, dp(6), 0, dp(6));
                list.addView(card, lp);
                M3Theme.animateEntrance(card, 40L + Math.min(shown, 10) * 30L);
                shown++;
            }
            if (shown == 0) {
                TextView empty = new TextView(act);
                empty.setText(PromptLibrary.CATEGORY_CUSTOM.equals(filter)
                        ? UiLanguage.text(act, "还没有自定义提示词，点右下角“新建提示词”。",
                                "No custom prompts yet. Tap “New prompt”.")
                        : UiLanguage.text(act, "此分类暂无提示词", "No prompts in this category"));
                empty.setTextColor(pal.onSurfaceVariant);
                empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(16), dp(32), dp(16), dp(32));
                list.addView(empty);
            }
        }

        boolean matches(PromptLibrary.Prompt p) {
            if ("全部".equals(filter)) return true;
            if (PromptLibrary.CATEGORY_CUSTOM.equals(filter)) return !p.builtIn;
            return filter.equals(p.category);
        }

        View promptCard(final PromptLibrary.Prompt p, boolean active) {
            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(12));
            card.setBackground(active
                    ? M3Theme.roundedStroke(pal.surfaceContainerLow, dp(24), dp(2), pal.primary)
                    : M3Theme.card(pal, dp(24)));
            card.setElevation(dp(active ? 3 : 1));

            LinearLayout head = new LinearLayout(act);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(act);
            title.setText(p.title);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setTextColor(pal.onSurface);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            head.addView(title, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView tag = new TextView(act);
            tag.setText(UiLanguage.dynamic(act, p.builtIn ? p.category : PromptLibrary.CATEGORY_CUSTOM
                    + " · " + p.category));
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tag.setTextColor(pal.onSecondaryContainer);
            tag.setPadding(dp(10), dp(3), dp(10), dp(3));
            tag.setBackground(M3Theme.rounded(pal.secondaryContainer, dp(8)));
            head.addView(tag);
            card.addView(head);

            TextView preview = new TextView(act);
            preview.setText(p.content);
            preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            preview.setTextColor(pal.onSurfaceVariant);
            preview.setLineSpacing(dp(2), 1f);
            preview.setMaxLines(3);
            preview.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = dp(8);
            card.addView(preview, plp);

            LinearLayout actions = new LinearLayout(act);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alp.topMargin = dp(10);
            card.addView(actions, alp);

            if (!p.builtIn) {
                actions.addView(textButton(UiLanguage.text(act, "删除", "Delete"), pal.error,
                        new View.OnClickListener() {
                            @Override public void onClick(View v) { confirmDelete(p); }
                        }));
                actions.addView(textButton(UiLanguage.text(act, "编辑", "Edit"), pal.primary,
                        new View.OnClickListener() {
                            @Override public void onClick(View v) { edit(p); }
                        }));
            } else {
                actions.addView(textButton(UiLanguage.text(act, "另存为我的", "Copy to mine"),
                        pal.primary, new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                PromptLibrary.Prompt copy = new PromptLibrary.Prompt(
                                        PromptLibrary.newId(), p.category, p.title, p.content, false);
                                edit(copy);
                            }
                        }));
            }
            TextView apply = new TextView(act);
            apply.setText(active ? UiLanguage.text(act, "已写入", "Applied")
                    : UiLanguage.text(act, "一键写入", "Apply"));
            apply.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            apply.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            apply.setTextColor(pal.onPrimary);
            apply.setGravity(Gravity.CENTER);
            apply.setPadding(dp(20), 0, dp(20), 0);
            apply.setBackground(M3Theme.rounded(pal.primary, dp(20)));
            apply.setClickable(true);
            M3Theme.attachPress(apply);
            apply.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { applyPrompt(p); }
            });
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
            blp.leftMargin = dp(8);
            actions.addView(apply, blp);

            card.setClickable(true);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { preview(p); }
            });
            return card;
        }

        TextView textButton(String label, int color, View.OnClickListener click) {
            TextView b = new TextView(act);
            b.setText(label);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            b.setTextColor(color);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(12), 0, dp(12), 0);
            b.setMinHeight(dp(40));
            b.setBackground(DeekseepUi.rowRipple(act));
            b.setClickable(true);
            b.setOnClickListener(click);
            return b;
        }

        void applyPrompt(PromptLibrary.Prompt p) {
            String error = Main.applyLibraryPrompt(p.title, p.content);
            if (error != null) {
                Toast.makeText(act, error, Toast.LENGTH_LONG).show();
                return;
            }
            try { DeekseepUi.refreshPromptControls(); } catch (Throwable ignored) {}
            Toast.makeText(act, UiLanguage.text(act, "已写入并开启提示词注入：", "Applied and injection on: ")
                    + p.title, Toast.LENGTH_SHORT).show();
            refreshStatus();
            refreshList();
        }

        void preview(final PromptLibrary.Prompt p) {
            TextView body = new TextView(act);
            body.setText(p.content);
            body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            body.setTextColor(pal.onSurface);
            body.setLineSpacing(dp(3), 1f);
            body.setTextIsSelectable(true);
            body.setPadding(dp(24), dp(8), dp(24), dp(8));
            ScrollView scroll = new ScrollView(act);
            scroll.addView(body);
            AlertDialog d = new AlertDialog.Builder(act)
                    .setTitle(p.title)
                    .setView(scroll)
                    .setNegativeButton(UiLanguage.text(act, "关闭", "Close"), null)
                    .setPositiveButton(UiLanguage.text(act, "一键写入", "Apply"),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface dd, int w) {
                                    applyPrompt(p);
                                }
                            })
                    .create();
            M3Theme.styleDialog(d, act);
            d.show();
        }

        void confirmDelete(final PromptLibrary.Prompt p) {
            AlertDialog d = new AlertDialog.Builder(act)
                    .setTitle(UiLanguage.text(act, "删除提示词", "Delete prompt"))
                    .setMessage(UiLanguage.text(act, "确定删除“", "Delete “") + p.title
                            + UiLanguage.text(act, "”？此操作不会影响当前已写入的注入内容。",
                            "”? The currently injected text is not affected."))
                    .setNegativeButton(UiLanguage.text(act, "取消", "Cancel"), null)
                    .setPositiveButton(UiLanguage.text(act, "删除", "Delete"),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface dd, int w) {
                                    List<PromptLibrary.Prompt> custom = PromptLibrary.loadCustom();
                                    for (int i = custom.size() - 1; i >= 0; i--) {
                                        if (custom.get(i).id.equals(p.id)) custom.remove(i);
                                    }
                                    if (!PromptLibrary.saveCustom(custom)) {
                                        Toast.makeText(act, UiLanguage.text(act, "删除失败", "Delete failed"),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    refreshList();
                                }
                            })
                    .create();
            M3Theme.styleDialog(d, act);
            d.show();
        }

        /** Create (p == null), edit an existing custom prompt, or save a copy of a built-in. */
        void edit(final PromptLibrary.Prompt p) {
            LinearLayout box = new LinearLayout(act);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(24), dp(8), dp(24), 0);
            final EditText title = field(UiLanguage.text(act, "标题", "Title"), false);
            if (p != null) title.setText(p.title);
            box.addView(title);
            final String[] category = {p != null ? p.category : PromptLibrary.CATEGORY_GENERAL};
            final LinearLayout catRow = new LinearLayout(act);
            catRow.setOrientation(LinearLayout.HORIZONTAL);
            HorizontalScrollView catScroll = new HorizontalScrollView(act);
            catScroll.setHorizontalScrollBarEnabled(false);
            catScroll.addView(catRow);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.topMargin = dp(8);
            box.addView(catScroll, clp);
            final Runnable[] renderCats = new Runnable[1];
            renderCats[0] = new Runnable() {
                @Override public void run() {
                    catRow.removeAllViews();
                    for (final String c : PromptLibrary.CATEGORIES) {
                        boolean sel = c.equals(category[0]);
                        TextView chip = new TextView(act);
                        chip.setText(UiLanguage.dynamic(act, c));
                        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        chip.setGravity(Gravity.CENTER);
                        chip.setPadding(dp(14), 0, dp(14), 0);
                        chip.setTextColor(sel ? pal.onSecondaryContainer : pal.onSurfaceVariant);
                        chip.setBackground(sel ? M3Theme.rounded(pal.secondaryContainer, dp(8))
                                : M3Theme.roundedStroke(0x00000000, dp(8), dp(1), pal.outline));
                        chip.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                category[0] = c;
                                renderCats[0].run();
                            }
                        });
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
                        lp.rightMargin = dp(8);
                        catRow.addView(chip, lp);
                    }
                }
            };
            renderCats[0].run();
            final EditText content = field(UiLanguage.text(act, "提示词内容", "Prompt text"), true);
            if (p != null) content.setText(p.content);
            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(200));
            contentLp.topMargin = dp(10);
            box.addView(content, contentLp);

            final boolean existingCustom = p != null && !p.builtIn && containsCustom(p.id);
            AlertDialog d = new AlertDialog.Builder(act)
                    .setTitle(p == null ? UiLanguage.text(act, "新建提示词", "New prompt")
                            : existingCustom ? UiLanguage.text(act, "编辑提示词", "Edit prompt")
                            : UiLanguage.text(act, "另存为我的提示词", "Save as my prompt"))
                    .setView(box)
                    .setNegativeButton(UiLanguage.text(act, "取消", "Cancel"), null)
                    .setPositiveButton(UiLanguage.text(act, "保存", "Save"),
                            new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface dd, int w) {
                                    String t = title.getText() == null ? "" : title.getText().toString().trim();
                                    String c = content.getText() == null ? "" : content.getText().toString();
                                    if (c.trim().length() == 0) {
                                        Toast.makeText(act, UiLanguage.text(act, "提示词内容不能为空",
                                                "Prompt text cannot be empty"), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (t.length() == 0) t = UiLanguage.text(act, "未命名", "Untitled");
                                    if (t.length() > 40) t = t.substring(0, 40);
                                    List<PromptLibrary.Prompt> custom = PromptLibrary.loadCustom();
                                    boolean replaced = false;
                                    if (existingCustom) {
                                        for (PromptLibrary.Prompt item : custom) {
                                            if (item.id.equals(p.id)) {
                                                item.title = t;
                                                item.content = c;
                                                item.category = category[0];
                                                replaced = true;
                                            }
                                        }
                                    }
                                    if (!replaced) {
                                        custom.add(new PromptLibrary.Prompt(
                                                p != null && !p.builtIn ? p.id : PromptLibrary.newId(),
                                                category[0], t, c, false));
                                    }
                                    if (!PromptLibrary.saveCustom(custom)) {
                                        Toast.makeText(act, UiLanguage.text(act, "保存失败", "Save failed"),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    refreshList();
                                }
                            })
                    .create();
            M3Theme.styleDialog(d, act);
            d.show();
        }

        boolean containsCustom(String id) {
            for (PromptLibrary.Prompt item : PromptLibrary.loadCustom()) {
                if (item.id.equals(id)) return true;
            }
            return false;
        }

        EditText field(String hint, boolean multiLine) {
            EditText e = new EditText(act);
            e.setHint(hint);
            e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            e.setTextColor(pal.onSurface);
            e.setHintTextColor(pal.onSurfaceVariant);
            e.setPadding(dp(16), dp(12), dp(16), dp(12));
            e.setBackground(M3Theme.roundedStroke(pal.surfaceContainerHighest, dp(12), dp(1),
                    pal.outlineVariant));
            if (multiLine) {
                e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                e.setGravity(Gravity.TOP | Gravity.START);
                e.setSingleLine(false);
                e.setMinLines(5);
            } else {
                e.setSingleLine(true);
            }
            return e;
        }

        int dp(float v) { return DeekseepUi.dp(act, v); }
    }
}
