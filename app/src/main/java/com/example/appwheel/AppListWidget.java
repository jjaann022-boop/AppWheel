package com.example.appwheel;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

public class AppListWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) draw(c, m, id);
        if (Prefs.shape(c) == 1) m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, AppListWidget.class));
        if (ids.length > 0) new AppListWidget().onUpdate(c, m, ids);
    }

    private static void draw(Context c, AppWidgetManager m, int id) {
        m.updateAppWidget(id, Prefs.shape(c) == 0 ? wheel(c) : column(c, id));
    }

    private static RemoteViews wheel(Context c) {
        RemoteViews v = WidgetViews.wheel(c, Prefs.position(c));
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 2,
                new Intent(c, WheelActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
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
        // periodic and system updates pick up newly installed apps
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(i.getAction())) Apps.forget();
        super.onReceive(c, i);
    }
}
