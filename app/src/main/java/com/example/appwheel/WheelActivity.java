package com.example.appwheel;

import android.app.Activity;
import android.os.Bundle;
import java.util.List;

public class WheelActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        WheelView wheel = new WheelView(this);
        wheel.setListener(new WheelView.Listener() {
            @Override public void onPick(Apps.App a) {
                Apps.launch(WheelActivity.this, a.component);
                finish();
            }
            @Override public void onDismiss() { finish(); }
        });
        setContentView(wheel);
        int px = Math.round(64 * getResources().getDisplayMetrics().density);
        new Thread(() -> {
            List<Apps.App> apps = Apps.load(getApplicationContext(), px);
            runOnUiThread(() -> wheel.setApps(apps));
        }).start();
    }

    @Override protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) finish();
    }
}
