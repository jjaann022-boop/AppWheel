package com.example.appwheel;

import android.content.Context;
import android.content.SharedPreferences;

/** User choices, shared by the settings screen and the widget. */
final class Prefs {
    static final String SHAPE = "shape";        // 0 wheel, 1 column
    static final String SIDE = "side";          // 0 right, 1 left
    static final String SIZE = "size";          // 0 small, 1 medium, 2 large
    static final String BG = "bg";              // RGB
    static final String OPACITY = "opacity";    // percent
    static final String TEXT = "text";          // RGB
    static final String LABELS = "labels";
    static final String POSITION = "position";

    static SharedPreferences of(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
    static int shape(Context c) { return of(c).getInt(SHAPE, 0); }
    static int side(Context c) { return of(c).getInt(SIDE, 0); }
    static int size(Context c) { return of(c).getInt(SIZE, 1); }
    static int bg(Context c) { return of(c).getInt(BG, 0x101418); }
    static int opacity(Context c) { return of(c).getInt(OPACITY, 70); }
    static int text(Context c) { return of(c).getInt(TEXT, 0xFFFFFF); }
    static int position(Context c) { return of(c).getInt(POSITION, 0); }
    static boolean labels(Context c) { return of(c).getBoolean(LABELS, true); }
    static int bgArgb(Context c) { return (Math.round(opacity(c) * 2.55f) << 24) | bg(c); }
}
