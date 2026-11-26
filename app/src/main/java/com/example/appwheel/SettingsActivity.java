package com.example.appwheel;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.List;

/** Home screen of the app: every change shows in the preview and is saved at once. */
public class SettingsActivity extends Activity {
    private static final int[] BG_COLORS =
            {0x101418, 0x000000, 0xFFFFFF, 0x1D3557, 0x2A9D8F, 0x6A4C93, 0x9B2226, 0x5C4033};
    private static final int[] TEXT_COLORS =
            {0xFFFFFF, 0x000000, 0xFFD166, 0x90E0EF, 0xB7E4C7, 0xFFAFCC};

    private WheelView preview;
    private LinearLayout form;
    private float dp;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setFitsSystemWindows(true);

        // the preview stays put while the options scroll under it
        preview = new WheelView(this);
        page.addView(preview, new LinearLayout.LayoutParams(-1, 0, 2));

        form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(px(16), px(4), px(16), px(24));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 3));

        choice(R.string.shape, Prefs.SHAPE, Prefs.shape(this), R.string.wheel, R.string.column);
        choice(R.string.side, Prefs.SIDE, Prefs.side(this), R.string.right, R.string.left);
        choice(R.string.size, Prefs.SIZE, Prefs.size(this),
                R.string.small, R.string.medium, R.string.large);
        colors(R.string.bg_color, Prefs.BG, BG_COLORS, Prefs.bg(this));
        opacity();
        colors(R.string.text_color, Prefs.TEXT, TEXT_COLORS, Prefs.text(this));
        toggle(R.string.show_labels, Prefs.LABELS, Prefs.labels(this));
        toggle(R.string.haptic, Prefs.HAPTIC, Prefs.haptic(this));

        Button open = new Button(this);
        open.setText(R.string.try_full);
        open.setOnClickListener(v -> startActivity(new Intent(this, WheelActivity.class)));
        form.addView(open);
        note(R.string.widget_note);

        setContentView(page);

        int iconPx = px(80);
        new Thread(() -> {
            List<Apps.App> apps = Apps.load(getApplicationContext(), iconPx);
            runOnUiThread(() -> preview.setApps(apps));
        }).start();
    }

    @Override protected void onPause() {
        super.onPause();
        AppListWidget.refreshAll(this);
    }

    private int px(int v) { return Math.round(v * dp); }

    private void title(int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextSize(16);
        t.setPadding(0, px(14), 0, px(6));
        form.addView(t);
    }

    private void note(int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setPadding(0, px(10), 0, 0);
        form.addView(t);
    }

    private void save(String key, int value) {
        Prefs.of(this).edit().putInt(key, value).apply();
        preview.reloadStyle();
    }

    private void choice(int heading, String key, int current, int... names) {
        title(heading);
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < names.length; i++) {
            final int value = i;
            RadioButton r = new RadioButton(this);
            r.setId(View.generateViewId());
            r.setText(names[i]);
            r.setPadding(0, 0, px(18), 0);
            group.addView(r);
            if (i == current) group.check(r.getId());
            r.setOnClickListener(v -> save(key, value));
        }
        form.addView(group);
    }

    private void colors(int heading, String key, int[] options, int current) {
        title(heading);
        LinearLayout line = new LinearLayout(this);
        View[] dots = new View[options.length];
        for (int i = 0; i < options.length; i++) {
            final int rgb = options[i];
            View dot = new View(this);
            dot.setBackground(swatch(rgb, rgb == current));
            dot.setContentDescription(String.format("#%06X", rgb));
            dot.setOnClickListener(v -> {
                save(key, rgb);
                for (int k = 0; k < dots.length; k++) {
                    dots[k].setBackground(swatch(options[k], options[k] == rgb));
                }
            });
            dots[i] = dot;
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(px(36), px(36));
            p.setMarginEnd(px(10));
            line.addView(dot, p);
        }
        HorizontalScrollView h = new HorizontalScrollView(this);
        h.addView(line);
        form.addView(h);
    }

    private GradientDrawable swatch(int rgb, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xFF000000 | rgb);
        d.setStroke(px(selected ? 4 : 1), selected ? 0xFFFF8800 : Color.GRAY);
        return d;
    }

    private void opacity() {
        title(R.string.opacity);
        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(Prefs.opacity(this));
        bar.setProgressTintList(ColorStateList.valueOf(0xFF2A9D8F));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                if (user) save(Prefs.OPACITY, v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        form.addView(bar);
    }

    private void toggle(int label, String key, boolean current) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(16);
        s.setChecked(current);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(0, px(14), 0, 0);
        s.setOnCheckedChangeListener((v, on) -> {
            Prefs.of(this).edit().putBoolean(key, on).apply();
            preview.reloadStyle();
        });
        form.addView(s);
    }
}
