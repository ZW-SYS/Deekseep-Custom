package com.dsmod.probe;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.widget.Switch;

/** KSU-style switch whose thumb remains fully inset inside a slightly larger track. */
final class HubInsetSwitch extends Switch {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ColorStateList moduleThumbTint;
    private ColorStateList moduleTrackTint;
    private float thumbPosition;
    private ValueAnimator positionAnimator;
    private M3Theme.Palette palette;

    HubInsetSwitch(Context context) {
        super(context);
        setShowText(false);
        setSplitTrack(false);
        setMinWidth(0);
        setMinimumWidth(0);
        setMinimumHeight(0);
        setPadding(0, 0, 0, 0);
        thumbPosition = isChecked() ? 1f : 0f;
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override public void setThumbTintList(ColorStateList tint) {
        moduleThumbTint = tint;
        try { super.setThumbTintList(tint); } catch (Throwable ignored) {}
        invalidate();
    }

    @Override public void setTrackTintList(ColorStateList tint) {
        moduleTrackTint = tint;
        try { super.setTrackTintList(tint); } catch (Throwable ignored) {}
        invalidate();
    }

    @Override public void setChecked(boolean checked) {
        boolean changed = checked != isChecked();
        super.setChecked(checked);
        float target = checked ? 1f : 0f;
        if (!changed || getWidth() == 0 || getWindowToken() == null) {
            thumbPosition = target;
            invalidate();
            return;
        }
        if (positionAnimator != null) positionAnimator.cancel();
        positionAnimator = ValueAnimator.ofFloat(thumbPosition, target);
        positionAnimator.setDuration(220L);
        positionAnimator.setInterpolator(
                new android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f));
        positionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                thumbPosition = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        positionAnimator.start();
    }

    @Override public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = dp(56);
        int desiredHeight = dp(36);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        M3Theme.Palette pal = palette;
        if (pal == null) {
            try { pal = palette = M3Theme.palette(getContext()); } catch (Throwable ignored) {}
        }
        if (pal != null) {
            drawMaterial3(canvas, pal);
            return;
        }
        int stateColorTrack = colorFor(moduleTrackTint,
                isChecked() ? 0xFFADBFFF : 0xFFBFBFBF);
        int stateColorThumb = colorFor(moduleThumbTint,
                isChecked() ? DeekseepUi.BRAND : 0xFFFFFFFF);
        if (!isEnabled()) {
            stateColorTrack = withAlpha(stateColorTrack, 0.45f);
            stateColorThumb = withAlpha(stateColorThumb, 0.55f);
        }

        float trackWidth = Math.min(dp(52), getWidth());
        float trackHeight = Math.min(dp(30), getHeight());
        float left = (getWidth() - trackWidth) * 0.5f;
        float top = (getHeight() - trackHeight) * 0.5f;
        paint.setColor(stateColorTrack);
        canvas.drawRoundRect(left, top, left + trackWidth, top + trackHeight,
                trackHeight * 0.5f, trackHeight * 0.5f, paint);

        float inset = dp(3);
        float diameter = trackHeight - inset * 2f;
        float start = left + inset + diameter * 0.5f;
        float end = left + trackWidth - inset - diameter * 0.5f;
        float visualPosition = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                ? 1f - thumbPosition : thumbPosition;
        float centerX = start + (end - start) * visualPosition;
        paint.setColor(stateColorThumb);
        canvas.drawCircle(centerX, top + trackHeight * 0.5f, diameter * 0.5f, paint);
    }

    /**
     * Material 3 switch: 52x32dp track. Off = outlined surface track with a 16dp outline-colored
     * thumb; on = primary track with a 24dp onPrimary thumb. The thumb grows to 28dp while pressed
     * and its size/color interpolate with the position animation.
     */
    private void drawMaterial3(Canvas canvas, M3Theme.Palette pal) {
        float t = Math.max(0f, Math.min(1f, thumbPosition));
        float trackWidth = Math.min(dp(52), getWidth());
        float trackHeight = Math.min(dp(32), getHeight());
        float left = (getWidth() - trackWidth) * 0.5f;
        float top = (getHeight() - trackHeight) * 0.5f;
        float radius = trackHeight * 0.5f;

        int trackOff = pal.surfaceContainerHighest;
        int trackOn = pal.primary;
        int track = blendColor(trackOff, trackOn, t);
        int thumb = blendColor(pal.outline, pal.onPrimary, t);
        if (!isEnabled()) {
            track = withAlpha(track, 0.38f);
            thumb = withAlpha(thumb, 0.5f);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(track);
        canvas.drawRoundRect(left, top, left + trackWidth, top + trackHeight, radius, radius, paint);
        if (t < 1f) {
            paint.setStyle(Paint.Style.STROKE);
            float stroke = dp(2);
            paint.setStrokeWidth(stroke);
            paint.setColor(withAlpha(isEnabled() ? pal.outline : withAlpha(pal.outline, 0.38f),
                    1f - t));
            canvas.drawRoundRect(left + stroke / 2f, top + stroke / 2f,
                    left + trackWidth - stroke / 2f, top + trackHeight - stroke / 2f,
                    radius - stroke / 2f, radius - stroke / 2f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        float diameter = isPressed() ? dp(28) : dp(16) + (dp(24) - dp(16)) * t;
        float pad = (trackHeight - dp(24)) * 0.5f + dp(12);
        float start = left + pad;
        float end = left + trackWidth - pad;
        float visualPosition = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? 1f - t : t;
        float centerX = start + (end - start) * visualPosition;
        paint.setColor(thumb);
        canvas.drawCircle(centerX, top + radius, diameter * 0.5f, paint);
    }

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try { palette = M3Theme.palette(getContext()); } catch (Throwable ignored) {}
    }

    private static int blendColor(int from, int to, float t) {
        float inv = 1f - t;
        int a = Math.round(((from >>> 24) & 0xFF) * inv + ((to >>> 24) & 0xFF) * t);
        int r = Math.round(((from >> 16) & 0xFF) * inv + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * inv + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * inv + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override protected void onDetachedFromWindow() {
        if (positionAnimator != null) positionAnimator.cancel();
        positionAnimator = null;
        super.onDetachedFromWindow();
    }

    private int colorFor(ColorStateList colors, int fallback) {
        return colors == null ? fallback
                : colors.getColorForState(getDrawableState(), colors.getDefaultColor());
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, getResources().getDisplayMetrics()));
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * multiplier);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
