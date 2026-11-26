package com.example.appwheel;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.OverScroller;
import java.util.*;

/** Endless wheel: apps ride an arc on the right edge; the middle one is the biggest. */
public class WheelView extends View {
    interface Listener { void onPick(Apps.App a); void onDismiss(); }

    private final float dp = getResources().getDisplayMetrics().density;
    private final float row = 76 * dp;           // finger travel per app
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final OverScroller scroller;
    private final GestureDetector gestures;
    private final List<RectF> hits = new ArrayList<>();
    private final List<Integer> hitIndex = new ArrayList<>();
    private List<Apps.App> apps = new ArrayList<>();
    private Listener listener;
    private float scroll;                        // pixels; one app = row
    private int lastCentre;
    private int shape, bgColor;
    private float scale;
    private boolean left, labels, haptic;
    private boolean snapped = true, stoppedFling;

    public WheelView(Context c) {
        super(c);
        scroller = new OverScroller(c);
        reloadStyle();
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) {
                // keep drags here when placed inside another layout
                getParent().requestDisallowInterceptTouchEvent(true);
                stoppedFling = !scroller.isFinished();
                scroller.forceFinished(true);
                snapped = false;
                return true;
            }
            @Override public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                scroll += dy;
                changed();
                return true;
            }
            @Override public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                scroller.fling(0, Math.round(scroll), 0, Math.round(-vy),
                        0, 0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
                postInvalidateOnAnimation();
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
                if (listener != null) listener.onDismiss();
                return true;
            }
        });
        gestures.setIsLongpressEnabled(false);
    }

    void setListener(Listener l) { listener = l; }

    void reloadStyle() {
        Context c = getContext();
        shape = Prefs.shape(c);
        left = Prefs.side(c) == 1;
        scale = new float[] {0.8f, 1f, 1.25f}[Prefs.size(c)];
        bgColor = Prefs.bgArgb(c);
        labels = Prefs.labels(c);
        haptic = Prefs.haptic(c);
        int fg = Prefs.text(c);
        text.setColor(0xFF000000 | fg);
        // dark text gets a light shadow and the other way round
        text.setShadowLayer(4 * dp, 0, dp, fg == 0 ? Color.WHITE : Color.BLACK);
        invalidate();
    }

    void setApps(List<Apps.App> list) { apps = list; invalidate(); }

    @Override public boolean performClick() { return super.performClick(); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        gestures.onTouchEvent(e);
        int action = e.getActionMasked();
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                && scroller.isFinished()) {
            snap();
        }
        return true;
    }

    /** Glide so the nearest app rests exactly in the middle. */
    private void snap() {
        snapped = true;
        int from = Math.round(scroll);
        int to = Math.round(Math.round(scroll / row) * row);
        scroller.startScroll(0, from, 0, to - from, 220);
        postInvalidateOnAnimation();
    }

    @Override public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scroll = scroller.getCurrY();
            changed();
        } else if (!snapped) {
            // a fling just ended; finger is up because onDown force-finishes
            if (!isPressedDown) snap();
        }
    }

    private boolean isPressedDown;

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) isPressedDown = true;
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) isPressedDown = false;
        return super.dispatchTouchEvent(e);
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
        canvas.drawColor(bgColor);
        hits.clear();
        hitIndex.clear();
        int n = apps.size();
        if (n == 0) return;

        float cy = getHeight() / 2f;
        float radius = Math.max(1f, Math.min(getHeight() * 0.40f, 340 * dp));
        // wider angle on small areas so neighbours do not pile up
        float step = Math.max(17f, Math.min(35f, (float) Math.toDegrees(66 * dp * scale / radius)));
        float gap = 72 * dp * scale;             // straight column spacing
        float pos = scroll / row;
        int base = (int) Math.floor(pos);
        int reach = shape == 0 ? 5 : (int) (cy / gap) + 1;
        // never show the same app twice when there are only a few
        int first = Math.max(-reach, -((n - 1) / 2));
        int last = Math.min(reach, n / 2);

        for (int o = first; o <= last; o++) {
            float delta = base + o - pos;
            float focus = Math.max(0f, 1f - Math.abs(delta));
            float x, y, size, fade;              // x is measured for the right side
            if (shape == 0) {
                double ang = Math.toRadians(delta * step);
                if (Math.abs(ang) > Math.PI / 2) continue;
                float cos = (float) Math.cos(ang);
                y = cy + radius * (float) Math.sin(ang);
                x = getWidth() - 56 * dp - radius * 0.55f * (cos - 0.35f);
                size = (36 + 14 * cos + 14 * focus) * dp * scale;
                fade = cos * cos * cos;
            } else {
                y = cy + delta * gap;
                x = getWidth() - 56 * dp;
                size = (48 + 14 * focus) * dp * scale;
                fade = 1f - 0.7f * Math.min(1f, Math.abs(y - cy) / Math.max(1f, cy));
            }
            if (left) x = getWidth() - x;
            int alpha = (int) (255 * Math.max(0.15f, fade));

            int idx = Math.floorMod(base + o, n);
            Apps.App a = apps.get(idx);
            RectF icon = new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
            iconPaint.setAlpha(alpha);
            canvas.drawBitmap(a.icon, null, icon, iconPaint);
            RectF hit = new RectF(icon);

            if (labels) {
                text.setTextSize((14 + 6 * focus) * dp * scale);
                text.setAlpha(alpha);
                text.setTextAlign(Paint.Align.LEFT);
                float room = Math.max(0, (left ? getWidth() - icon.right : icon.left) - 28 * dp);
                String label = TextUtils.ellipsize(a.label, text, room, TextUtils.TruncateAt.END).toString();
                float width = text.measureText(label);
                float start = left ? icon.right + 12 * dp : icon.left - 12 * dp - width;
                canvas.drawText(label, start, y + text.getTextSize() * 0.35f, text);
                hit.union(start, icon.top, start + width, icon.bottom);
            }

            hits.add(hit);
            hitIndex.add(idx);
        }
    }
}
