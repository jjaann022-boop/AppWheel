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

    @Override public void onReceive(Context c, Intent i) {
        if (REFRESH.equals(i.getAction())) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            onUpdate(c, m, m.getAppWidgetIds(new ComponentName(c, AppListWidget.class)));
        } else {
            super.onReceive(c, i);
        }
    }
}
