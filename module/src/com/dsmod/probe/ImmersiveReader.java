package com.dsmod.probe;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 沉浸阅读 (YunSeek 1.8.1.1): one message in a full-screen, distraction-free page.
 * Plain UI only: 18sp text, 1.6 line spacing, warm eye-care background, monospace code blocks.
 * Tap anywhere or press Back to close. Nothing is sent or modified.
 */
final class ImmersiveReader {
    private ImmersiveReader() {}

    static void show(final Activity act, String text) {
        if (act == null || act.isFinishing() || text == null) return;
        boolean dark = M3Theme.isDark(act);
        int bg = dark ? 0xFF1B1A17 : 0xFFF6F1E7;
        int fg = dark ? 0xFFE6E1D6 : 0xFF2B2A27;
        int codeBg = dark ? 0xFF2A2824 : 0xFFEAE3D3;

        final Dialog dialog = new Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout root = new FrameLayout(act);
        root.setBackgroundColor(bg);
        ScrollView scroll = new ScrollView(act);
        scroll.setFillViewport(true);
        TextView body = new TextView(act);
        body.setTextIsSelectable(false);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        body.setLineSpacing(0f, 1.6f);
        body.setTextColor(fg);
        int padH = DeekseepUi.dp(act, 24);
        body.setPadding(padH, DeekseepUi.dp(act, 48), padH, DeekseepUi.dp(act, 64));
        body.setText(format(text, codeBg));
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View.OnClickListener close = new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { dialog.dismiss(); } catch (Throwable ignored) {}
            }
        };
        body.setOnClickListener(close);
        root.setOnClickListener(close);
        dialog.setContentView(root);
        dialog.setCancelable(true);
        try {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
                w.setStatusBarColor(bg);
                w.setNavigationBarColor(bg);
            }
        } catch (Throwable ignored) {}
        dialog.show();
    }

    /** Markdown-light: ``` fenced blocks become monospace on a tinted background. */
    static CharSequence format(String text, int codeBg) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = text.replace("\r", "").split("\n", -1);
        boolean inCode = false;
        int codeStart = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("```")) {
                if (!inCode) {
                    inCode = true;
                    codeStart = out.length();
                } else {
                    inCode = false;
                    int end = out.length();
                    if (end > codeStart) {
                        out.setSpan(new TypefaceSpan("monospace"), codeStart, end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        out.setSpan(new BackgroundColorSpan(codeBg), codeStart, end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                continue;
            }
            out.append(line);
            if (i < lines.length - 1) out.append('\n');
        }
        if (inCode && out.length() > codeStart) {
            out.setSpan(new TypefaceSpan("monospace"), codeStart, out.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.setSpan(new BackgroundColorSpan(codeBg), codeStart, out.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
    }

    @SuppressWarnings("unused")
    private static final Typeface MONO = Typeface.MONOSPACE;
}
