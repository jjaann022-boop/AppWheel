package com.example.appwheel;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every icon look. A style id is family * 100 + variant, so adding shapes or effects
 * later never changes what an already saved id means.
 */
final class IconStyles {
    static final String[] FAMILIES = {"Basic", "Shaped", "Color", "Letter", "Outline"};
    private static final int BASIC = 0, SHAPED = 1, COLOR = 2, LETTER = 3, OUTLINE = 4;

    static final String[] BASICS = {"System", "Original", "Grayscale", "White", "Text color",
            "Inverted", "Pixel", "Soft fade", "Vivid", "Sepia"};

    static final String[] SHAPES = {"Circle", "Square", "Rounded", "Squircle", "Drop", "Leaf",
            "Diamond", "Triangle", "Pentagon", "Hexagon", "Octagon", "Star", "Sparkle",
            "Flower", "Clover", "Heart", "Shield", "Scallop", "Pill", "Arch"};

    static int count(int family) { return family == BASIC ? BASICS.length : SHAPES.length; }
    static String name(int family, int i) { return family == BASIC ? BASICS[i] : SHAPES[i]; }

    private static final int S = Apps.ICON_PX;
    private static final Map<String, Integer> colors = new ConcurrentHashMap<>();

    static void forgetColors() { colors.clear(); }

    static Bitmap render(Context c, Apps.App a, int style) {
        int family = style / 100, variant = style % 100;
        if (family < 0 || family >= FAMILIES.length || variant >= count(family)) {
            family = BASIC;
            variant = 0;
        }
        Bitmap out = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (family == BASIC) {
            basic(c, a, variant, canvas, paint);
            return out;
        }
        Path shape = shape(variant);
        if (family == SHAPED) {
            // the original art, enlarged a little, cut to the shape over its own color
            Bitmap art = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
            Canvas ac = new Canvas(art);
            ac.drawColor(color(c, a));
            draw(original(c, a), ac, -S / 12, S + S / 12);
            paint.setShader(new BitmapShader(art, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, paint);
            return out;
        }
        int color = color(c, a);
        paint.setColor(color);
        if (family == OUTLINE) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(S * 0.07f);
            paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.save();
            canvas.scale(0.92f, 0.92f, S / 2f, S / 2f);
            canvas.drawPath(shape, paint);
            canvas.restore();
            paint.setStyle(Paint.Style.FILL);
            letter(canvas, paint, a.label, color);
        } else {
            canvas.drawPath(shape, paint);
            if (family == LETTER) {
                boolean light = Color.luminance(color) > 0.55f;
                letter(canvas, paint, a.label, light ? 0xFF16181D : Color.WHITE);
            }
        }
        return out;
    }

    private static void letter(Canvas canvas, Paint paint, String label, int color) {
        if (label.isEmpty()) return;
        String first = label.substring(0, Character.charCount(label.codePointAt(0))).toUpperCase();
        paint.setShader(null);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(S * 0.4f);
        canvas.drawText(first, S / 2f, S / 2f - (paint.ascent() + paint.descent()) / 2, paint);
    }

    private static void draw(Drawable d, Canvas canvas, int from, int to) {
        d.setBounds(from, from, to, to);
        d.draw(canvas);
    }

    /** The icon as the phone shows it, including any system theme. */
    private static Drawable system(Context c, Apps.App a) {
        return a.info.loadIcon(c.getPackageManager());
    }

    /** The app's own artwork, read from its package so icon themes do not replace it. */
    private static Drawable original(Context c, Apps.App a) {
        ActivityInfo info = a.info.activityInfo;
        try {
            Resources res = c.getPackageManager().getResourcesForApplication(info.applicationInfo);
            int id = info.getIconResource();
            if (id != 0) {
                Drawable d = res.getDrawableForDensity(id, 640, null);
                if (d != null) return d.mutate();
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            return system(c, a);
        }
        return system(c, a);
    }

    private static void basic(Context c, Apps.App a, int variant, Canvas canvas, Paint paint) {
        if (variant == 0) { draw(system(c, a), canvas, 0, S); return; }
        if (variant == 1) { draw(original(c, a), canvas, 0, S); return; }
        Bitmap src = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        draw(original(c, a), new Canvas(src), 0, S);
        ColorMatrix m = new ColorMatrix();
        if (variant == 2) {                         // grayscale
            m.setSaturation(0);
        } else if (variant == 3 || variant == 4) {  // flat tint that keeps the details
            int tint = variant == 3 ? Color.WHITE : Prefs.argb(c, Prefs.N.TEXT);
            // brightness becomes opacity, so inner artwork stays readable
            m.set(new float[] {
                    0, 0, 0, 0, Color.red(tint),
                    0, 0, 0, 0, Color.green(tint),
                    0, 0, 0, 0, Color.blue(tint),
                    0.15f, 0.5f, 0.05f, 0.35f, -40});
        } else if (variant == 5) {                  // inverted
            m.set(new float[] {-1, 0, 0, 0, 255, 0, -1, 0, 0, 255, 0, 0, -1, 0, 255, 0, 0, 0, 1, 0});
        } else if (variant == 6) {                  // pixel art
            Bitmap tiny = Bitmap.createScaledBitmap(src, 14, 14, true);
            paint.setFilterBitmap(false);
            canvas.drawBitmap(tiny, null, new Rect(0, 0, S, S), paint);
            return;
        } else if (variant == 7) {                  // soft fade
            paint.setAlpha(150);
        } else if (variant == 8) {                  // vivid
            m.setSaturation(1.8f);
        } else {                                    // sepia
            m.setSaturation(0);
            ColorMatrix warm = new ColorMatrix();
            warm.setScale(1.1f, 0.92f, 0.7f, 1);
            m.postConcat(warm);
        }
        paint.setColorFilter(new ColorMatrixColorFilter(m));
        canvas.drawBitmap(src, 0, 0, paint);
    }

    /** The app's main color: the most common strong hue in its original icon. */
    private static int color(Context c, Apps.App a) {
        return colors.computeIfAbsent(a.component.flattenToString(), key -> {
            Bitmap b = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            draw(original(c, a), new Canvas(b), 0, 48);
            float[] weight = new float[24];
            float[][] sum = new float[24][3];
            long r = 0, g = 0, bl = 0, n = 0;
            float[] hsv = new float[3];
            for (int y = 0; y < 48; y++) {
                for (int x = 0; x < 48; x++) {
                    int p = b.getPixel(x, y);
                    if (Color.alpha(p) < 200) continue;
                    r += Color.red(p);
                    g += Color.green(p);
                    bl += Color.blue(p);
                    n++;
                    Color.colorToHSV(p, hsv);
                    if (hsv[1] < 0.3f || hsv[2] < 0.25f) continue;
                    int bucket = Math.min(23, (int) (hsv[0] / 15));
                    float w = hsv[1] * hsv[2];
                    weight[bucket] += w;
                    sum[bucket][0] += w * Color.red(p);
                    sum[bucket][1] += w * Color.green(p);
                    sum[bucket][2] += w * Color.blue(p);
                }
            }
            int best = 0;
            for (int i = 1; i < 24; i++) if (weight[i] > weight[best]) best = i;
            if (weight[best] > 12) {
                return Color.rgb((int) (sum[best][0] / weight[best]),
                        (int) (sum[best][1] / weight[best]), (int) (sum[best][2] / weight[best]));
            }
            // black and white icons keep their average gray
            return n == 0 ? Color.GRAY : Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n));
        });
    }

    // ---- shapes, drawn on a 100 x 100 grid ----

    private static Path shape(int i) {
        Path p = new Path();
        switch (i) {
            case 0: p.addCircle(50, 50, 47, Path.Direction.CW); break;
            case 1: p.addRect(6, 6, 94, 94, Path.Direction.CW); break;
            case 2: p.addRoundRect(4, 4, 96, 96, 24, 24, Path.Direction.CW); break;
            case 3:
                p.moveTo(50, 3);
                p.cubicTo(88, 3, 97, 12, 97, 50);
                p.cubicTo(97, 88, 88, 97, 50, 97);
                p.cubicTo(12, 97, 3, 88, 3, 50);
                p.cubicTo(3, 12, 12, 3, 50, 3);
                break;
            case 4:   // drop: round with one sharp corner
                p.addRoundRect(4, 4, 96, 96,
                        new float[] {46, 46, 8, 8, 46, 46, 46, 46}, Path.Direction.CW);
                break;
            case 5:   // leaf
                p.addRoundRect(4, 4, 96, 96,
                        new float[] {46, 46, 8, 8, 46, 46, 8, 8}, Path.Direction.CW);
                break;
            case 6: polygon(p, 4, 50, -90, 50); break;
            case 7: polygon(p, 3, 56, -90, 61); break;
            case 8: polygon(p, 5, 50, -90, 53); break;
            case 9: polygon(p, 6, 49, -90, 50); break;
            case 10: polygon(p, 8, 50, -67.5f, 50); break;
            case 11: star(p, 5, 52, 25, 53); break;
            case 12: star(p, 4, 50, 19, 50); break;
            case 13: wavy(p, 6, 41, 8); break;
            case 14: wavy(p, 4, 39, 10); break;
            case 15:
                p.moveTo(50, 92);
                p.cubicTo(10, 64, 2, 46, 4, 31);
                p.cubicTo(8, 6, 40, 4, 50, 27);
                p.cubicTo(60, 4, 92, 6, 96, 31);
                p.cubicTo(98, 46, 90, 64, 50, 92);
                break;
            case 16:
                p.moveTo(50, 3);
                p.lineTo(92, 17);
                p.lineTo(92, 50);
                p.cubicTo(92, 74, 72, 90, 50, 97);
                p.cubicTo(28, 90, 8, 74, 8, 50);
                p.lineTo(8, 17);
                break;
            case 17: wavy(p, 12, 44, 3.5f); break;
            case 18: p.addRoundRect(17, 3, 83, 97, 33, 33, Path.Direction.CW); break;
            default:  // arch
                p.addRoundRect(9, 3, 91, 97,
                        new float[] {41, 41, 41, 41, 10, 10, 10, 10}, Path.Direction.CW);
        }
        p.close();
        Matrix grow = new Matrix();
        grow.setScale(S / 100f, S / 100f);
        p.transform(grow);
        return p;
    }

    private static void point(Path p, boolean first, double degrees, float radius, float cy) {
        float x = 50 + radius * (float) Math.cos(Math.toRadians(degrees));
        float y = cy + radius * (float) Math.sin(Math.toRadians(degrees));
        if (first) p.moveTo(x, y);
        else p.lineTo(x, y);
    }

    private static void polygon(Path p, int sides, float radius, float start, float cy) {
        for (int k = 0; k < sides; k++) point(p, k == 0, start + 360.0 * k / sides, radius, cy);
    }

    private static void star(Path p, int tips, float outer, float inner, float cy) {
        for (int k = 0; k < tips * 2; k++) {
            point(p, k == 0, -90 + 180.0 * k / tips, k % 2 == 0 ? outer : inner, cy);
        }
    }

    private static void wavy(Path p, int waves, float radius, float depth) {
        for (int deg = 0; deg < 360; deg += 3) {
            float r = radius + depth * (float) Math.cos(Math.toRadians((deg + 90) * waves));
            point(p, deg == 0, deg, r, 50);
        }
    }
}
