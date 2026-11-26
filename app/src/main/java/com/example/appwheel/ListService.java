package com.example.appwheel;

import android.content.*;
import android.widget.*;
import java.util.*;

public class ListService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        final Context c = getApplicationContext();
        return new RemoteViewsFactory() {
            private List<Apps.App> apps = new ArrayList<>();

            @Override public void onCreate() {}
            @Override public void onDataSetChanged() { apps = Apps.load(c, 128); }
            @Override public void onDestroy() { apps.clear(); }
            @Override public int getCount() { return apps.size(); }

            @Override public RemoteViews getViewAt(int pos) {
                if (pos < 0 || pos >= apps.size()) return null;
                Apps.App a = apps.get(pos);
                int[] layouts = {R.layout.widget_item_small, R.layout.widget_item, R.layout.widget_item_large};
                RemoteViews v = new RemoteViews(c.getPackageName(), layouts[Prefs.size(c)]);
                v.setTextColor(R.id.label, 0xFF000000 | Prefs.text(c));
                v.setViewVisibility(R.id.label,
                        Prefs.labels(c) ? android.view.View.VISIBLE : android.view.View.GONE);
                v.setImageViewBitmap(R.id.icon, a.icon);
                v.setTextViewText(R.id.label, a.label);
                v.setOnClickFillInIntent(R.id.row,
                        new Intent().putExtra("component", a.component.flattenToString()));
                return v;
            }

            @Override public RemoteViews getLoadingView() { return null; }
            @Override public int getViewTypeCount() { return 3; }
            @Override public long getItemId(int pos) { return pos; }
            @Override public boolean hasStableIds() { return false; }
        };
    }
}
