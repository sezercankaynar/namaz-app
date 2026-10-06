package com.sezercan.namazvakti;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Kilit ekranında açılan tam ekran vakit alarmı. */
public class AlarmActivity extends Activity {

    private static final Locale TR = new Locale("tr", "TR");

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int prayer = getIntent().getIntExtra(AlarmScheduler.EXTRA_PRAYER, PrayerTimes.OGLE);
        Calendar now = Times.today();
        boolean friday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        PrayerInfo info = PrayerInfo.forPrayer(prayer, friday);

        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.color(this, R.color.navy_dark));
        root.setFitsSystemWindows(true);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int h = Ui.px(this, R.dimen.screen_h);
        root.setPadding(h, Ui.px(this, R.dimen.main_bottom), h, Ui.px(this, R.dimen.main_bottom));

        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", TR);
        hm.setTimeZone(Times.TR);
        SimpleDateFormat day = new SimpleDateFormat("d MMMM, EEEE", TR);
        day.setTimeZone(Times.TR);
        TextView clock = Ui.text(this, hm.format(now.getTime()), R.style.Text_LockClock);
        clock.setGravity(Gravity.CENTER);
        Ui.add(root, clock, 0);
        TextView date = Ui.text(this, day.format(now.getTime()), R.style.Text_Date);
        date.setGravity(Gravity.CENTER);
        Ui.add(root, date, R.dimen.item_gap_title_large);

        LinearLayout spacer = new LinearLayout(this);
        root.addView(spacer, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));

        LinearLayout card = Ui.vertical(this);
        int p = Ui.px(this, R.dimen.pad_detail);
        card.setPadding(p, p, p, p);
        card.setBackground(Ui.shape(this, R.color.surface, R.dimen.radius_notification));
        Ui.add(card, Ui.text(this, "🕌 Namaz Vakti", R.style.Text_SmallBold), 0);
        Ui.add(card, Ui.text(this, info.alarmTitle, R.style.Text_NotifTitle), R.dimen.item_gap_title_large);
        Ui.add(card, Ui.text(this, info.summary + (info.summary.endsWith(".") ? "" : ".")
                + " Nasıl kılındığını görmek için dokunun.", R.style.Text_Body), R.dimen.item_gap_title);
        Button how = Ui.add(card, Ui.button(this, "📖 Nasıl kılınır?", R.style.Btn_Primary), R.dimen.gap_block);
        how.setOnClickListener(v -> {
            stopSound();
            Intent i = new Intent(this, PrayerDetailActivity.class).putExtra(PrayerDetailActivity.EXTRA_KEY, info.key);
            KeyguardManager km = getSystemService(KeyguardManager.class);
            if (km != null && km.isKeyguardLocked()) {
                km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                    @Override public void onDismissSucceeded() { startActivity(i); finish(); }
                });
            } else {
                startActivity(i);
                finish();
            }
        });
        Button stop = Ui.add(card, Ui.button(this, "🔇 Sesi durdur", R.style.Btn_Secondary), R.dimen.gap_row);
        stop.setOnClickListener(v -> { stopSound(); finish(); });
        Ui.add(root, card, R.dimen.gap_block);

        setContentView(root);
    }

    private void stopSound() {
        if (AlarmSoundService.running) {
            startService(new Intent(this, AlarmSoundService.class).setAction(AlarmSoundService.ACTION_STOP));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        stopSound();
        super.onBackPressed();
    }
}
