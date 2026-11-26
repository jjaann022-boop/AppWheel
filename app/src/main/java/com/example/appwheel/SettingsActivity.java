package com.example.appwheel;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.List;

/** Home screen of the app: preview on top, options in cards, "Add" at the bottom. */
public class SettingsActivity extends Activity {
    private static final int PAGE = 0xFF0F1115, CARD = 0xFF1A1D24, CHIP = 0xFF272B35;
    private static final int ACCENT = 0xFF6C63FF, MUTED = 0xFF9AA0AE, WHITE = 0xFFFFFFFF;

    private FrameLayout preview;
    private LinearLayout form;
    private View sideCard;
    private float dp;
    private int offset;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(PAGE);
        getWindow().setNavigationBarColor(PAGE);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        page.setBackgroundColor(PAGE);
        page.setFitsSystemWindows(true);

        TextView title = label(getString(R.string.app_name), 24, WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(px(20), px(14), px(20), px(10));
        page.addView(title);

        // stands in for the wallpaper behind the widget
        preview = new FrameLayout(this);
        GradientDrawable wallpaper = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF3A4A7A, 0xFF7A4A72});
        wallpaper.setCornerRadius(px(24));
        preview.setBackground(wallpaper);
        preview.setPadding(px(12), px(12), px(12), px(12));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, px(270));
        pp.setMargins(px(16), 0, px(16), px(4));
        page.addView(preview, pp);

        form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(px(16), 0, px(16), px(12));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        segments(card("Shape"), Prefs.SHAPE, Prefs.shape(this), "Wheel", "Column");
        LinearLayout side = card("Wheel side");
        sideCard = side;
        segments(side, Prefs.SIDE, Prefs.side(this), "Left side", "Right side");
        segments(card("Size"), Prefs.SIZE, Prefs.size(this), "Small", "Medium", "Large");

        LinearLayout bg = card("Background");
        colorRow(bg, "Color", Prefs.BG);
        opacity(bg);

        LinearLayout names = card("App names");
        Switch show = new Switch(this);
        show.setText("Show names");
        show.setTextColor(WHITE);
        show.setTextSize(15);
        show.setChecked(Prefs.labels(this));
        show.setPadding(0, px(4), 0, px(10));
        show.setOnCheckedChangeListener((v, on) -> {
            Prefs.of(this).edit().putBoolean(Prefs.LABELS, on).apply();
            render();
        });
        names.addView(show);
        colorRow(names, "Text color", Prefs.TEXT);

        TextView add = label("Add", 18, WHITE);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setGravity(Gravity.CENTER);
        add.setBackground(round(ACCENT, 28));
        add.setClickable(true);
        add.setFocusable(true);
        add.setOnClickListener(v -> addWidget());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, px(56));
        ap.setMargins(px(16), px(8), px(16), px(14));
        page.addView(add, ap);

        setContentView(page);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        Apps.forget();
        render();
    }

    @Override protected void onPause() {
        super.onPause();
        AppListWidget.refreshAll(this);
    }

    private void addWidget() {
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        boolean asked = m.isRequestPinAppWidgetSupported() && m.requestPinAppWidget(
                new ComponentName(this, AppListWidget.class), null, null);
        if (!asked) {
            Toast.makeText(this, R.string.add_by_hand, Toast.LENGTH_LONG).show();
        }
    }

    /** Draws the preview with the same views the widget uses. */
    private void render() {
        sideCard.setVisibility(Prefs.shape(this) == 0 ? View.VISIBLE : View.GONE);
        preview.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(250), -1, Gravity.CENTER);
        if (Prefs.shape(this) == 0) {
            View v = WidgetViews.wheel(this, offset, null)
                    .apply(getApplicationContext(), preview);
            v.findViewById(R.id.up).setOnClickListener(x -> { offset--; render(); });
            v.findViewById(R.id.down).setOnClickListener(x -> { offset++; render(); });
            preview.addView(v, lp);
        } else {
            View v = WidgetViews.frame(this, R.layout.widget_preview_column)
                    .apply(getApplicationContext(), preview);
            ViewGroup rows = v.findViewById(R.id.rows);
            List<Apps.App> apps = Apps.list(this);
            for (int i = 0; i < Math.min(12, apps.size()); i++) {
                rows.addView(WidgetViews.item(this, apps.get(i))
                        .apply(getApplicationContext(), rows));
            }
            preview.addView(v, lp);
        }
    }

    private int px(float v) { return Math.round(v * dp); }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(px(radius));
        return d;
    }

    private TextView label(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout card(String heading) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(CARD, 20));
        card.setPadding(px(16), px(12), px(16), px(14));
        TextView h = label(heading, 13, MUTED);
        h.setPadding(0, 0, 0, px(8));
        card.addView(h);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = px(10);
        form.addView(card, p);
        return card;
    }

    private void segments(LinearLayout card, String key, int current, String... names) {
        LinearLayout bar = new LinearLayout(this);
        bar.setBackground(round(CHIP, 14));
        bar.setPadding(px(4), px(4), px(4), px(4));
        TextView[] items = new TextView[names.length];
        // "Left side" is shown first but stored as 1
        boolean mirrored = Prefs.SIDE.equals(key);
        for (int i = 0; i < names.length; i++) {
            final int value = mirrored ? 1 - i : i;
            TextView t = label(names[i], 15, WHITE);
            t.setGravity(Gravity.CENTER);
            t.setBackground(value == current ? round(ACCENT, 11) : null);
            t.setOnClickListener(v -> {
                Prefs.of(this).edit().putInt(key, value).apply();
                for (TextView other : items) other.setBackground(null);
                t.setBackground(round(ACCENT, 11));
                render();
            });
            items[i] = t;
            bar.addView(t, new LinearLayout.LayoutParams(0, px(40), 1));
        }
        card.addView(bar);
    }

    private void colorRow(LinearLayout card, String name, String key) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(label(name, 15, WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        View dot = new View(this);
        Runnable paint = () -> {
            boolean isBg = Prefs.BG.equals(key);
            GradientDrawable d = round(0xFF000000 | (isBg ? Prefs.bg(this) : Prefs.text(this)), 18);
            d.setStroke(px(2), MUTED);
            dot.setBackground(d);
        };
        paint.run();
        line.addView(dot, new LinearLayout.LayoutParams(px(36), px(36)));
        line.setMinimumHeight(px(44));
        line.setOnClickListener(v -> ColorPicker.show(this, name,
                Prefs.BG.equals(key) ? Prefs.bg(this) : Prefs.text(this), rgb -> {
                    Prefs.of(this).edit().putInt(key, rgb).apply();
                    paint.run();
                    render();
                }));
        card.addView(line);
    }

    private void opacity(LinearLayout card) {
        TextView value = label("", 15, WHITE);
        value.setPadding(0, px(8), 0, px(4));
        value.setText("Opacity  " + Prefs.opacity(this) + "%");
        card.addView(value);
        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(Prefs.opacity(this));
        bar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        bar.setThumbTintList(ColorStateList.valueOf(ACCENT));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                if (!user) return;
                Prefs.of(SettingsActivity.this).edit().putInt(Prefs.OPACITY, v).apply();
                value.setText("Opacity  " + v + "%");
                render();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        card.addView(bar);
    }
}
