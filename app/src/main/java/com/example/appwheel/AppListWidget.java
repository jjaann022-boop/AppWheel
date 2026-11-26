package com.example.appwheel;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import com.example.appwheel.Prefs.N;

public class AppListWidget extends AppWidgetProvider {
    private static final String PRESS = "com.example.appwheel.PRESS";
    private static final int UP = 1, DOWN = 2, REFRESH = 3;
    private static final int FRAMES = 5;

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c, id));
        if (Prefs.get(c, N.SHAPE) == 1) m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, AppListWidget.class));
        if (ids.length > 0) new AppListWidget().onUpdate(c, m, ids);
    }

    private static RemoteViews build(Context c, int id) {
        boolean wheel = Prefs.get(c, N.SHAPE) == 0;
        RemoteViews v = WidgetViews.frame(c, wheel ? R.layout.widget_wheel : R.layout.widget_column);
        addBar(c, v, id);
        if (wheel) {
            WidgetViews.fillWheel(c, v, Prefs.get(c, N.POSITION), 0);
            v.setOnClickPendingIntent(R.id.root, screen(c, WheelActivity.class, 2));
        } else {
            Intent svc = new Intent(c, ListService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
            v.setRemoteAdapter(R.id.list, svc);
            int mutable = Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
            v.setPendingIntentTemplate(R.id.list, PendingIntent.getActivity(c, 1,
                    new Intent(c, LaunchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT | mutable));
        }
        return v;
    }

    private static void addBar(Context c, RemoteViews v, int id) {
        int where = Prefs.get(c, N.BAR);
        v.removeAllViews(R.id.bar_top);
        v.removeAllViews(R.id.bar_bottom);
        if (where == 0) return;
        RemoteViews bar = WidgetViews.bar(c);
        bar.setOnClickPendingIntent(R.id.up, press(c, id, UP));
        bar.setOnClickPendingIntent(R.id.down, press(c, id, DOWN));
        bar.setOnClickPendingIntent(R.id.refresh, press(c, id, REFRESH));
        bar.setOnClickPendingIntent(R.id.wheel, screen(c, WheelActivity.class, 2));
        bar.setOnClickPendingIntent(R.id.settings, screen(c, SettingsActivity.class, 3));
        v.addView(where == 1 ? R.id.bar_top : R.id.bar_bottom, bar);
    }

    private static PendingIntent screen(Context c, Class<?> target, int code) {
        return PendingIntent.getActivity(c, code,
                new Intent(c, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent press(Context c, int id, int what) {
        Intent i = new Intent(c, AppListWidget.class).setAction(PRESS)
                .setData(Uri.parse("appwheel://press/" + id + "/" + what))
                .putExtra("id", id).putExtra("what", what);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public void onReceive(Context c, Intent i) {
        if (!PRESS.equals(i.getAction())) {
            // periodic and system updates pick up newly installed apps
            if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(i.getAction())) Apps.forget();
            super.onReceive(c, i);
            return;
        }
        int id = i.getIntExtra("id", 0);
        int what = i.getIntExtra("what", 0);
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        if (what == REFRESH) {
            Apps.forget();
            refreshAll(c);
        } else if (Prefs.get(c, N.SHAPE) == 1) {
            // the list view animates this scroll itself
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_column);
            v.setRelativeScrollPosition(R.id.list, what == UP ? -3 : 3);
            m.partiallyUpdateAppWidget(id, v);
        } else {
            turn(c, m, id, what == UP ? -1 : 1);
        }
    }

    /** Moves the wheel one app, drawing a few in-between frames. */
    private void turn(Context c, AppWidgetManager m, int id, int by) {
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                synchronized (AppListWidget.class) {
                    int n = Math.max(1, Apps.visible(c).size());
                    int next = Math.floorMod(Prefs.get(c, N.POSITION) + by, n);
                    Prefs.set(c, N.POSITION, next);
                    int frames = Prefs.on(c, N.ANIM) ? FRAMES : 1;
                    long pause = new long[] {55, 35, 20}[Prefs.get(c, N.ANIM_SPEED)];
                    for (int f = 1; f <= frames; f++) {
                        float left = 1f - (float) f / frames;
                        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_wheel);
                        WidgetViews.fillWheel(c, v, next, by * left * left);
                        m.partiallyUpdateAppWidget(id, v);
                        if (f < frames) Thread.sleep(pause);
                    }
                    // a full update afterwards so partial ones do not pile up
                    m.updateAppWidget(id, build(c, id));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pending.finish();
            }
        }).start();
    }
}
