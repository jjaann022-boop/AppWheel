package com.example.appwheel;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.util.function.IntConsumer;

/** Full palette: saturation/brightness square, hue bar and a hex field. */
final class ColorPicker {
    static void show(Context c, String title, int rgb, IntConsumer done) {
        float dp = c.getResources().getDisplayMetrics().density;
        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | rgb, hsv);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        int pad = Math.round(20 * dp);
        box.setPadding(pad, pad, pad, 0);

        View sample = new View(c);
        GradientDrawable chip = new GradientDrawable();
        chip.setCornerRadius(10 * dp);
        sample.setBackground(chip);
        EditText hex = new EditText(c);
        hex.setSingleLine(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hex.setFilters(new InputFilter[] {new InputFilter.LengthFilter(7)});
        boolean[] syncing = {false};

        Runnable sync = () -> {
            int col = Color.HSVToColor(hsv);
            chip.setColor(col);
            syncing[0] = true;
            hex.setText(String.format("#%06X", col & 0xFFFFFF));
            syncing[0] = false;
        };

        Pad square = new Pad(c, hsv, false);
        Pad hue = new Pad(c, hsv, true);
        square.changed = sync;
        hue.changed = () -> { square.invalidate(); sync.run(); };

        box.addView(square, new LinearLayout.LayoutParams(-1, Math.round(200 * dp)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, Math.round(32 * dp));
        hp.topMargin = Math.round(16 * dp);
        box.addView(hue, hp);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, Math.round(36 * dp));
        sp.topMargin = Math.round(16 * dp);
        box.addView(sample, sp);
        box.addView(hex);
        sync.run();

        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) {
                if (syncing[0] || !e.toString().matches("#[0-9a-fA-F]{6}")) return;
                int col = Color.parseColor(e.toString());
                Color.colorToHSV(col, hsv);
                chip.setColor(col);
                square.invalidate();
                hue.invalidate();
            }
        });

        new AlertDialog.Builder(c, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        (d, w) -> done.accept(Color.HSVToColor(hsv) & 0xFFFFFF))
                .show();
    }

    private static final class Pad extends View {
        private final float[] hsv;
        private final boolean hueBar;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        Runnable changed;

        Pad(Context c, float[] hsv, boolean hueBar) {
            super(c);
            this.hsv = hsv;
            this.hueBar = hueBar;
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(3 * c.getResources().getDisplayMetrics().density);
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            if (hueBar) {
                int[] rainbow = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
                        0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
                paint.setShader(new LinearGradient(0, 0, w, 0, rainbow, null, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
            } else {
                int pure = Color.HSVToColor(new float[] {hsv[0], 1, 1});
                paint.setShader(new LinearGradient(0, 0, w, 0, Color.WHITE, pure, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new LinearGradient(0, 0, 0, h, 0x00000000, Color.BLACK, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
            }
            float x = hueBar ? hsv[0] / 360f * w : hsv[1] * w;
            float y = hueBar ? h / 2 : (1 - hsv[2]) * h;
            float r = ring.getStrokeWidth() * 3;
            ring.setColor(Color.BLACK);
            canvas.drawCircle(x, y, r + ring.getStrokeWidth() / 2, ring);
            ring.setColor(Color.WHITE);
            canvas.drawCircle(x, y, r, ring);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float x = Math.max(0, Math.min(1, e.getX() / getWidth()));
            float y = Math.max(0, Math.min(1, e.getY() / getHeight()));
            if (hueBar) {
                hsv[0] = x * 359.9f;
            } else {
                hsv[1] = x;
                hsv[2] = 1 - y;
            }
            invalidate();
            changed.run();
            return true;
        }
    }
}
