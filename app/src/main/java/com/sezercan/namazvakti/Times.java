package com.sezercan.namazvakti;

import android.content.Context;

import java.util.Calendar;
import java.util.TimeZone;

/** Vakitlerin tek kaynağı: önce Diyanet'in resmi vakitleri, yoksa telefonda hesaplanan vakitler. */
public final class Times {
    private Times() {}

    public static final TimeZone TR = TimeZone.getTimeZone("Europe/Istanbul");

    public static Calendar today() {
        return Calendar.getInstance(TR);
    }

    public static long[] forDay(Context c, Calendar day) {
        long[] official = Diyanet.cached(c, Prefs.ilce(c), day);
        if (official != null) return official;
        double[] ll = Cities.COORDS[Prefs.city(c)];
        return PrayerTimes.forDay(day, ll[0], ll[1]);
    }

    public static boolean isOfficial(Context c, Calendar day) {
        return Diyanet.cached(c, Prefs.ilce(c), day) != null;
    }
}
