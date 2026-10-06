package com.sezercan.namazvakti;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Vakit geldiğinde çalışır: alarmı çaldırır, sonraki alarmı kurar, gerekirse vakitleri günceller. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        int prayer = intent.getIntExtra(AlarmScheduler.EXTRA_PRAYER, -1);
        long time = intent.getLongExtra(AlarmScheduler.EXTRA_TIME, 0);
        if (intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)) {
            if (prayer >= 0) Notifier.alarm(c, prayer);
            return;
        }
        // Telefon uzun süre kapalı kaldıysa geçmiş vakit için ses çıkarma.
        boolean fresh = System.currentTimeMillis() - time < 30 * 60 * 1000L;
        if (prayer >= 0 && fresh && Prefs.alarmOn(c, prayer)) {
            Notifier.alarm(c, prayer);
        }
        AlarmScheduler.scheduleNext(c);

        if (Diyanet.needsRefresh(c)) {
            final PendingResult pr = goAsync();
            final Context app = c.getApplicationContext();
            new Thread(() -> {
                try {
                    if (Diyanet.refresh(app)) AlarmScheduler.scheduleNext(app);
                } finally {
                    pr.finish();
                }
            }).start();
        }
    }
}
