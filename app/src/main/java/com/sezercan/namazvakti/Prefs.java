package com.sezercan.namazvakti;

import android.content.Context;
import android.content.SharedPreferences;

/** Kullanıcı ayarları: şehir, ilçe, alarm sesi ve hangi vakitlerde alarm çalacağı. */
public final class Prefs {
    private Prefs() {}

    public static final int SES_HUZUR = 0, SES_TELEFON = 1, SES_OZEL = 2, SES_TITRESIM = 3;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("ayarlar", Context.MODE_PRIVATE);
    }

    public static int city(Context c) {
        int i = sp(c).getInt("sehir", Cities.DEFAULT);
        return (i >= 0 && i < Cities.NAMES.length) ? i : Cities.DEFAULT;
    }

    /** Şehri değiştirir ve ilçeyi o şehrin merkezine ayarlar. */
    public static void setCity(Context c, int i) {
        sp(c).edit().putInt("sehir", i).putString("ilce", Districts.ids(c, i)[0]).apply();
    }

    /** Diyanet ilçe kimliği. */
    public static String ilce(Context c) {
        String id = sp(c).getString("ilce", null);
        return id != null ? id : Districts.ids(c, city(c))[0];
    }

    public static void setIlce(Context c, String id) {
        sp(c).edit().putString("ilce", id).apply();
    }

    public static boolean alarmOn(Context c, int prayer) {
        // Güneş doğuşu bir namaz vakti değil; alarmı her zaman kapalı.
        if (prayer == PrayerTimes.GUNES) return false;
        return sp(c).getBoolean("alarm_" + prayer, true);
    }

    public static void setAlarmOn(Context c, int prayer, boolean on) {
        sp(c).edit().putBoolean("alarm_" + prayer, on).apply();
    }

    public static int sound(Context c) {
        return sp(c).getInt("ses", SES_HUZUR);
    }

    public static void setSound(Context c, int mode) {
        sp(c).edit().putInt("ses", mode).apply();
    }

    public static String customSoundName(Context c) {
        return sp(c).getString("ses_adi", "Seçilen ses");
    }

    public static void setCustomSoundName(Context c, String name) {
        sp(c).edit().putString("ses_adi", name).apply();
    }

    static SharedPreferences raw(Context c) {
        return sp(c);
    }
}
