package com.sezercan.namazvakti;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Hangi gün hangi namazın kılındığını telefonda saklar (gün başına 5 bitlik işaret). */
public final class Habits {
    private Habits() {}

    /** Takip edilen vakitler (Güneş hariç). */
    public static final int[] PRAYERS = {
            PrayerTimes.IMSAK, PrayerTimes.OGLE, PrayerTimes.IKINDI, PrayerTimes.AKSAM, PrayerTimes.YATSI
    };
    public static final int DAYS = 7;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("kildim", Context.MODE_PRIVATE);
    }

    public static String dayKey(Calendar day) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(day.getTimeZone());
        return f.format(day.getTime());
    }

    public static int mask(Context c, String dayKey) {
        return sp(c).getInt(dayKey, 0);
    }

    public static boolean prayed(Context c, String dayKey, int prayer) {
        return (mask(c, dayKey) & (1 << prayer)) != 0;
    }

    public static void set(Context c, String dayKey, int prayer, boolean done) {
        int m = mask(c, dayKey);
        m = done ? (m | (1 << prayer)) : (m & ~(1 << prayer));
        sp(c).edit().putInt(dayKey, m).apply();
        cleanup(c);
    }

    /** Bugün dahil son 7 günün anahtarları, en eskiden en yeniye. */
    public static String[] lastDays() {
        String[] out = new String[DAYS];
        Calendar d = Times.today();
        d.add(Calendar.DAY_OF_MONTH, -(DAYS - 1));
        for (int i = 0; i < DAYS; i++) {
            out[i] = dayKey(d);
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        return out;
    }

    public static int count(int mask) {
        int n = 0;
        for (int p : PRAYERS) if ((mask & (1 << p)) != 0) n++;
        return n;
    }

    /** 30 günden eski kayıtları siler. */
    private static void cleanup(Context c) {
        Calendar limit = Times.today();
        limit.add(Calendar.DAY_OF_MONTH, -30);
        String min = dayKey(limit);
        SharedPreferences.Editor e = null;
        for (String k : sp(c).getAll().keySet()) {
            if (k.compareTo(min) < 0) {
                if (e == null) e = sp(c).edit();
                e.remove(k);
            }
        }
        if (e != null) e.apply();
    }
}
