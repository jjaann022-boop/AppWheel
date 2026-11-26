package com.example.appwheel;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import com.example.appwheel.Prefs.N;

/** Preview on top, tabbed options in the middle, "Add" at the bottom. */
public class SettingsActivity extends Activity {
    private static final int PAGE = 0xFF0F1115, CARD = 0xFF1A1D24, CHIP = 0xFF272B35;
    private static final int ACCENT = 0xFF6C63FF, MUTED = 0xFF9AA0AE, WHITE = 0xFFFFFFFF;

    private LinearLayout panel, form;
    private LinearLayout barTop, barBottom;
    private WheelView wheel;
    private ListView column;
    private BaseAdapter rows;
    private ScrollView scroll;
    private List<Apps.App> all = new ArrayList<>();
    private List<Apps.App> shown = new ArrayList<>();
    private int tab;
    private int iconFamily = -1;
    private float dp;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        if (b != null) tab = b.getInt("tab");
        getWindow().setStatusBarColor(PAGE);
        getWindow().setNavigationBarColor(PAGE);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        page.setBackgroundColor(PAGE);
        page.setFitsSystemWindows(true);

        TextView title = label(getString(R.string.app_name), 22, WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(px(20), px(10), px(20), px(8));
        page.addView(title);

        page.addView(buildPreview(), margins(-1, px(260), 16, 0, 16, 10));

        LinearLayout tabs = pills(new String[] {"Layout", "Icons", "Style", "Widget", "Apps"}, tab, i -> {
            if (i == tab) return;
            int direction = i > tab ? 1 : -1;
            tab = i;
            switchPage(direction);
        });
        page.addView(tabs, margins(-1, -2, 16, 0, 16, 0));

        form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(px(16), 0, px(16), px(12));
        scroll = new ScrollView(this);
        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView add = button("Add", ACCENT, 28);
        add.setTextSize(18);
        add.setOnClickListener(v -> addWidget());
        page.addView(add, margins(-1, px(54), 16, 8, 16, 12));

        setContentView(page);
        buildPage();
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    @Override protected void onResume() {
        super.onResume();
        new Thread(() -> {
            Apps.forget();
            List<Apps.App> list = Apps.all(getApplicationContext());
            for (Apps.App a : list) Apps.bigIcon(getApplicationContext(), a);
            runOnUiThread(() -> {
                all = list;
                reloadApps();
                if (tab == 1 || tab == 4) buildPage();
            });
        }).start();
    }

    @Override protected void onPause() {
        super.onPause();
        AppListWidget.refreshAll(this);
    }

    private void addWidget() {
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        boolean asked = m.isRequestPinAppWidgetSupported() && m.requestPinAppWidget(
                new ComponentName(this, AppListWidget.class), null, null);
        if (!asked) Toast.makeText(this, R.string.add_by_hand, Toast.LENGTH_LONG).show();
    }

    // ---- preview ----

    private View buildPreview() {
        FrameLayout wallpaper = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF3A4A7A, 0xFF7A4A72});
        g.setCornerRadius(px(24));
        wallpaper.setBackground(g);
        wallpaper.setPadding(px(10), px(10), px(10), px(10));

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(6), px(6), px(6), px(6));
        wallpaper.addView(panel, new FrameLayout.LayoutParams(px(260), -1, Gravity.CENTER));

        barTop = new LinearLayout(this);
        barBottom = new LinearLayout(this);
        wheel = new WheelView(this);
        column = new ListView(this);
        column.setDivider(null);
        column.setVerticalScrollBarEnabled(false);
        rows = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int i) { return shown.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View old, ViewGroup parent) {
                // same description the widget uses, so both look alike
                RemoteViews item = WidgetViews.item(SettingsActivity.this, shown.get(i));
                if (old == null) return item.apply(getApplicationContext(), parent);
                item.reapply(getApplicationContext(), old);
                return old;
            }
        };
        column.setAdapter(rows);

        panel.addView(barTop, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(wheel, new LinearLayout.LayoutParams(-1, 0, 1));
        panel.addView(column, new LinearLayout.LayoutParams(-1, 0, 1));
        panel.addView(barBottom, new LinearLayout.LayoutParams(-1, -2));
        return wallpaper;
    }

    private void reloadApps() {
        shown = Apps.visible(this);
        wheel.setApps(shown);
        wheel.setPosition(Prefs.get(this, N.POSITION));
        rows.notifyDataSetChanged();
    }

    /** Restyles the preview in place. */
    private void render() {
        boolean isWheel = Prefs.get(this, N.SHAPE) == 0;
        Drawable bg = getDrawable(Prefs.bgShape(this)).mutate();
        bg.setTint(Prefs.argb(this, N.BG));
        bg.setAlpha(Math.round(Prefs.get(this, N.OPACITY) * 2.55f));
        panel.setBackground(bg);
        panel.setPadding(px(6), px(6), px(6), px(6));

        View incoming = isWheel ? wheel : column;
        if (incoming.getVisibility() != View.VISIBLE) {
            incoming.setAlpha(0f);
            incoming.animate().alpha(1f).setDuration(300).start();
        }
        wheel.setVisibility(isWheel ? View.VISIBLE : View.GONE);
        column.setVisibility(isWheel ? View.GONE : View.VISIBLE);
        wheel.reloadStyle();
        rows.notifyDataSetChanged();

        barTop.removeAllViews();
        barBottom.removeAllViews();
        int where = Prefs.get(this, N.BAR);
        if (where != 0) {
            LinearLayout holder = where == 1 ? barTop : barBottom;
            View bar = WidgetViews.bar(this).apply(getApplicationContext(), holder);
            bar.findViewById(R.id.up).setOnClickListener(v -> step(-1));
            bar.findViewById(R.id.down).setOnClickListener(v -> step(1));
            holder.addView(bar);
        }
    }

    private void step(int by) {
        if (Prefs.get(this, N.SHAPE) == 0) wheel.step(by);
        else column.smoothScrollByOffset(by * 3);
    }

    // ---- pages ----

    private void switchPage(int direction) {
        form.animate().cancel();
        form.animate().alpha(0f).translationX(-direction * px(40)).setDuration(110)
                .withEndAction(() -> {
                    buildPage();
                    scroll.scrollTo(0, 0);
                    form.setTranslationX(direction * px(40));
                    form.animate().alpha(1f).translationX(0).setDuration(220)
                            .setInterpolator(new DecelerateInterpolator()).start();
                }).start();
    }

    private void buildPage() {
        form.removeAllViews();
        if (tab == 0) layoutPage();
        else if (tab == 1) iconsPage();
        else if (tab == 2) stylePage();
        else if (tab == 3) widgetPage();
        else appsPage();
    }

    private void layoutPage() {
        LinearLayout shape = card("Shape");
        LinearLayout[] wheelOnly = new LinearLayout[3];
        choice(shape, N.SHAPE, new String[] {"Wheel", "Column"}, i -> {
            animateChanges();
            for (LinearLayout card : wheelOnly) card.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        });
        wheelOnly[0] = card("Wheel side");
        // shown as Left, Right but stored as 1, 0
        wheelOnly[0].addView(pills(new String[] {"Left side", "Right side"},
                1 - Prefs.get(this, N.SIDE), i -> apply(N.SIDE, 1 - i)));
        wheelOnly[1] = card("Apps on the wheel");
        choice(wheelOnly[1], N.COUNT, new String[] {"3", "5", "7"}, null);
        wheelOnly[2] = card("Wheel curve");
        slider(wheelOnly[2], "Depth", N.CURVE, 0, 80, "");
        for (LinearLayout card : wheelOnly) {
            card.setVisibility(Prefs.get(this, N.SHAPE) == 0 ? View.VISIBLE : View.GONE);
        }
        LinearLayout sizes = card("Sizes");
        slider(sizes, "Icon size", N.ICON, 60, 150, "%");
        slider(sizes, "Space between apps", N.SPACING, 0, 30, " dp");
    }

    private void iconsPage() {
        int current = Prefs.get(this, N.ICON_STYLE);
        if (iconFamily < 0) iconFamily = current / 100;
        int total = 0;
        for (int i = 0; i < IconStyles.FAMILIES.length; i++) total += IconStyles.count(i);
        LinearLayout card = card(total + " icon styles");
        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        card.addView(pills(IconStyles.FAMILIES, iconFamily, i -> {
            iconFamily = i;
            holder.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                fillIcons(holder);
                holder.animate().alpha(1f).setDuration(200).start();
            }).start();
        }));
        card.addView(holder, margins(-1, -2, 0, 10, 0, 0));
        fillIcons(holder);
        card("Tip").addView(label("Color, Letter and Outline use each app's main color. "
                + "Icon size is in the Layout tab.", 14, WHITE));
    }

    /** A grid of live thumbnails, drawn from one of the user's own apps. */
    private void fillIcons(LinearLayout holder) {
        holder.removeAllViews();
        if (all.isEmpty()) {
            holder.addView(label("Loading apps…", 14, MUTED));
            return;
        }
        // prefer a colorful, familiar app as the sample
        Apps.App sample = all.get(0);
        for (Apps.App a : all) {
            String pkg = a.component.getPackageName();
            if (pkg.equals("com.android.chrome") || pkg.equals("com.whatsapp")) sample = a;
        }
        List<FrameLayout> cells = new ArrayList<>();
        LinearLayout line = null;
        for (int i = 0; i < IconStyles.count(iconFamily); i++) {
            if (i % 4 == 0) {
                line = new LinearLayout(this);
                holder.addView(line, new LinearLayout.LayoutParams(-1, -2));
            }
            final int style = iconFamily * 100 + i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, px(8), 0, px(6));
            ImageView image = new ImageView(this);
            image.setImageBitmap(IconStyles.render(this, sample, style));
            cell.addView(image, new LinearLayout.LayoutParams(px(44), px(44)));
            TextView name = label(IconStyles.name(iconFamily, i), 11, WHITE);
            name.setSingleLine(true);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, px(4), 0, 0);
            cell.addView(name);
            FrameLayout frame = new FrameLayout(this);
            frame.addView(cell);
            frame.setContentDescription(IconStyles.name(iconFamily, i));
            frame.setBackground(Prefs.get(this, N.ICON_STYLE) == style ? round(ACCENT, 14) : null);
            frame.setOnClickListener(v -> {
                Prefs.set(this, N.ICON_STYLE, style);
                Apps.forgetIcons();
                for (FrameLayout other : cells) other.setBackground(null);
                frame.setBackground(round(ACCENT, 14));
                v.setScaleX(0.85f);
                v.setScaleY(0.85f);
                v.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
                render();
                wheel.invalidate();
            });
            cells.add(frame);
            line.addView(frame, new LinearLayout.LayoutParams(0, -2, 1));
        }
        // keep the last row's cells the same width as the others
        while (line != null && line.getChildCount() < 4) {
            line.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        }
    }

    private void stylePage() {
        LinearLayout themes = card("Quick themes");
        String[] names = {"Midnight", "Glass", "Light", "Ocean"};
        int[][] t = {{0x101418, 85, 0xFFFFFF}, {0xFFFFFF, 18, 0xFFFFFF},
                {0xF4F4F8, 94, 0x16181D}, {0x0B3954, 82, 0xBFEFFF}};
        themes.addView(pills(names, -1, i -> {
            Prefs.set(this, N.BG, t[i][0]);
            Prefs.set(this, N.OPACITY, t[i][1]);
            Prefs.set(this, N.TEXT, t[i][2]);
            render();
            form.post(this::buildPage);
        }));
        LinearLayout bg = card("Background");
        colorRow(bg, "Color", N.BG);
        slider(bg, "Opacity", N.OPACITY, 0, 100, "%");
        choice(card("Corners"), N.CORNERS, new String[] {"Square", "Soft", "Round", "Pill"}, null);
        LinearLayout text = card("App names");
        toggle(text, "Show names", N.LABELS);
        toggle(text, "Bold", N.BOLD);
        colorRow(text, "Text color", N.TEXT);
        slider(text, "Text size", N.TEXT_SIZE, 70, 150, "%");
    }

    private void widgetPage() {
        LinearLayout bar = card("Button bar");
        LinearLayout buttons = card("Bar content");
        choice(bar, N.BAR, new String[] {"Off", "Top", "Bottom"}, i -> {
            animateChanges();
            buttons.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        });
        buttons.setVisibility(Prefs.get(this, N.BAR) == 0 ? View.GONE : View.VISIBLE);
        toggle(buttons, "Up / down buttons", N.BTN_ARROWS);
        toggle(buttons, "Open wheel button", N.BTN_WHEEL);
        toggle(buttons, "Refresh button", N.BTN_REFRESH);
        toggle(buttons, "Settings button", N.BTN_SETTINGS);
        EditText name = new EditText(this);
        name.setHint("Title (optional)");
        name.setText(Prefs.title(this));
        name.setSingleLine(true);
        name.setTextColor(WHITE);
        name.setHintTextColor(MUTED);
        name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(24)});
        name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) {
                Prefs.setTitle(SettingsActivity.this, e.toString());
                render();
            }
        });
        buttons.addView(name);

        LinearLayout motion = card("Animation");
        toggle(motion, "Animations on widget and wheel", N.ANIM);
        motion.addView(label("Up / down speed on the widget wheel", 13, MUTED));
        choice(motion, N.ANIM_SPEED, new String[] {"Slow", "Normal", "Fast"}, null);

        LinearLayout swipe = card("Swipe wheel");
        slider(swipe, "Spin speed", N.SPIN, 40, 180, "%");
        slider(swipe, "Dim the home screen", N.DIM, 0, 80, "%");
        toggle(swipe, "Vibrate while turning", N.HAPTIC);

        TextView reset = button("Reset all settings", CHIP, 16);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Reset all settings?")
                .setMessage("Colors, sizes, buttons and hidden apps go back to the start.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Reset", (d, w) -> {
                    Prefs.of(this).edit().clear().apply();
                    reloadApps();
                    render();
                    buildPage();
                }).show());
        form.addView(reset, margins(-1, px(48), 0, 14, 0, 0));
    }

    private void appsPage() {
        choice(card("Order"), N.SORT, new String[] {"A – Z", "Z – A", "Newest"}, i -> reloadApps());
        LinearLayout list = card("Choose which apps appear");
        LinearLayout both = new LinearLayout(this);
        TextView showAll = button("Show all", CHIP, 14);
        TextView hideAll = button("Hide all", CHIP, 14);
        showAll.setOnClickListener(v -> {
            Prefs.setHidden(this, new java.util.HashSet<>());
            reloadApps();
            buildPage();
        });
        hideAll.setOnClickListener(v -> {
            Set<String> every = new java.util.HashSet<>();
            for (Apps.App a : all) every.add(a.component.flattenToString());
            Prefs.setHidden(this, every);
            reloadApps();
            buildPage();
        });
        both.addView(showAll, margins(0, px(40), 0, 0, 4, 0, 1));
        both.addView(hideAll, margins(0, px(40), 4, 0, 0, 0, 1));
        list.addView(both);
        if (all.isEmpty()) list.addView(label("Loading apps…", 14, MUTED));
        Set<String> hidden = Prefs.hidden(this);
        for (Apps.App a : all) {
            String id = a.component.flattenToString();
            CheckBox box = new CheckBox(this);
            box.setText(a.label);
            box.setTextColor(WHITE);
            box.setButtonTintList(ColorStateList.valueOf(ACCENT));
            box.setChecked(!hidden.contains(id));
            box.setOnCheckedChangeListener((v, on) -> {
                Set<String> now = Prefs.hidden(this);
                if (on) now.remove(id);
                else now.add(id);
                Prefs.setHidden(this, now);
                reloadApps();
            });
            list.addView(box, new LinearLayout.LayoutParams(-1, px(44)));
        }
    }

    // ---- building blocks ----

    private void apply(N key, int value) {
        Prefs.set(this, key, value);
        render();
    }

    private void animateChanges() {
        TransitionManager.beginDelayedTransition(form, new AutoTransition().setDuration(220));
    }

    private int px(float v) { return Math.round(v * dp); }

    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(px(l), px(t), px(r), px(b));
        return p;
    }

    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b, float weight) {
        LinearLayout.LayoutParams p = margins(w, h, l, t, r, b);
        p.weight = weight;
        return p;
    }

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

    /** A rounded button that shrinks a little while pressed. */
    private TextView button(String s, int color, int radius) {
        TextView t = label(s, 14, WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(color, radius));
        t.setClickable(true);
        t.setFocusable(true);
        t.setOnTouchListener((v, e) -> {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });
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
        form.addView(card, margins(-1, -2, 0, 10, 0, 0));
        return card;
    }

    /** Segmented control whose highlight slides to the chosen item. */
    private LinearLayout pills(String[] names, int current, IntConsumer picked) {
        LinearLayout outer = new LinearLayout(this);
        outer.setBackground(round(CHIP, 14));
        outer.setPadding(px(4), px(4), px(4), px(4));
        FrameLayout stack = new FrameLayout(this);
        outer.addView(stack, new LinearLayout.LayoutParams(-1, px(40)));
        View mark = new View(this);
        mark.setBackground(round(ACCENT, 11));
        mark.setVisibility(current < 0 ? View.INVISIBLE : View.VISIBLE);
        stack.addView(mark, new FrameLayout.LayoutParams(0, -1));
        LinearLayout line = new LinearLayout(this);
        stack.addView(line, new FrameLayout.LayoutParams(-1, -1));
        int[] chosen = {current};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView t = label(names[i], 14, WHITE);
            t.setGravity(Gravity.CENTER);
            t.setOnClickListener(v -> {
                int width = stack.getWidth() / names.length;
                if (chosen[0] < 0) mark.setTranslationX(index * width);
                mark.setVisibility(View.VISIBLE);
                mark.animate().translationX(index * width).setDuration(250)
                        .setInterpolator(new DecelerateInterpolator(1.6f)).start();
                chosen[0] = index;
                picked.accept(index);
            });
            line.addView(t, new LinearLayout.LayoutParams(0, -1, 1));
        }
        stack.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int width = (r - l) / names.length;
            if (mark.getLayoutParams().width != width) {
                mark.getLayoutParams().width = width;
                mark.setTranslationX(Math.max(0, chosen[0]) * width);
                mark.post(mark::requestLayout);
            }
        });
        return outer;
    }

    private void choice(LinearLayout card, N key, String[] names, IntConsumer also) {
        card.addView(pills(names, Prefs.get(this, key), i -> {
            apply(key, i);
            if (also != null) also.accept(i);
        }));
    }

    private void toggle(LinearLayout card, String name, N key) {
        Switch s = new Switch(this);
        s.setText(name);
        s.setTextColor(WHITE);
        s.setTextSize(15);
        s.setChecked(Prefs.on(this, key));
        s.setOnCheckedChangeListener((v, on) -> apply(key, on ? 1 : 0));
        card.addView(s, new LinearLayout.LayoutParams(-1, px(46)));
    }

    private void slider(LinearLayout card, String name, N key, int min, int max, String unit) {
        TextView value = label(name + "  " + Prefs.get(this, key) + unit, 15, WHITE);
        value.setPadding(0, px(8), 0, px(4));
        card.addView(value);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(Prefs.get(this, key) - min);
        bar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        bar.setThumbTintList(ColorStateList.valueOf(ACCENT));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                if (!user) return;
                value.setText(name + "  " + (v + min) + unit);
                apply(key, v + min);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        card.addView(bar);
    }

    private void colorRow(LinearLayout card, String name, N key) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(label(name, 15, WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        View dot = new View(this);
        Runnable paint = () -> {
            GradientDrawable d = round(Prefs.argb(this, key), 18);
            d.setStroke(px(2), MUTED);
            dot.setBackground(d);
        };
        paint.run();
        line.addView(dot, new LinearLayout.LayoutParams(px(36), px(36)));
        line.setOnClickListener(v -> ColorPicker.show(this, name, Prefs.get(this, key), rgb -> {
            apply(key, rgb);
            paint.run();
        }));
        card.addView(line, new LinearLayout.LayoutParams(-1, px(46)));
    }
}
