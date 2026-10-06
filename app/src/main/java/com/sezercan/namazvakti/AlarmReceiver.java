package com.sezercan.namazvakti;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Vakit geldiğinde çalışır: bildirimi gösterir ve sonraki alarmı kurar. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        int prayer = intent.getIntExtra(AlarmScheduler.EXTRA_PRAYER, -1);
        long time = intent.getLongExtra(AlarmScheduler.EXTRA_TIME, 0);
        if (intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)) {
            if (prayer >= 0) Notifier.show(c, prayer);
            return;
        }
        // Telefon uzun süre kapalı kaldıysa geçmiş vakit için ses çıkarma.
        boolean fresh = System.currentTimeMillis() - time < 30 * 60 * 1000L;
        if (prayer >= 0 && fresh && Prefs.alarmOn(c, prayer)) {
            Notifier.show(c, prayer);
        }
        AlarmScheduler.scheduleNext(c);
    }
}
