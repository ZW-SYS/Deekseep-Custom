package com.dsmod.probe;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * 界面美化 › "液态玻璃输入框" and "输入框磨砂背景": two independent opt-in toggles (default off)
 * that style the chat composer with a translucent rounded surface, a subtle gradient highlight
 * border and, for the frosted variant, a blur.
 *
 * <p>Two reachable paths, both fail-safe (any exception leaves the host untouched):</p>
 * <ol>
 * <li><b>Compose path</b> — when the module's verified input-container hook is installed
 *     ({@code HookUiSurface.hookChatBubbleCustomization}; not available on hosts running in
 *     2.7.1 limited mode), the composer Modifier gets a translucent rounded background and a
 *     highlight border.</li>
 * <li><b>View path</b> — when the host hosts an {@link EditText} in the activity tree (some host
 *     generations embed one through an AndroidView), its nearest parent container gets a
 *     {@link GlassDrawable} background. On API 31+ the frosted variant adds a backdrop plate
 *     blurred with {@code RenderEffect.createBlurEffect}; below 31 it falls back to a denser
 *     translucent fill.</li>
 * </ol>
 * <p>Limitation: Android Views cannot blur other content of the same window behind them, so the
 * RenderEffect softens the plate itself (frosted material look) rather than the chat behind it.
 * If neither path is reachable on the running host (pure Compose composer, limited mode), the
 * toggles simply have no visible effect.</p>
 */
final class InputGlassStyle {

    private static final String DIR = new File(Main.PROMPT_FILE).getParent();
    static final String LIQUID_FILE = DIR + "/deekseep_input_liquid_glass";
    static final String FROSTED_FILE = DIR + "/deekseep_input_frosted";
    private static final String TAG_STYLED = "yunseek_input_glass_v1";
    private static final String TAG_PLATE = "yunseek_input_glass_plate_v1";

    private static volatile long cacheAt;
    private static volatile boolean liquidCache;
    private static volatile boolean frostedCache;
    private static WeakReference<Activity> listening = new WeakReference<>(null);
    private static long lastScanAt;

    private InputGlassStyle() {}

    private static void refreshCache() {
        long now = SystemClock.uptimeMillis();
        if (now - cacheAt < 1000L) return;
        cacheAt = now;
        liquidCache = new File(LIQUID_FILE).exists();
        frostedCache = new File(FROSTED_FILE).exists();
    }

    static boolean liquidEnabled() { refreshCache(); return liquidCache; }

    static boolean frostedEnabled() { refreshCache(); return frostedCache; }

    static boolean anyEnabled() { refreshCache(); return liquidCache || frostedCache; }

    static boolean setLiquid(boolean on) { return setFlag(LIQUID_FILE, on); }

    static boolean setFrosted(boolean on) { return setFlag(FROSTED_FILE, on); }

    private static boolean setFlag(String path, boolean on) {
        try {
            if (on) Main.overwriteTextFile(path, "1");
            else new File(path).delete();
            cacheAt = 0L;
            return new File(path).exists() == on;
        } catch (Throwable t) {
            Main.log("input glass flag failed: " + t);
            return false;
        }
    }

    // ── Compose path colors (used by HookUiSurface's input-container hook) ─────

    static int composeFill(boolean dark) {
        if (frostedEnabled()) return dark ? 0x5C1E1F24 : 0x8CFFFFFF;
        return dark ? 0x26FFFFFF : 0x40FFFFFF;
    }

    static int composeEdge(boolean dark) {
        return dark ? 0x4DFFFFFF : 0x66FFFFFF;
    }

    // ── View path ────────────────────────────────────────────────────────────

    /** Called on every host MainActivity resume and after a toggle changes. Never throws. */
    static void onActivityResumed(final Activity act) {
        try {
            if (act == null || act.getWindow() == null) return;
            final View decor = act.getWindow().getDecorView();
            if (!anyEnabled()) {
                restoreTree(decor);
                return;
            }
            if (listening.get() != act) {
                listening = new WeakReference<>(act);
                decor.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override public void onGlobalLayout() {
                                try {
                                    if (listening.get() != act) {
                                        decor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                        return;
                                    }
                                    long now = SystemClock.uptimeMillis();
                                    if (now - lastScanAt < 800L) return;
                                    lastScanAt = now;
                                    if (anyEnabled()) styleTree(decor, 0); else restoreTree(decor);
                                } catch (Throwable ignored) {}
                            }
                        });
            }
            decor.post(new Runnable() {
                @Override public void run() {
                    try { styleTree(decor, 0); } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable t) {
            Main.log("input glass resume skipped: " + t);
        }
    }

    private static void styleTree(View view, int depth) {
        if (view == null || depth > 40) return;
        if (view instanceof EditText) {
            apply((EditText) view);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleTree(group.getChildAt(i), depth + 1);
        }
    }

    private static void apply(EditText edit) {
        try {
            if (!(edit.getParent() instanceof ViewGroup)) return;
            ViewGroup container = (ViewGroup) edit.getParent();
            boolean dark = DeekseepUi.isDark(edit.getContext());
            boolean liquid = liquidEnabled();
            boolean frosted = frostedEnabled();
            String signature = TAG_STYLED + (liquid ? "L" : "") + (frosted ? "F" : "") + (dark ? "D" : "");
            Object tag = container.getTag(R_ID_TAG);
            if (signature.equals(tag)) return;
            if (!(tag instanceof String)) {
                container.setTag(R_ID_ORIGINAL, new Object[]{container.getBackground()});
            }
            float radius = DeekseepUi.dp(edit.getContext(), 24);
            container.setBackground(new GlassDrawable(dark, liquid, frosted, radius,
                    DeekseepUi.dp(edit.getContext(), 1)));
            if (Build.VERSION.SDK_INT >= 21) container.setClipToOutline(true);
            removePlate(container);
            if (frosted && Build.VERSION.SDK_INT >= 31 && container instanceof FrameLayout) {
                View plate = new View(edit.getContext());
                plate.setTag(TAG_PLATE);
                plate.setBackground(new GlassDrawable(dark, false, true, radius, 0));
                plate.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                        DeekseepUi.dp(edit.getContext(), 18), DeekseepUi.dp(edit.getContext(), 18),
                        Shader.TileMode.CLAMP));
                plate.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                container.addView(plate, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            container.setTag(R_ID_TAG, signature);
        } catch (Throwable t) {
            Main.log("input glass view styling skipped: " + t);
        }
    }

    private static void restoreTree(View view) {
        try { restoreTree(view, 0); } catch (Throwable ignored) {}
    }

    private static void restoreTree(View view, int depth) {
        if (view == null || depth > 40) return;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            Object tag = group.getTag(R_ID_TAG);
            if (tag instanceof String && ((String) tag).startsWith(TAG_STYLED)) {
                removePlate(group);
                Object original = group.getTag(R_ID_ORIGINAL);
                Drawable bg = original instanceof Object[] && ((Object[]) original).length == 1
                        && ((Object[]) original)[0] instanceof Drawable
                        ? (Drawable) ((Object[]) original)[0] : null;
                group.setBackground(bg);
                group.setTag(R_ID_TAG, null);
                group.setTag(R_ID_ORIGINAL, null);
            }
            for (int i = 0; i < group.getChildCount(); i++) restoreTree(group.getChildAt(i), depth + 1);
        }
    }

    private static void removePlate(ViewGroup container) {
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (TAG_PLATE.equals(child.getTag())) container.removeViewAt(i);
        }
    }

    // View.setTag(int, Object) requires an application-namespace id; use stable framework-free
    // ids in the 0x7F_FF_xxxx range that neither DeepSeek nor the platform allocates.
    private static final int R_ID_TAG = 0x7FFF5101;
    private static final int R_ID_ORIGINAL = 0x7FFF5102;

    /** Translucent rounded surface with a top-left → bottom-right gradient highlight border. */
    static final class GlassDrawable extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean dark;
        private final boolean liquid;
        private final float radius;
        private final float strokeWidth;
        private final RectF rect = new RectF();

        GlassDrawable(boolean dark, boolean liquid, boolean frosted, float radius, float strokeWidth) {
            this.dark = dark;
            this.liquid = liquid;
            this.radius = radius;
            this.strokeWidth = strokeWidth;
            int base;
            if (frosted) base = dark ? 0x8C202126 : 0xB3F7F8FB;
            else base = dark ? 0x33FFFFFF : 0x52FFFFFF;
            if (Build.VERSION.SDK_INT < 31 && frosted) base = dark ? 0xB3202126 : 0xD9F7F8FB;
            fill.setColor(base);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1f, strokeWidth));
        }

        @Override protected void onBoundsChange(android.graphics.Rect bounds) {
            super.onBoundsChange(bounds);
            float half = strokeWidth / 2f;
            rect.set(bounds.left + half, bounds.top + half, bounds.right - half, bounds.bottom - half);
            int hi = dark ? 0x80FFFFFF : 0xE6FFFFFF;
            int lo = dark ? 0x1AFFFFFF : 0x33A0A8C0;
            stroke.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    hi, lo, Shader.TileMode.CLAMP));
            if (liquid) {
                sheen.setShader(new LinearGradient(0, bounds.top, 0,
                        bounds.top + bounds.height() * 0.55f,
                        dark ? 0x22FFFFFF : 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            }
        }

        @Override public void draw(Canvas canvas) {
            float r = Math.min(radius, rect.height() / 2f);
            canvas.drawRoundRect(rect, r, r, fill);
            if (liquid) canvas.drawRoundRect(rect, r, r, sheen);
            if (strokeWidth > 0f) canvas.drawRoundRect(rect, r, r, stroke);
        }

        @Override public void getOutline(android.graphics.Outline outline) {
            android.graphics.Rect b = getBounds();
            outline.setRoundRect(b, Math.min(radius, b.height() / 2f));
        }

        @Override public void setAlpha(int alpha) { fill.setAlpha(alpha); invalidateSelf(); }

        @Override public void setColorFilter(ColorFilter colorFilter) {
            fill.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
