package com.example.appwheel;

import android.content.Context;
import android.graphics.*;
import com.example.appwheel.Prefs.N;
import android.text.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;
import java.util.*;

/** Endless wheel: apps ride an arc on the right edge; the middle one is the biggest. */
public class WheelView extends View {
    interface Listener { void onPick(Apps.App a); }

    private final float dp = getResources().getDisplayMetrics().density;
    private final float row = 76 * dp;           // finger travel per app
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator motion;
    private final GestureDetector gestures;
    private final List<RectF> hits = new ArrayList<>();
    private final List<Integer> hitIndex = new ArrayList<>();
    private List<Apps.App> apps = new ArrayList<>();
    private Listener listener;
    private float scroll;                        // pixels; one app = row
    private int lastCentre;
    private android.graphics.drawable.Drawable panel;
    private float iconScale, textScale, curve, spin;
    private int spacing, half;
    private boolean left, labels, haptic;
    private boolean flung, stoppedFling;

    public WheelView(Context c) {
        super(c);
        reloadStyle();
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) {
                // keep drags here when placed inside another layout
                getParent().requestDisallowInterceptTouchEvent(true);
                stoppedFling = motion != null && motion.isRunning();
                stop();
                flung = false;
                return true;
            }
            @Override public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                scroll += dy;
                changed();
                return true;
            }
            @Override public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                // one continuous glide that ends exactly on an app
                float speed = Math.max(-9000 * dp, Math.min(9000 * dp, -vy)) * spin;
                float target = Math.round((scroll + speed * 0.25f) / row) * row;
                long time = (long) (4000 * Math.abs(target - scroll) / Math.max(1f, Math.abs(speed)));
                flung = true;
                glide(target, Math.max(250, Math.min(1400, time)));
                return true;
            }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                if (stoppedFling) return true;   // first touch only stops the spin
                for (int k = hits.size() - 1; k >= 0; k--) {
                    if (hits.get(k).contains(e.getX(), e.getY())) {
                        performClick();
                        if (listener != null) listener.onPick(apps.get(hitIndex.get(k)));
                        return true;
                    }
                }
                return true;
            }
        });
        gestures.setIsLongpressEnabled(false);
    }

    void setListener(Listener l) { listener = l; }

    void reloadStyle() {
        Context c = getContext();
        left = Prefs.get(c, N.SIDE) == 1;
        iconScale = Prefs.get(c, N.ICON) / 100f;
        textScale = Prefs.get(c, N.TEXT_SIZE) / 100f;
        spacing = Prefs.get(c, N.SPACING);
        curve = Prefs.get(c, N.CURVE) / 44f;
        half = Prefs.get(c, N.COUNT) + 1;
        spin = Prefs.get(c, N.SPIN) / 100f;
        labels = Prefs.on(c, N.LABELS);
        haptic = Prefs.on(c, N.HAPTIC);
        int fg = Prefs.get(c, N.TEXT);
        text.setColor(0xFF000000 | fg);
        text.setTypeface(Prefs.on(c, N.BOLD) ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        // dark text gets a light shadow and the other way round
        text.setShadowLayer(4 * dp, 0, dp, fg == 0 ? Color.WHITE : Color.BLACK);
        invalidate();
    }

    /** The overlay paints its own panel; the settings preview already has one. */
    void drawPanel() {
        android.graphics.drawable.Drawable d = getContext().getDrawable(Prefs.bgShape(getContext())).mutate();
        d.setTint(Prefs.argb(getContext(), N.BG));
        d.setAlpha(Math.round(Prefs.get(getContext(), N.OPACITY) * 2.55f));
        panel = d;
        invalidate();
    }

    void setApps(List<Apps.App> list) { apps = list; invalidate(); }

    /** Index of the app in the middle. */
    int position() { return apps.isEmpty() ? 0 : Math.floorMod(Math.round(scroll / row), apps.size()); }

    void setPosition(int index) {
        stop();
        scroll = index * row;
        lastCentre = index;
        invalidate();
    }

    /** Moves by whole apps, used by the preview's arrow buttons. */
    void step(int by) {
        flung = true;
        glide((Math.round(scroll / row) + by) * row, 260);
    }

    @Override public boolean performClick() { return super.performClick(); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        gestures.onTouchEvent(e);
        int action = e.getActionMasked();
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && !flung) {
            glide(Math.round(scroll / row) * row, 220);
        }
        return true;
    }

    private void stop() {
        if (motion != null) motion.cancel();
    }

    private void glide(float target, long millis) {
        stop();
        motion = ValueAnimator.ofFloat(scroll, target);
        motion.setDuration(millis);
        motion.setInterpolator(new DecelerateInterpolator(2f));
        motion.addUpdateListener(v -> {
            scroll = (float) v.getAnimatedValue();
            changed();
        });
        motion.start();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void changed() {
        int centre = Math.round(scroll / row);
        if (centre != lastCentre) {
            lastCentre = centre;
            if (haptic) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (panel != null) {
            panel.setBounds(0, 0, getWidth(), getHeight());
            panel.draw(canvas);
        }
        hits.clear();
        hitIndex.clear();
        int n = apps.size();
        if (n == 0) return;

        float cy = getHeight() / 2f;
        // vertical distance between neighbours at the middle
        float gap = (48 * iconScale + spacing) * dp;
        float edge = half + 0.6f;
        float pos = scroll / row;
        int base = (int) Math.floor(pos);
        // never show the same app twice when there are only a few
        int first = Math.max(-half - 1, -((n - 1) / 2));
        int last = Math.min(half + 1, n / 2);
        // squeeze to fit when the chosen spacing is taller than the view
        float fit = Math.min(1f, (cy - 20 * dp) / (half * gap * 0.92f));

        for (int o = first; o <= last; o++) {
            float delta = base + o - pos;
            float d = Math.abs(delta);
            if (d > edge) continue;
            float focus = Math.max(0f, 1f - d);
            float arc = (float) Math.cos(d / edge * Math.PI / 2);
            float y = cy + delta * gap * fit * (1f - 0.08f * d / edge);
            float x = getWidth() - 36 * dp * Math.max(1f, iconScale) - 44 * dp * curve * arc;
            if (left) x = getWidth() - x;
            float size = (26 + 10 * arc + 14 * focus) * dp * iconScale * Math.max(0.7f, fit);
            int alpha = (int) (255 * Math.max(0f, Math.min(1f, (edge - d) / edge * 1.8f)));

            int idx = Math.floorMod(base + o, n);
            Apps.App a = apps.get(idx);
            RectF icon = new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
            iconPaint.setAlpha(alpha);
            canvas.drawBitmap(Apps.bigIcon(getContext(), a), null, icon, iconPaint);
            RectF hit = new RectF(icon);

            if (labels) {
                text.setTextSize((11 + 2 * arc + 5 * focus) * dp * textScale);
                text.setAlpha(alpha);
                text.setTextAlign(Paint.Align.LEFT);
                float room = Math.max(0, (left ? getWidth() - icon.right : icon.left) - 20 * dp);
                String label = TextUtils.ellipsize(a.label, text, room, TextUtils.TruncateAt.END).toString();
                float width = text.measureText(label);
                float start = left ? icon.right + 10 * dp : icon.left - 10 * dp - width;
                canvas.drawText(label, start, y + text.getTextSize() * 0.35f, text);
                hit.union(start, icon.top, start + width, icon.bottom);
            }

            hits.add(hit);
            hitIndex.add(idx);
        }
    }
}
