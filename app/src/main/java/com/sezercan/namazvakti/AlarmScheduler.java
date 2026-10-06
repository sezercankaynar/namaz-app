package com.sezercan.namazvakti;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** Bir sonraki açık vakit için tek bir alarm kurar. Alarm çalınca bir sonrakini kurar. */
public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static final String EXTRA_PRAYER = "vakit";
    public static final String EXTRA_TIME = "zaman";
    public static final String EXTRA_TEST = "deneme";

    /** {vakit, zaman} veya hiçbir alarm açık değilse null. */
    public static long[] findNext(Context c, long after) {
        Calendar day = Times.today();
        for (int offset = 0; offset < 3; offset++) {
            long[] times = Times.forDay(c, day);
            for (int p = 0; p < PrayerTimes.COUNT; p++) {
                if (times[p] > after && Prefs.alarmOn(c, p)) return new long[]{p, times[p]};
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return null;
    }

    public static void scheduleNext(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, AlarmReceiver.class);
        long[] next = findNext(c, System.currentTimeMillis() + 1000);
        if (next == null) {
            PendingIntent old = PendingIntent.getBroadcast(c, 0, i,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (old != null) am.cancel(old);
            return;
        }
        i.putExtra(EXTRA_PRAYER, (int) next[0]);
        i.putExtra(EXTRA_TIME, next[1]);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next[1], pi);
            return;
        }
        PendingIntent show = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(next[1], show), pi);
    }

    /** Şu anki vakit (en son giren namaz vakti); deneme alarmında kullanılır. */
    public static int currentPrayer(Context c, long at) {
        long[] times = Times.forDay(c, Times.today());
        int prayer = PrayerTimes.YATSI;
        for (int p = 0; p < PrayerTimes.COUNT; p++) {
            if (p != PrayerTimes.GUNES && times[p] <= at) prayer = p;
        }
        return prayer;
    }

    /** "1 dakika sonra" denemesi: gerçek alarm yoluyla çalar. */
    public static void scheduleTest(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long at = System.currentTimeMillis() + 60_000;
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra(EXTRA_PRAYER, currentPrayer(c, at));
        i.putExtra(EXTRA_TIME, at);
        i.putExtra(EXTRA_TEST, true);
        PendingIntent pi = PendingIntent.getBroadcast(c, 2, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
