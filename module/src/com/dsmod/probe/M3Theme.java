package com.dsmod.probe;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

/**
 * YunSeek Material 3 palette and small styling helpers, implemented by hand because the host
 * process does not ship the AndroidX Material library.
 *
 * <p>On Android 12+ the palette is seeded from the device's Monet dynamic colors
 * ({@code android.R.color.system_accent1_*} / {@code system_neutral1_*}) read through
 * {@link Context#getColor(int)}. Below API 31 (or if any read fails) it falls back to a fixed
 * seed palette that matches YunSeek's historical brand blue. Dark mode is honoured in both cases.
 */
final class M3Theme {

    private M3Theme() {}

    /** A resolved set of M3 roles for the current context and light/dark mode. */
    static final class Palette {
        boolean dark;
        int primary;
        int onPrimary;
        int primaryContainer;
        int onPrimaryContainer;
        int secondaryContainer;
        int onSecondaryContainer;
        int background;
        int surface;
        int surfaceContainerLow;
        int surfaceContainer;
        int surfaceContainerHigh;
        int surfaceContainerHighest;
        int onSurface;
        int onSurfaceVariant;
        int outline;
        int outlineVariant;
        int error;
    }

    static boolean isDark(Context c) {
        try {
            int systemMode = Resources.getSystem().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            if (systemMode == Configuration.UI_MODE_NIGHT_YES) return true;
            if (systemMode == Configuration.UI_MODE_NIGHT_NO) return false;
        } catch (Throwable ignored) {}
        try {
            return c != null && (c.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Builds a palette for the given context honouring the user theme-color override when set. */
    static Palette palette(Context c) {
        return palette(c, isDark(c));
    }

    /** Palette for an explicit light/dark mode (used where host-themed text must stay legible). */
    static Palette palette(Context c, boolean dark) {
        Palette p = new Palette();
        p.dark = dark;

        int seed = brandSeed();
        boolean monet = false;
        if (Build.VERSION.SDK_INT >= 31 && c != null) {
            try {
                // system_accent1_500 is the Monet primary tone; use it as the seed.
                int accent = c.getColor(android.R.color.system_accent1_500);
                if ((accent >>> 24) != 0) { seed = accent | 0xFF000000; monet = true; }
            } catch (Throwable ignored) {}
        }

        if (monet && c != null) {
            try {
                buildFromMonet(c, p);
                return amoled(p);
            } catch (Throwable ignored) {
                // fall through to the seeded palette
            }
        }
        buildFromSeed(seed, p);
        return amoled(p);
    }

    /** YunSeek 纯黑 AMOLED: true-black surfaces for module pages in dark mode. */
    private static Palette amoled(Palette p) {
        try {
            if (p.dark && PrivacyGuard.amoledEnabled()) {
                p.background = 0xFF000000;
                p.surface = 0xFF000000;
                p.surfaceContainerLow = 0xFF050505;
                p.surfaceContainer = 0xFF0B0B0B;
            }
        } catch (Throwable ignored) {}
        return p;
    }

    private static int brandSeed() {
        try {
            ThemeColorConfig.Value v = ThemeColorConfig.get();
            if (v != null && v.enabled) return v.primary | 0xFF000000;
        } catch (Throwable ignored) {}
        return 0xFF4D6BFE;
    }

    private static void buildFromMonet(Context c, Palette p) {
        // Monet tonal ramps: accent1 (primary) and neutral1 (surfaces).
        int a100 = c.getColor(android.R.color.system_accent1_100);
        int a200 = c.getColor(android.R.color.system_accent1_200);
        int a300 = c.getColor(android.R.color.system_accent1_300);
        int a600 = c.getColor(android.R.color.system_accent1_600);
        int a700 = c.getColor(android.R.color.system_accent1_700);
        int a800 = c.getColor(android.R.color.system_accent1_800);
        int n0   = safeColor(c, android.R.color.system_neutral1_0, 0xFFFFFFFF);
        int n10  = c.getColor(android.R.color.system_neutral1_10);
        int n50  = c.getColor(android.R.color.system_neutral1_50);
        int n100 = c.getColor(android.R.color.system_neutral1_100);
        int n800 = c.getColor(android.R.color.system_neutral1_800);
        int n900 = c.getColor(android.R.color.system_neutral1_900);
        int nv30 = safeColor(c, android.R.color.system_neutral2_300, 0xFFBFC6D0);
        int nv60 = safeColor(c, android.R.color.system_neutral2_600, 0xFF6B7280);

        if (p.dark) {
            p.primary = a200;
            p.onPrimary = a800;
            p.primaryContainer = a700;
            p.onPrimaryContainer = a100;
            p.secondaryContainer = a800;
            p.onSecondaryContainer = a100;
            p.background = darken(n900, 0.4f);
            p.surface = n900;
            p.surfaceContainerLow = lighten(n900, 0.03f);
            p.surfaceContainer = lighten(n900, 0.06f);
            p.surfaceContainerHigh = lighten(n900, 0.10f);
            p.surfaceContainerHighest = lighten(n900, 0.14f);
            p.onSurface = 0xFFECECEC;
            p.onSurfaceVariant = nv30;
            p.outline = nv60;
            p.outlineVariant = n800;
        } else {
            p.primary = a600;
            p.onPrimary = 0xFFFFFFFF;
            p.primaryContainer = a100;
            p.onPrimaryContainer = a800;
            p.secondaryContainer = a100;
            p.onSecondaryContainer = a800;
            p.background = n10;
            p.surface = n0;
            p.surfaceContainerLow = n10;
            p.surfaceContainer = n50;
            p.surfaceContainerHigh = n100;
            p.surfaceContainerHighest = darken(n100, 0.04f);
            p.onSurface = 0xFF1A1A1A;
            p.onSurfaceVariant = nv60;
            p.outline = nv60;
            p.outlineVariant = n100;
        }
        p.error = p.dark ? 0xFFFFB4AB : 0xFFBA1A1A;
    }

    private static int safeColor(Context c, int id, int fallback) {
        try { return c.getColor(id); } catch (Throwable t) { return fallback; }
    }

    private static void buildFromSeed(int seed, Palette p) {
        if (p.dark) {
            p.primary = tone(seed, 0.55f, true);
            p.onPrimary = tone(seed, 0.20f, false);
            p.primaryContainer = tone(seed, 0.30f, false);
            p.onPrimaryContainer = tone(seed, 0.85f, true);
            p.secondaryContainer = blend(0xFF2A2A2D, seed, 0.18f);
            p.onSecondaryContainer = 0xFFE4E4E8;
            p.background = 0xFF141416;
            p.surface = 0xFF1B1B1D;
            p.surfaceContainerLow = 0xFF212123;
            p.surfaceContainer = 0xFF262628;
            p.surfaceContainerHigh = 0xFF2E2E31;
            p.surfaceContainerHighest = 0xFF37373A;
            p.onSurface = 0xFFECECEC;
            p.onSurfaceVariant = 0xFFB8B8BE;
            p.outline = 0xFF8A8A90;
            p.outlineVariant = 0xFF3A3A3D;
        } else {
            p.primary = seed;
            p.onPrimary = 0xFFFFFFFF;
            p.primaryContainer = blend(0xFFFFFFFF, seed, 0.16f);
            p.onPrimaryContainer = tone(seed, 0.28f, false);
            p.secondaryContainer = blend(0xFFEEF1FF, seed, 0.10f);
            p.onSecondaryContainer = tone(seed, 0.25f, false);
            p.background = 0xFFF6F7FB;
            p.surface = 0xFFFFFFFF;
            p.surfaceContainerLow = 0xFFF7F8FC;
            p.surfaceContainer = 0xFFF1F2F7;
            p.surfaceContainerHigh = 0xFFEBECF2;
            p.surfaceContainerHighest = 0xFFE5E6ED;
            p.onSurface = 0xFF1A1A1A;
            p.onSurfaceVariant = 0xFF6B6F78;
            p.outline = 0xFF999CA4;
            p.outlineVariant = 0xFFE2E3EA;
        }
        p.error = p.dark ? 0xFFFFB4AB : 0xFFBA1A1A;
    }

    // ── color math ──────────────────────────────────────────────────────────
    private static int blend(int base, int over, float amount) {
        float inv = 1f - amount;
        return Color.argb(255,
                Math.round(Color.red(base) * inv + Color.red(over) * amount),
                Math.round(Color.green(base) * inv + Color.green(over) * amount),
                Math.round(Color.blue(base) * inv + Color.blue(over) * amount));
    }

    private static int lighten(int c, float amount) { return blend(c, 0xFFFFFFFF, amount); }

    private static int darken(int c, float amount) { return blend(c, 0xFF000000, amount); }

    /** Produces a lighter/darker tonal variant of a seed toward white or black. */
    private static int tone(int seed, float lightness, boolean towardWhite) {
        return towardWhite ? blend(seed, 0xFFFFFFFF, Math.max(0f, lightness - 0.5f) * 1.2f)
                : blend(seed, 0xFF000000, Math.max(0f, 0.5f - lightness) * 1.1f);
    }

    // ── drawable helpers ────────────────────────────────────────────────────
    static GradientDrawable rounded(int color, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    static GradientDrawable roundedStroke(int color, int radiusPx, int strokePx, int strokeColor) {
        GradientDrawable d = rounded(color, radiusPx);
        if (strokePx > 0) d.setStroke(strokePx, strokeColor);
        return d;
    }

    /** A rounded, elevated M3 card background with a subtle hairline outline. */
    static GradientDrawable card(Palette p, int radiusPx) {
        GradientDrawable d = rounded(p.surfaceContainer, radiusPx);
        d.setStroke(Math.max(1, radiusPx / 20), withAlpha(p.outlineVariant, p.dark ? 0.6f : 0.9f));
        return d;
    }

    static int withAlpha(int color, float multiplier) {
        int a = Math.round(255 * clamp01(multiplier));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    /** Applies the M3 surface + rounded corners to an AlertDialog window. */
    static void styleDialog(Dialog dialog, Context c) {
        if (dialog == null) return;
        try {
            Window w = dialog.getWindow();
            if (w == null) return;
            // AlertDialog text follows the host Activity theme, so match its night mode rather
            // than the system one; otherwise light text could land on a light surface.
            boolean activityDark = (c.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            Palette p = palette(c, activityDark);
            // Keep the platform dialog inset (the theme's background is an InsetDrawable).
            int h = dp(c, 16);
            int v = dp(c, 24);
            w.setBackgroundDrawable(new android.graphics.drawable.InsetDrawable(
                    rounded(p.surfaceContainerHigh, dp(c, 28)), h, v, h, v));
        } catch (Throwable ignored) {}
    }

    /** A hardware-friendly press animation (scale + alpha) shared by M3 touch targets. */
    static void attachPress(final View view) {
        if (view == null) return;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                if (!v.isEnabled()) return false;
                int action = e.getActionMasked();
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().cancel();
                    v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.85f).setDuration(110L)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                            .start();
                } else if (action == android.view.MotionEvent.ACTION_UP
                        || action == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().cancel();
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150L)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                            .start();
                }
                return false;
            }
        });
    }

    /** Fade + upward slide entrance for a freshly added row/card. */
    static void animateEntrance(final View view, long delayMs) {
        if (view == null) return;
        try {
            view.setAlpha(0f);
            view.setTranslationY(dp(view.getContext(), 10));
            view.animate().alpha(1f).translationY(0f).setStartDelay(delayMs).setDuration(260L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                    .start();
        } catch (Throwable ignored) {}
    }
}
