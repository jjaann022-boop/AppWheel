package com.example.appwheel;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.List;

/** The swipeable wheel, opened right where the widget sits on the home screen. */
public class WheelActivity extends Activity {
    private WheelView wheel;
    private boolean ready;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
        root.setBackgroundColor(0x33000000);
        root.setOnClickListener(v -> finish());          // tap outside closes
        wheel = new WheelView(this);
        wheel.setListener(a -> {
            Apps.launch(this, a.component);
            finish();
        });
        root.addView(wheel, new FrameLayout.LayoutParams(1, 1));
        setContentView(root);

        Rect widget = getIntent().getSourceBounds();    // set by the home screen
        root.post(() -> place(root, widget));

        new Thread(() -> {
            List<Apps.App> apps = Apps.list(getApplicationContext());
            for (Apps.App a : apps) Apps.bigIcon(getApplicationContext(), a);
            runOnUiThread(() -> {
                wheel.setApps(apps);
                wheel.setPosition(Prefs.position(this));
                ready = true;
            });
        }).start();
    }

    /** Same centre and width as the widget, made taller if needed, kept on screen. */
    private void place(FrameLayout root, Rect widget) {
        float dp = getResources().getDisplayMetrics().density;
        int w = root.getWidth(), h = root.getHeight();
        int[] origin = new int[2];
        root.getLocationOnScreen(origin);
        int width = Math.min(w, Math.max(widget == null ? 0 : widget.width(), Math.round(260 * dp)));
        int height = Math.min(h, Math.max(widget == null ? 0 : widget.height(), Math.round(360 * dp)));
        int cx = widget == null ? w / 2 : widget.centerX() - origin[0];
        int cy = widget == null ? h / 2 : widget.centerY() - origin[1];
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(width, height);
        p.leftMargin = Math.max(0, Math.min(w - width, cx - width / 2));
        p.topMargin = Math.max(0, Math.min(h - height, cy - height / 2));
        wheel.setLayoutParams(p);
    }

    @Override protected void onStop() {
        super.onStop();
        if (ready) {
            // the widget rests on the same app the wheel was left on
            Prefs.of(this).edit().putInt(Prefs.POSITION, wheel.position()).apply();
            AppListWidget.refreshAll(this);
        }
        if (!isChangingConfigurations()) finish();
    }
}
