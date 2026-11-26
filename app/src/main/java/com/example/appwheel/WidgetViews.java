package com.example.appwheel;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.List;
import com.example.appwheel.Prefs.N;

/** Builds the widget's looks. */
final class WidgetViews {
    private static int px(Context c, float dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    /** Background, corners and the optional button bar. */
    static RemoteViews frame(Context c, int layout) {
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        v.setImageViewResource(R.id.bg, Prefs.bgShape(c));
        v.setInt(R.id.bg, "setColorFilter", Prefs.argb(c, N.BG));
        v.setInt(R.id.bg, "setImageAlpha", Math.round(Prefs.get(c, N.OPACITY) * 2.55f));
        int bar = Prefs.get(c, N.BAR);          // 0 none, 1 top, 2 bottom
        v.setViewVisibility(R.id.bar_top, bar == 1 ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.bar_bottom, bar == 2 ? View.VISIBLE : View.GONE);
        return v;
    }

    /** One button bar, added into the top or bottom holder. */
    static RemoteViews bar(Context c) {
        RemoteViews b = new RemoteViews(c.getPackageName(), R.layout.widget_bar);
        int color = Prefs.argb(c, N.TEXT);
        String title = Prefs.title(c);
        b.setTextViewText(R.id.title, title);
        b.setTextColor(R.id.title, color);
        int[] ids = {R.id.up, R.id.down, R.id.wheel, R.id.refresh, R.id.settings};
        N[] show = {N.BTN_ARROWS, N.BTN_ARROWS, N.BTN_WHEEL, N.BTN_REFRESH, N.BTN_SETTINGS};
        for (int i = 0; i < ids.length; i++) {
            b.setInt(ids[i], "setColorFilter", color);
            b.setViewVisibility(ids[i], Prefs.on(c, show[i]) ? View.VISIBLE : View.GONE);
        }
        return b;
    }

    /**
     * The wheel with the app at `position` in the middle. `turn` is 0 at rest; during an
     * animation it is how far (in apps) the wheel still has to travel.
     */
    static void fillWheel(Context c, RemoteViews v, int position, float turn) {
        v.removeAllViews(R.id.slots);
        List<Apps.App> apps = Apps.visible(c);
        int n = apps.size();
        if (n == 0) return;
        int half = Prefs.get(c, N.COUNT) + 1;                // 3, 5 or 7 apps
        boolean left = Prefs.get(c, N.SIDE) == 1;
        float iconScale = Prefs.get(c, N.ICON) / 100f;
        float textScale = Prefs.get(c, N.TEXT_SIZE) / 100f;
        int gap = px(c, Prefs.get(c, N.SPACING) / 2f);
        float curve = Prefs.get(c, N.CURVE);
        for (int k = -half; k <= half; k++) {
            if (n < 2 * half + 1 && Math.abs(k) > (n - 1) / 2) continue;
            Apps.App a = apps.get(Math.floorMod(position + k, n));
            float d = Math.min(half + 0.5f, Math.abs(k + turn));
            float focus = Math.max(0f, 1f - d);
            float arc = (float) Math.cos(d / (half + 0.5f) * Math.PI / 2);
            int alpha = Math.round(255 * Math.max(0.3f, 1f - 0.6f * d / half));
            RemoteViews slot = new RemoteViews(c.getPackageName(),
                    left ? R.layout.slot_left : R.layout.slot_right);
            int indent = px(c, 6 + curve * arc);
            slot.setViewPadding(R.id.row, left ? indent : 0, gap, left ? 0 : indent, gap);
            float dp = (26 + 10 * arc + 12 * focus) * iconScale;
            slot.setImageViewBitmap(R.id.icon, Apps.icon(c, a, px(c, dp)));
            slot.setInt(R.id.icon, "setImageAlpha", alpha);
            slot.setTextViewText(R.id.label, styled(c, a.label));
            slot.setTextViewTextSize(R.id.label, TypedValue.COMPLEX_UNIT_SP,
                    (11 + 2 * arc + 3 * focus) * textScale);
            slot.setTextColor(R.id.label, (alpha << 24) | Prefs.get(c, N.TEXT));
            slot.setViewVisibility(R.id.label, Prefs.on(c, N.LABELS) ? View.VISIBLE : View.GONE);
            v.addView(R.id.slots, slot);
        }
        // slide the whole column while turning
        int shift = Math.round(turn * (px(c, 40 * iconScale) + 2 * gap));
        v.setViewPadding(R.id.slots, 0, Math.max(0, 2 * shift), 0, Math.max(0, -2 * shift));
    }

    static CharSequence styled(Context c, String s) {
        if (!Prefs.on(c, N.BOLD)) return s;
        android.text.SpannableString bold = new android.text.SpannableString(s);
        bold.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, s.length(), 0);
        return bold;
    }

    /** One line of the column shape. */
    static RemoteViews item(Context c, Apps.App a) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_item);
        int gap = px(c, 2 + Prefs.get(c, N.SPACING) / 2f);
        v.setViewPadding(R.id.row, px(c, 8), gap, px(c, 8), gap);
        v.setImageViewBitmap(R.id.icon, Apps.icon(c, a, px(c, 40 * Prefs.get(c, N.ICON) / 100f)));
        v.setTextViewText(R.id.label, styled(c, a.label));
        v.setTextViewTextSize(R.id.label, TypedValue.COMPLEX_UNIT_SP,
                15 * Prefs.get(c, N.TEXT_SIZE) / 100f);
        v.setTextColor(R.id.label, Prefs.argb(c, N.TEXT));
        v.setViewVisibility(R.id.label, Prefs.on(c, N.LABELS) ? View.VISIBLE : View.GONE);
        return v;
    }
}
