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

    private static final float STEP_DEG = 17f;   // angle between neighbours

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
    private boolean snapped = true, stoppedFling;

    public WheelView(Context c) {
        super(c);
        scroller = new OverScroller(c);
        text.setColor(Color.WHITE);
        text.setShadowLayer(4 * dp, 0, dp, Color.BLACK);
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) {
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
                        listener.onPick(apps.get(hitIndex.get(k)));
                        return true;
                    }
                }
                listener.onDismiss();
                return true;
            }
        });
        gestures.setIsLongpressEnabled(false);
    }

    void setListener(Listener l) { listener = l; }

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
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0x99000000);
        hits.clear();
        hitIndex.clear();
        int n = apps.size();
        if (n == 0) return;

        float cy = getHeight() / 2f;
        float radius = Math.min(getHeight() * 0.40f, 340 * dp);
        float pos = scroll / row;
        int base = (int) Math.floor(pos);
        // never show the same app twice when there are only a few
        int first = Math.max(-5, -((n - 1) / 2));
        int last = Math.min(5, n / 2);

        for (int o = first; o <= last; o++) {
            float delta = base + o - pos;
            double ang = Math.toRadians(delta * STEP_DEG);
            if (Math.abs(ang) > Math.PI / 2) continue;
            float cos = (float) Math.cos(ang);
            float y = cy + radius * (float) Math.sin(ang);
            float x = getWidth() - 56 * dp - radius * 0.55f * (cos - 0.35f);
            float focus = Math.max(0f, 1f - Math.abs(delta));
            float size = (36 + 14 * cos + 14 * focus) * dp;
            int alpha = (int) (255 * Math.max(0.15f, cos * cos * cos));

            int idx = Math.floorMod(base + o, n);
            Apps.App a = apps.get(idx);
            RectF icon = new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
            iconPaint.setAlpha(alpha);
            canvas.drawBitmap(a.icon, null, icon, iconPaint);

            text.setTextSize((14 + 6 * focus) * dp);
            text.setAlpha(alpha);
            text.setTextAlign(Paint.Align.LEFT);
            float room = Math.max(0, icon.left - 28 * dp);
            String label = TextUtils.ellipsize(a.label, text, room, TextUtils.TruncateAt.END).toString();
            float left = icon.left - 12 * dp - text.measureText(label);
            canvas.drawText(label, left, y + text.getTextSize() * 0.35f, text);

            hits.add(new RectF(left, icon.top, icon.right, icon.bottom));
            hitIndex.add(idx);
        }
    }
}
