package com.example.appwheel;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class Apps {
    static final class App {
        String label;
        ComponentName component;
        ResolveInfo info;
        long installed;
    }

    static final int ICON_PX = 192;
    private static List<App> cache;
    private static final Map<String, Bitmap> icons = new ConcurrentHashMap<>();

    /** Every launchable app. */
    static synchronized List<App> all(Context c) {
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
            try {
                a.installed = pm.getPackageInfo(r.activityInfo.packageName, 0).firstInstallTime;
            } catch (PackageManager.NameNotFoundException ignored) {
                a.installed = 0;
            }
            out.add(a);
        }
        Collator col = Collator.getInstance();
        out.sort((x, y) -> col.compare(x.label, y.label));
        cache = out;
        return out;
    }

    /** The apps the user chose to show, in the chosen order. */
    static List<App> visible(Context c) {
        Set<String> hidden = Prefs.hidden(c);
        List<App> out = new ArrayList<>();
        for (App a : all(c)) {
            if (!hidden.contains(a.component.flattenToString())) out.add(a);
        }
        int sort = Prefs.get(c, Prefs.N.SORT);
        if (sort == 1) Collections.reverse(out);
        if (sort == 2) out.sort((x, y) -> Long.compare(y.installed, x.installed));
        return out;
    }

    static synchronized void forget() {
        cache = null;
        icons.clear();
        IconStyles.forgetColors();
    }

    static Bitmap bigIcon(Context c, App a) {
        int style = Prefs.get(c, Prefs.N.ICON_STYLE);
        // the "text color" look also depends on that color
        String key = a.component.flattenToString() + "#" + style
                + (style == 4 ? "#" + Prefs.get(c, Prefs.N.TEXT) : "");
        return icons.computeIfAbsent(key, k -> IconStyles.render(c, a, style));
    }

    static void forgetIcons() { icons.clear(); }

    /** A copy at exactly `px`, so a wrap_content ImageView shows it at that size. */
    static Bitmap icon(Context c, App a, int px) {
        Bitmap b = Bitmap.createScaledBitmap(bigIcon(c, a), px, px, true);
        b.setDensity(c.getResources().getDisplayMetrics().densityDpi);
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
