package com.sezercan.namazvakti;

import static org.junit.Assert.*;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Ayarlar'daki alarm düğmeleri parmakla dokununca açılıp kapanabilmeli. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w360dp-h800dp-xxhdpi")
public class SwitchTouchTest {

    static void collect(View v, List<Switch> out) {
        if (v instanceof Switch) out.add((Switch) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out);
        }
    }

    /** Gerçek bir dokunuşu (bas-bırak) taklit eder. */
    static void tap(View v) {
        long t = SystemClock.uptimeMillis();
        float x = v.getWidth() / 2f, y = v.getHeight() / 2f;
        MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, x, y, 0);
        v.dispatchTouchEvent(down);
        v.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
    }

    @Test
    public void alarmSwitchesTurnOffAndOnByTouch() {
        SettingsActivity a = Robolectric.buildActivity(SettingsActivity.class).setup().visible().get();
        List<Switch> switches = new ArrayList<>();
        collect(a.getWindow().getDecorView(), switches);
        assertEquals(6, switches.size());

        int ogle = PrayerTimes.OGLE;
        Switch sw = switches.get(ogle);
        assertTrue(sw.isChecked());
        assertTrue(Prefs.alarmOn(a, ogle));

        tap(sw);
        assertFalse("dokununca kapanmalı", sw.isChecked());
        assertFalse(Prefs.alarmOn(a, ogle));

        tap(sw);
        assertTrue("tekrar dokununca açılmalı", sw.isChecked());
        assertTrue(Prefs.alarmOn(a, ogle));

        // Güneş satırı devre dışı kalır.
        Switch gunes = switches.get(PrayerTimes.GUNES);
        tap(gunes);
        assertFalse(gunes.isChecked());
    }
}
