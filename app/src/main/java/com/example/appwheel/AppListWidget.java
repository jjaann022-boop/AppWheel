package com.example.appwheel;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

public class AppListWidget extends AppWidgetProvider {
    private static final String TURN = "com.example.appwheel.TURN";

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        Apps.forget();
        for (int id : ids) draw(c, m, id);
        if (Prefs.shape(c) == 1) m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, AppListWidget.class));
        if (ids.length > 0) new AppListWidget().onUpdate(c, m, ids);
    }

    private static void draw(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, Prefs.shape(c) == 0 ? wheel(c, id) : column(c, id));
    }

    private static RemoteViews wheel(Context c, int id) {
        RemoteViews v = WidgetViews.wheel(c, Prefs.of(c).getInt("offset_" + id, 0), (slot, app) -> {
            String cn = app.component.flattenToString();
            Intent open = new Intent(c, LaunchActivity.class)
                    .setData(Uri.parse("appwheel://open/" + Uri.encode(cn)))
                    .putExtra("component", cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            slot.setOnClickPendingIntent(R.id.row, PendingIntent.getActivity(c, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        });
        v.setOnClickPendingIntent(R.id.up, turn(c, id, -1));
        v.setOnClickPendingIntent(R.id.down, turn(c, id, 1));
        return v;
    }

    private static PendingIntent turn(Context c, int id, int by) {
        Intent i = new Intent(c, AppListWidget.class).setAction(TURN)
                .setData(Uri.parse("appwheel://turn/" + id + "/" + by))
                .putExtra("id", id).putExtra("by", by);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static RemoteViews column(Context c, int id) {
        RemoteViews v = WidgetViews.frame(c, R.layout.widget_column);
        Intent svc = new Intent(c, ListService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.list, svc);
        int mutable = Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
        v.setPendingIntentTemplate(R.id.list, PendingIntent.getActivity(c, 1,
                new Intent(c, LaunchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | mutable));
        return v;
    }

    @Override public void onReceive(Context c, Intent i) {
        if (TURN.equals(i.getAction())) {
            int id = i.getIntExtra("id", 0);
            String key = "offset_" + id;
            int n = Math.max(1, Apps.list(c).size());
            int next = Math.floorMod(Prefs.of(c).getInt(key, 0) + i.getIntExtra("by", 0), n);
            Prefs.of(c).edit().putInt(key, next).apply();
            draw(c, AppWidgetManager.getInstance(c), id);
        } else {
            super.onReceive(c, i);
        }
    }

    @Override public void onDeleted(Context c, int[] ids) {
        for (int id : ids) Prefs.of(c).edit().remove("offset_" + id).apply();
    }
}
