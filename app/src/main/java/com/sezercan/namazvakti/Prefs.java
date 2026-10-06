package com.sezercan.namazvakti;

import android.content.Context;
import android.content.SharedPreferences;

/** Kullanıcı ayarları: seçili şehir ve hangi vakitlerde alarm çalacağı. */
public final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("ayarlar", Context.MODE_PRIVATE);
    }

    public static int city(Context c) {
        int i = sp(c).getInt("sehir", Cities.DEFAULT);
        return (i >= 0 && i < Cities.NAMES.length) ? i : Cities.DEFAULT;
    }

    public static void setCity(Context c, int i) {
        sp(c).edit().putInt("sehir", i).apply();
    }

    public static boolean alarmOn(Context c, int prayer) {
        // Güneş doğuşu bir namaz vakti değil; varsayılan olarak kapalı.
        return sp(c).getBoolean("alarm_" + prayer, prayer != PrayerTimes.GUNES);
    }

    public static void setAlarmOn(Context c, int prayer, boolean on) {
        sp(c).edit().putBoolean("alarm_" + prayer, on).apply();
    }

    public static long[] todayTimes(Context c, java.util.Calendar day) {
        double[] ll = Cities.COORDS[city(c)];
        return PrayerTimes.forDay(day, ll[0], ll[1]);
    }
}
