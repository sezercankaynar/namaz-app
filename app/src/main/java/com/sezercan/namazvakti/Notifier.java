package com.sezercan.namazvakti;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.Calendar;

public final class Notifier {
    private Notifier() {}

    /** Ana kanal: sesi uygulama kendisi çalar (seçilen zil/ilahi), kanal sadece titreşir. */
    static final String CHANNEL = "vakit_v2";
    /** Yedek kanal: ses servisi başlatılamazsa telefonun alarm sesiyle bildirim. */
    static final String CHANNEL_FALLBACK = "vakit_yedek";

    static int notificationId(int prayer) {
        return 100 + prayer;
    }

    public static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.deleteNotificationChannel("vakit_alarmi"); // ilk sürümün kanalı
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Namaz vakti alarmı",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Namaz vakti girdiğinde gösterilen uyarı (sesi uygulama ayarlarından seçilir)");
            ch.setSound(null, null);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 700, 400, 700, 400, 700});
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setBypassDnd(true);
            nm.createNotificationChannel(ch);
        }
        if (nm.getNotificationChannel(CHANNEL_FALLBACK) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_FALLBACK, "Namaz vakti (yedek)",
                    NotificationManager.IMPORTANCE_HIGH);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ch.setSound(sound, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            ch.enableVibration(true);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
    }

    /** Vakit alarmını başlatır: sesi çalan servisi açar; olmazsa sesli yedek bildirim gösterir. */
    public static void alarm(Context c, int prayer) {
        ensureChannel(c);
        Intent svc = new Intent(c, AlarmSoundService.class);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER, prayer);
        try {
            c.startForegroundService(svc);
        } catch (Exception e) {
            c.getSystemService(NotificationManager.class)
                    .notify(notificationId(prayer), build(c, prayer, false, CHANNEL_FALLBACK));
        }
    }

    static Notification build(Context c, int prayer, boolean playing) {
        return build(c, prayer, playing, CHANNEL);
    }

    static Notification build(Context c, int prayer, boolean playing, String channel) {
        boolean friday = Times.today().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        PrayerInfo info = PrayerInfo.forPrayer(prayer, friday);

        Intent open = new Intent(c, PrayerDetailActivity.class);
        open.putExtra(PrayerDetailActivity.EXTRA_KEY, info.key);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 100 + prayer, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(c, AlarmSoundService.class).setAction(AlarmSoundService.ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(c, 200, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dismiss = new Intent(c, AlarmSoundService.class).setAction(AlarmSoundService.ACTION_DISMISS);
        PendingIntent dismissPi = PendingIntent.getService(c, 201, dismiss,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = info.summary + "\nNasıl kılındığını görmek için dokunun.";
        Notification.Builder b = new Notification.Builder(c, channel)
                .setSmallIcon(R.drawable.ic_stat_moon)
                .setContentTitle(info.alarmTitle)
                .setContentText(info.summary)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setColor(Ui.PLUM)
                .setContentIntent(pi)
                .setDeleteIntent(dismissPi)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (playing) {
            b.addAction(new Notification.Action.Builder(null, "🔇 Sesi durdur", stopPi).build());
        }
        return b.build();
    }
}
