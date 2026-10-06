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

    static final String CHANNEL = "vakit_alarmi";

    public static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Namaz vakti alarmı",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Namaz vakti girdiğinde çalan uyarı");
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ch.setSound(sound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setBypassDnd(true);
        nm.createNotificationChannel(ch);
    }

    public static void show(Context c, int prayer) {
        ensureChannel(c);
        boolean friday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        PrayerInfo info = PrayerInfo.forPrayer(prayer, friday);

        Intent open = new Intent(c, PrayerDetailActivity.class);
        open.putExtra(PrayerDetailActivity.EXTRA_KEY, info.key);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 100 + prayer, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = info.alarmTitle;
        String text = info.summary + "\nNasıl kılındığını görmek için dokunun.";

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_moon)
                .setContentTitle(title)
                .setContentText(info.summary)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setColor(0xFF1B5E20)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        c.getSystemService(NotificationManager.class).notify(prayer, n);
    }
}
