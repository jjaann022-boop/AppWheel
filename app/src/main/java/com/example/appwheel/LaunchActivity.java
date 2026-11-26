package com.example.appwheel;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;

/** Invisible hop: the widget list needs an explicit target for its click template. */
public class LaunchActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String s = getIntent().getStringExtra("component");
        ComponentName cn = s == null ? null : ComponentName.unflattenFromString(s);
        if (cn != null) Apps.launch(this, cn);
        finish();
    }
}
