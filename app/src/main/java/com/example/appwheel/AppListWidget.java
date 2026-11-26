package com.example.appwheel;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

public class AppListWidget extends AppWidgetProvider {
    private static final String REFRESH = "com.example.appwheel.REFRESH";

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
            Intent svc = new Intent(c, ListService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
            v.setRemoteAdapter(R.id.list, svc);

            int mutable = Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
            v.setPendingIntentTemplate(R.id.list, PendingIntent.getActivity(c, 0,
                    new Intent(c, LaunchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT | mutable));

            v.setInt(R.id.bg, "setColorFilter", 0xFF000000 | Prefs.bg(c));
            v.setInt(R.id.bg, "setImageAlpha", Math.round(Prefs.opacity(c) * 2.55f));
            v.setTextColor(R.id.open_wheel, 0xFF000000 | Prefs.text(c));
            v.setTextColor(R.id.refresh, 0xFF000000 | Prefs.text(c));

            v.setOnClickPendingIntent(R.id.open_wheel, PendingIntent.getActivity(c, 1,
                    new Intent(c, WheelActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

            v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 2,
                    new Intent(c, AppListWidget.class).setAction(REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

            m.updateAppWidget(id, v);
        }
        m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, AppListWidget.class));
        if (ids.length > 0) new AppListWidget().onUpdate(c, m, ids);
    }

    @Override public void onReceive(Context c, Intent i) {
        if (REFRESH.equals(i.getAction())) {
            refreshAll(c);
        } else {
            super.onReceive(c, i);
        }
    }
}
