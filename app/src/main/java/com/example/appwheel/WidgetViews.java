package com.example.appwheel;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.List;

/** Builds the widget's looks. The settings preview uses the same code, so they match. */
final class WidgetViews {
    private static final int[] INDENT = {6, 34, 50, 34, 6};     // dp from the edge: the arc
    private static final int[] ICON = {28, 36, 50, 36, 28};     // dp
    private static final int[] TEXT = {11, 13, 16, 13, 11};     // sp
    private static final int[] ALPHA = {110, 190, 255, 190, 110};

    static float scale(Context c) { return new float[] {0.82f, 1f, 1.2f}[Prefs.size(c)]; }

    private static int px(Context c, float dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    static RemoteViews frame(Context c, int layout) {
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        v.setInt(R.id.bg, "setColorFilter", 0xFF000000 | Prefs.bg(c));
        v.setInt(R.id.bg, "setImageAlpha", Math.round(Prefs.opacity(c) * 2.55f));
        return v;
    }

    /** Wheel with the app at `offset` in the middle. No arrows: it is a resting picture of the wheel. */
    static RemoteViews wheel(Context c, int offset) {
        RemoteViews v = frame(c, R.layout.widget_wheel);
        v.removeAllViews(R.id.slots);
        List<Apps.App> apps = Apps.list(c);
        int n = apps.size();
        if (n == 0) return v;
        boolean left = Prefs.side(c) == 1;
        float s = scale(c);
        for (int k = 0; k < 5; k++) {
            if (n < 5 && Math.abs(k - 2) > (n - 1) / 2) continue;
            Apps.App a = apps.get(Math.floorMod(offset + k - 2, n));
            RemoteViews slot = new RemoteViews(c.getPackageName(),
                    left ? R.layout.slot_left : R.layout.slot_right);
            int indent = px(c, INDENT[k]);
            slot.setViewPadding(R.id.row, left ? indent : 0, 0, left ? 0 : indent, 0);
            slot.setImageViewBitmap(R.id.icon, Apps.icon(c, a, px(c, ICON[k] * s)));
            slot.setInt(R.id.icon, "setImageAlpha", ALPHA[k]);
            slot.setTextViewText(R.id.label, a.label);
            slot.setTextViewTextSize(R.id.label, TypedValue.COMPLEX_UNIT_SP, TEXT[k] * s);
            slot.setTextColor(R.id.label, (ALPHA[k] << 24) | Prefs.text(c));
            slot.setViewVisibility(R.id.label, Prefs.labels(c) ? View.VISIBLE : View.GONE);
            v.addView(R.id.slots, slot);
        }
        return v;
    }

    /** One line of the column shape. */
    static RemoteViews item(Context c, Apps.App a) {
        int[] layouts = {R.layout.widget_item_small, R.layout.widget_item, R.layout.widget_item_large};
        RemoteViews v = new RemoteViews(c.getPackageName(), layouts[Prefs.size(c)]);
        v.setImageViewBitmap(R.id.icon, Apps.icon(c, a, 128));
        v.setTextViewText(R.id.label, a.label);
        v.setTextColor(R.id.label, 0xFF000000 | Prefs.text(c));
        v.setViewVisibility(R.id.label, Prefs.labels(c) ? View.VISIBLE : View.GONE);
        return v;
    }
}
