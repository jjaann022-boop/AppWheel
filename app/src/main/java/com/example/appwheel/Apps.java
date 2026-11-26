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
        Bitmap icon;
    }

    /** All launchable apps, sorted by name. Call off the main thread. */
    static List<App> load(Context c, int iconPx) {
        PackageManager pm = c.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<App> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(query, 0)) {
            if (r.activityInfo.packageName.equals(c.getPackageName())) continue;
            App a = new App();
            a.label = r.loadLabel(pm).toString();
            a.component = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
            Drawable d = r.loadIcon(pm);
            a.icon = Bitmap.createBitmap(iconPx, iconPx, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, iconPx, iconPx);
            d.draw(new Canvas(a.icon));
            out.add(a);
        }
        Collator col = Collator.getInstance();
        out.sort((x, y) -> col.compare(x.label, y.label));
        return out;
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
