package com.example.appwheel;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** User choices, shared by the settings screen, the widget and the wheel. */
final class Prefs {
    // key and default of every number setting
    enum N {
        SHAPE(0), SIDE(0), COUNT(1), ICON(100), TEXT_SIZE(100), SPACING(6), CURVE(44),
        BG(0x101418), OPACITY(70), TEXT(0xFFFFFF), CORNERS(2),
        BAR(0), ANIM(1), ANIM_SPEED(1),
        SORT(0), SPIN(100), DIM(20),
        LABELS(1), BOLD(0), HAPTIC(1),
        BTN_ARROWS(1), BTN_WHEEL(1), BTN_REFRESH(0), BTN_SETTINGS(1),
        ICON_STYLE(0),
        POSITION(0);

        final int fallback;
        N(int fallback) { this.fallback = fallback; }
        String key() { return "n_" + name(); }
    }

    static SharedPreferences of(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
    static int get(Context c, N n) { return of(c).getInt(n.key(), n.fallback); }
    static boolean on(Context c, N n) { return get(c, n) != 0; }
    static void set(Context c, N n, int value) { of(c).edit().putInt(n.key(), value).apply(); }

    static String title(Context c) { return of(c).getString("title", ""); }
    static void setTitle(Context c, String s) { of(c).edit().putString("title", s).apply(); }

    static Set<String> hidden(Context c) {
        return new HashSet<>(of(c).getStringSet("hidden", new HashSet<>()));
    }
    static void setHidden(Context c, Set<String> s) { of(c).edit().putStringSet("hidden", s).apply(); }

    static int argb(Context c, N color) { return 0xFF000000 | get(c, color); }
    static int bgArgb(Context c) {
        return (Math.round(get(c, N.OPACITY) * 2.55f) << 24) | get(c, N.BG);
    }
    static int bgShape(Context c) {
        int[] d = {R.drawable.bg_square, R.drawable.bg_soft, R.drawable.widget_bg, R.drawable.bg_pill};
        return d[get(c, N.CORNERS)];
    }
}
