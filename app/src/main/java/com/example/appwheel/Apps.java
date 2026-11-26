package com.example.appwheel;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.*;

final class Apps {
    static final class App {
        String label;
        ComponentName component;
        ResolveInfo info;
    }

    private static List<App> cache;
    private static final Map<String, Bitmap> icons = new java.util.concurrent.ConcurrentHashMap<>();
    static final int ICON_PX = 192;

    static Bitmap bigIcon(Context c, App a) {
        return icons.computeIfAbsent(a.component.flattenToString(), k -> icon(c, a, ICON_PX));
    }

    /** All launchable apps sorted by name; icons are drawn only when needed. */
    static synchronized List<App> list(Context c) {
        if (cache != null) return cache;
        PackageManager pm = c.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<App> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(query, 0)) {
            if (r.activityInfo.packageName.equals(c.getPackageName())) continue;
            App a = new App();
            a.info = r;
            a.label = r.loadLabel(pm).toString();
            a.component = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
            out.add(a);
        }
        Collator col = Collator.getInstance();
        out.sort((x, y) -> col.compare(x.label, y.label));
        cache = out;
        return out;
    }

    static synchronized void forget() {
        cache = null;
        icons.clear();
    }

    static Bitmap icon(Context c, App a, int px) {
        Drawable d = a.info.loadIcon(c.getPackageManager());
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        b.setDensity(c.getResources().getDisplayMetrics().densityDpi);
        d.setBounds(0, 0, px, px);
        d.draw(new Canvas(b));
        return b;
    }

    static void launch(Context c, ComponentName cn) {
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            android.widget.Toast.makeText(c, R.string.cant_open, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
