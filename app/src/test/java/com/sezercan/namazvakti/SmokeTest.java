package com.sezercan.namazvakti;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Ekranların açılırken çökmediğini kontrol eder. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 29, 30, 33, 34, 35})
public class SmokeTest {

    @org.junit.Before
    public void offline() {
        Sync.server = "http://127.0.0.1:9/";
    }


    @Test
    public void mainScreenOpens() {
        Robolectric.buildActivity(MainActivity.class).setup();
    }

    @Test
    public void prayerScreensOpen() {
        for (String key : new String[]{"sabah", "gunes", "ogle", "cuma", "ikindi", "aksam", "yatsi", "rehber"}) {
            Intent i = new Intent().putExtra(PrayerDetailActivity.EXTRA_KEY, key);
            Robolectric.buildActivity(PrayerDetailActivity.class, i).setup();
        }
    }

    @Test
    public void textScreensOpen() {
        Robolectric.buildActivity(TextsActivity.class).setup();
        Texts.Category[] cats = Texts.all(org.robolectric.RuntimeEnvironment.getApplication());
        for (int c = 0; c < cats.length; c++) {
            Robolectric.buildActivity(TextsActivity.class,
                    new Intent().putExtra(TextsActivity.EXTRA_CAT, c)).setup();
            for (int i = 0; i < cats[c].items.length; i++) {
                Robolectric.buildActivity(TextsActivity.class, new Intent()
                        .putExtra(TextsActivity.EXTRA_CAT, c).putExtra(TextsActivity.EXTRA_ITEM, i)).setup();
            }
        }
    }

    @Test
    public void alarmScreenAndSheetsOpen() {
        Robolectric.buildActivity(AlarmActivity.class,
                new Intent().putExtra(AlarmScheduler.EXTRA_PRAYER, 2)).setup();
        MainActivity main = Robolectric.buildActivity(MainActivity.class).setup().get();
        clickAll(main.getWindow().getDecorView(), "Günün mısrası");
        SettingsActivity set = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        clickAll(set.getWindow().getDecorView(), "Alarm sesi:");
        clickAll(set.getWindow().getDecorView(), "Alarmı dene");
        TrackActivity track = Robolectric.buildActivity(TrackActivity.class).setup().get();
        clickAll(track.getWindow().getDecorView(), "Arkadaşınla eşleş");
    }

    /** Metni içeren tıklanabilir görünüme dokunur. */
    static void clickAll(android.view.View v, String text) {
        if (v instanceof android.widget.TextView
                && ((android.widget.TextView) v).getText().toString().contains(text)) {
            android.view.View t = v;
            while (t != null && !t.isClickable()) t = (android.view.View) t.getParent();
            if (t != null) t.performClick();
            return;
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) clickAll(g.getChildAt(i), text);
        }
    }

    @Test
    public void alarmFlowWorks() {
        android.content.Context c = org.robolectric.RuntimeEnvironment.getApplication();
        AlarmScheduler.scheduleNext(c);
        AlarmScheduler.scheduleTest(c);
        new AlarmReceiver().onReceive(c, new Intent()
                .putExtra(AlarmScheduler.EXTRA_PRAYER, 2)
                .putExtra(AlarmScheduler.EXTRA_TIME, System.currentTimeMillis()));
        Robolectric.buildService(AlarmSoundService.class, new Intent()
                .putExtra(AlarmScheduler.EXTRA_PRAYER, 2)).create().startCommand(0, 1);
    }
}
