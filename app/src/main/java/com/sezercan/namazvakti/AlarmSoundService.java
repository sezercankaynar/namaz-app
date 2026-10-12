package com.sezercan.namazvakti;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.io.File;

/**
 * Vakit alarmının sesini (huzur zili, telefonun alarm sesi ya da seçilen ilahi) çalar.
 * Ses telefonun zil sesi gibi davranır: telefon sessiz/titreşimdeyse çalmaz, zil sesi
 * seviyesini kullanır ve çalarken ses tuşlarına basılınca susar.
 */
public class AlarmSoundService extends Service {

    public static final String ACTION_STOP = "com.sezercan.namazvakti.SESI_DURDUR";
    /** Bildirim kaydırılıp silindi ya da açıldı: sesi durdur, bildirimi geri koyma. */
    public static final String ACTION_DISMISS = "com.sezercan.namazvakti.BILDIRIM_KAPANDI";
    public static final String EXTRA_PREVIEW = "onizleme";
    /** Uzun bir ilahi seçilse bile en fazla bu kadar çalar. */
    private static final long MAX_MS = 5 * 60 * 1000L;

    static volatile boolean running;
    /** Şu anda gerçekten ses çalınıyor mu (testler ve ekranlar için). */
    static volatile boolean soundPlaying;

    /** Alarm sesi zil sesi akışında çalar; böylece sessiz mod ve zil sesi seviyesi geçerli olur. */
    static final AudioAttributes ATTRS = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private MediaSession session;
    private BroadcastReceiver volumeReceiver;
    private int prayer = -1;
    private boolean fullScreen;

    /** Telefon normal (sesli) modda ve zil sesi açık mı? */
    static boolean ringerAllowsSound(Context c) {
        AudioManager am = c.getSystemService(AudioManager.class);
        if (am == null) return true;
        return am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL
                && am.getStreamVolume(AudioManager.STREAM_RING) > 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            finish(true);
            return START_NOT_STICKY;
        }
        if (ACTION_DISMISS.equals(intent.getAction())) {
            finish(false);
            return START_NOT_STICKY;
        }
        releasePlayer();
        prayer = intent.getIntExtra(AlarmScheduler.EXTRA_PRAYER, PrayerTimes.OGLE);
        fullScreen = !intent.getBooleanExtra(EXTRA_PREVIEW, false);
        int id = Notifier.notificationId(prayer);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(id, Notifier.build(this, prayer, true, fullScreen),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(id, Notifier.build(this, prayer, true, fullScreen));
            }
        } catch (Exception e) {
            // Sistem izin vermezse en azından telefonun alarm sesiyle bildirim göster.
            getSystemService(NotificationManager.class)
                    .notify(id, Notifier.build(this, prayer, false, fullScreen, Notifier.CHANNEL_FALLBACK));
            stopSelf();
            return START_NOT_STICKY;
        }
        running = true;
        if (!play()) {
            finish(true);
            return START_NOT_STICKY;
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> finish(true), MAX_MS);
        if (soundPlaying) listenForVolumeKeys();
        return START_NOT_STICKY;
    }

    /**
     * Çalarken ses tuşlarına basılınca susar. Ekran kilitliyken de çalışması için ses tuşlarını
     * bir medya oturumuyla yakalarız; bazı telefonlarda bu çalışmazsa ses seviyesi değişimini
     * ve sessiz moda geçişi de dinleriz.
     */
    private void listenForVolumeKeys() {
        try {
            session = new MediaSession(this, "vakit_alarmi");
            session.setPlaybackToRemote(new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 100, 50) {
                @Override public void onAdjustVolume(int direction) {
                    handler.post(() -> finish(true));
                }
                @Override public void onSetVolumeTo(int volume) {
                    handler.post(() -> finish(true));
                }
            });
            session.setPlaybackState(new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f).build());
            session.setActive(true);
        } catch (Exception ignored) {
            session = null;
        }

        volumeReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(i.getAction())) {
                    if (!ringerAllowsSound(c)) finish(true);
                } else {
                    finish(true); // ses seviyesi değişti: kullanıcı ses tuşuna bastı
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("android.media.VOLUME_CHANGED_ACTION");
        f.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(volumeReceiver, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(volumeReceiver, f);
    }

    private void stopListening() {
        if (session != null) {
            try { session.setActive(false); session.release(); } catch (Exception ignored) {}
            session = null;
        }
        if (volumeReceiver != null) {
            try { unregisterReceiver(volumeReceiver); } catch (Exception ignored) {}
            volumeReceiver = null;
        }
    }

    static Uri soundUri(android.content.Context c) {
        switch (Prefs.sound(c)) {
            case Prefs.SES_TITRESIM:
                return null;
            case Prefs.SES_TELEFON: {
                Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                return u != null ? u : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            case Prefs.SES_OZEL: {
                File f = customFile(c);
                if (f.exists()) return Uri.fromFile(f);
                // dosya yoksa huzur ziline düş
            }
            default:
                return Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.huzur_zili);
        }
    }

    static File customFile(android.content.Context c) {
        return new File(c.getFilesDir(), "ozel_ses");
    }

    private boolean play() {
        Uri uri = soundUri(this);
        if (uri == null || !ringerAllowsSound(this)) {
            // Sadece titreşim seçili ya da telefon sessiz/titreşimde: ses yok. Titreşimi telefonun
            // kendi modu belirler (sessizde titremez). Bildirim ekranda kalır.
            soundPlaying = false;
            handler.postDelayed(() -> finish(true), 4000);
            return true;
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(ATTRS);
            player.setDataSource(this, uri);
            player.setOnCompletionListener(mp -> finish(true));
            player.setOnErrorListener((mp, what, extra) -> { finish(true); return true; });
            player.prepare();
            player.start();
            soundPlaying = true;
            return true;
        } catch (Exception e) {
            releasePlayer();
            return false;
        }
    }

    private void releasePlayer() {
        soundPlaying = false;
        stopListening();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
    }

    /** Sesi durdurur; keepNotification ise bildirim (dokununca kılınış açılır) ekranda kalır. */
    private void finish(boolean keepNotification) {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (running) {
            stopForeground(keepNotification ? STOP_FOREGROUND_DETACH : STOP_FOREGROUND_REMOVE);
            if (keepNotification && prayer >= 0) {
                getSystemService(NotificationManager.class)
                        .notify(Notifier.notificationId(prayer), Notifier.build(this, prayer, false, false));
            }
        }
        running = false;
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
