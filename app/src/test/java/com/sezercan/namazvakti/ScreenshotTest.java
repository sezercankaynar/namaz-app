package com.sezercan.namazvakti;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.io.FileOutputStream;

/** Ekran görüntülerini build/ekranlar klasörüne kaydeder (tasarım kontrolü için). */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w360dp-h800dp-xxhdpi")
public class ScreenshotTest {

    private static final File OUT = new File("build/ekranlar");

    static void save(View v, String name) throws Exception {
        OUT.mkdirs();
        int w = v.getWidth() > 0 ? v.getWidth() : 1080;
        // Kaydırılabilir içeriğin tamamını çiz.
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int h = Math.max(v.getMeasuredHeight(), 2400);
        v.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    static View content(Activity a) {
        android.view.ViewGroup root = a.findViewById(android.R.id.content);
        View outer = root.getChildAt(0);
        // Sayfa: [üst çubuk] + ScrollView → tam yüksekliği çizebilmek için ScrollView'u açıyoruz.
        if (outer instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout l = (android.widget.LinearLayout) outer;
            for (int i = 0; i < l.getChildCount(); i++) {
                if (l.getChildAt(i) instanceof android.widget.ScrollView) {
                    l.getChildAt(i).setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
                }
            }
        }
        return outer;
    }

    /** Haftalık özet için örnek veri: kendi işaretlerimiz ve bir arkadaştan gelmiş özet. */
    static void seedWeek() throws Exception {
        android.content.Context c = org.robolectric.RuntimeEnvironment.getApplication();
        String[] days = Habits.lastDays();
        int[][] mine = {{0, 2, 3, 4, 5}, {0, 2, 4, 5}, {2, 3, 4, 5}, {0, 2, 3, 4, 5}, {0, 4, 5}, {0, 2, 3, 4, 5}, {0, 2}};
        for (int i = 0; i < days.length; i++) for (int p : mine[i]) Habits.set(c, days[i], p, true);
        Sync.setMyName(c, "Sezer");
        Sync.pair(c, "K7PQ2X9M");
        org.json.JSONObject g = new org.json.JSONObject();
        int[] masks = {61, 61, 53, 61, 45, 61, 5};
        for (int i = 0; i < days.length; i++) g.put(days[i], masks[i]);
        org.json.JSONObject snap = new org.json.JSONObject()
                .put("v", 1).put("id", "arkadas").put("ad", "Ahmet").put("t", System.currentTimeMillis() - 3600_000).put("g", g);
        Sync.mergeLines(c, java.util.Collections.singletonList(
                new org.json.JSONObject().put("event", "message").put("message", snap.toString()).toString()));
    }

    @Test
    public void screens() throws Exception {
        seedWeek();
        save(content(Robolectric.buildActivity(MainActivity.class).setup().get()), "1_ana_ekran");
        save(content(Robolectric.buildActivity(PrayerDetailActivity.class,
                new Intent().putExtra(PrayerDetailActivity.EXTRA_KEY, "ogle")).setup().get()), "2_ogle_detay");
        save(content(Robolectric.buildActivity(PrayerDetailActivity.class,
                new Intent().putExtra(PrayerDetailActivity.EXTRA_KEY, "rehber")).setup().get()), "3_nasil_kilinir");
        save(content(Robolectric.buildActivity(TextsActivity.class).setup().get()), "4_dualar_kategoriler");
        save(content(Robolectric.buildActivity(TextsActivity.class,
                new Intent().putExtra(TextsActivity.EXTRA_CAT, 2)).setup().get()), "5_kisa_sureler");
        save(content(Robolectric.buildActivity(TextsActivity.class,
                new Intent().putExtra(TextsActivity.EXTRA_CAT, 2).putExtra(TextsActivity.EXTRA_ITEM, 9)).setup().get()), "6_ihlas");
        save(content(Robolectric.buildActivity(TextsActivity.class,
                new Intent().putExtra(TextsActivity.EXTRA_CAT, 0).putExtra(TextsActivity.EXTRA_ITEM, 5)).setup().get()), "6b_ettehiyyatu");
        save(content(Robolectric.buildActivity(PrayerDetailActivity.class,
                new Intent().putExtra(PrayerDetailActivity.EXTRA_KEY, "yatsi")).setup().get()), "2b_yatsi_detay");

        Activity alarm = Robolectric.buildActivity(AlarmActivity.class,
                new Intent().putExtra(AlarmScheduler.EXTRA_PRAYER, 2)).setup().get();
        View av = alarm.getWindow().getDecorView();
        av.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
        av.layout(0, 0, 1080, 2400);
        Bitmap b = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
        av.draw(new Canvas(b));
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, "8_kilit_ekrani.png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }

        MainActivity main = Robolectric.buildActivity(MainActivity.class).setup().get();
        SmokeTest.clickAll(main.getWindow().getDecorView(), "👥 Kod:");
        sheetShot(ShadowDialog.getLatestDialog(), "9_eslesme_kodu");
        Sync.unpair(org.robolectric.RuntimeEnvironment.getApplication());
        main = Robolectric.buildActivity(MainActivity.class).setup().get();
        SmokeTest.clickAll(main.getWindow().getDecorView(), "Arkadaşınla eşleş");
        sheetShot(ShadowDialog.getLatestDialog(), "9b_eslesme");
        SmokeTest.clickAll(main.getWindow().getDecorView(), "Alarm sesi");
        Dialog d = ShadowDialog.getLatestDialog();
        View dv = d.getWindow().getDecorView();
        dv.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
        dv.layout(0, 0, 1080, 2400);
        Bitmap s = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
        s.eraseColor(0xFFFFF9F5);
        dv.draw(new Canvas(s));
        try (FileOutputStream out = new FileOutputStream(new File(OUT, "7_alarm_sesi.png"))) {
            s.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    static void sheetShot(Dialog d, String name) throws Exception {
        View dv = d.getWindow().getDecorView();
        dv.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
        dv.layout(0, 0, 1080, 2400);
        Bitmap s = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
        s.eraseColor(0xFFFFF9F5);
        dv.draw(new Canvas(s));
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            s.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        d.dismiss();
    }
}
