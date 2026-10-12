package com.sezercan.namazvakti;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.util.DataSource;

/** Alarm sesi telefonun sessiz modunu dinlemeli ve ses tuşuyla susmalı. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SilentModeTest {

    private final Context c = RuntimeEnvironment.getApplication();
    private AudioManager am;

    @Before
    public void setUp() {
        Sync.server = "http://127.0.0.1:9/";
        ShadowMediaPlayer.setMediaInfoProvider((DataSource ds) -> new ShadowMediaPlayer.MediaInfo(60_000, 0));
        am = c.getSystemService(AudioManager.class);
        am.setStreamVolume(AudioManager.STREAM_RING, 5, 0);
        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    private ServiceController<AlarmSoundService> start() {
        ServiceController<AlarmSoundService> sc = Robolectric.buildService(AlarmSoundService.class,
                new Intent().putExtra(AlarmScheduler.EXTRA_PRAYER, PrayerTimes.OGLE)).create().startCommand(0, 1);
        ShadowLooper.idleMainLooper();
        return sc;
    }

    @Test
    public void ringsWhenPhoneIsLoud() {
        start();
        assertTrue(AlarmSoundService.running);
        assertTrue(AlarmSoundService.soundPlaying);
    }

    @Test
    public void silentWhenPhoneIsSilent() {
        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        start();
        assertFalse(AlarmSoundService.soundPlaying);
        assertFalse(AlarmSoundService.ringerAllowsSound(c));
    }

    @Test
    public void silentWhenPhoneIsOnVibrate() {
        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        start();
        assertFalse(AlarmSoundService.soundPlaying);
    }

    @Test
    public void silentWhenRingVolumeIsZero() {
        am.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
        start();
        assertFalse(AlarmSoundService.soundPlaying);
    }

    @Test
    public void volumeKeyStopsRingingAlarm() {
        start();
        assertTrue(AlarmSoundService.soundPlaying);
        c.sendBroadcast(new Intent("android.media.VOLUME_CHANGED_ACTION"));
        ShadowLooper.idleMainLooper();
        assertFalse("ses tuşuna basınca susmalı", AlarmSoundService.soundPlaying);
        assertFalse(AlarmSoundService.running);
    }

    @Test
    public void switchingToSilentStopsRingingAlarm() {
        start();
        assertTrue(AlarmSoundService.soundPlaying);
        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        c.sendBroadcast(new Intent(AudioManager.RINGER_MODE_CHANGED_ACTION));
        ShadowLooper.idleMainLooper();
        assertFalse(AlarmSoundService.soundPlaying);
    }

    @Test
    public void volumeKeyOnLockScreenAlarmStopsSound() {
        start();
        AlarmActivity a = Robolectric.buildActivity(AlarmActivity.class,
                new Intent().putExtra(AlarmScheduler.EXTRA_PRAYER, PrayerTimes.OGLE)).setup().get();
        assertTrue(a.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)));
        // AlarmActivity durdurma isteğini servise gönderir.
        Intent sent = org.robolectric.Shadows.shadowOf(a).getNextStartedService();
        assertNotNull(sent);
        assertEquals(AlarmSoundService.ACTION_STOP, sent.getAction());
    }
}
