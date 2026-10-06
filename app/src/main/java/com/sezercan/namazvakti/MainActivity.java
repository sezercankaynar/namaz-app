package com.sezercan.namazvakti;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final Locale TR = new Locale("tr", "TR");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private TextView nextText, dateText;
    private LinearLayout timesBox;
    private LinearLayout warnBox;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 30_000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.GREEN);
        Notifier.ensureChannel(this);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.scheduleNext(this);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF7F7F2);
        scroll.setFitsSystemWindows(true);
        root = Ui.vertical(this);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        // Başlık
        LinearLayout header = Ui.card(this, Ui.GREEN);
        header.addView(Ui.text(this, "🕌 Namaz Vakitleri", 24, 0xFFFFFFFF, true));
        dateText = Ui.text(this, "", 15, 0xFFC8E6C9, false);
        header.addView(dateText);
        Ui.space(header, 8);
        nextText = Ui.text(this, "", 18, 0xFFFFFFFF, true);
        header.addView(nextText);
        root.addView(header);

        // Şehir seçimi
        LinearLayout cityRow = new LinearLayout(this);
        cityRow.setGravity(Gravity.CENTER_VERTICAL);
        cityRow.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        cityRow.addView(Ui.text(this, "📍 Şehir:  ", 17, Ui.TEXT, true));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Cities.NAMES);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(ad);
        spinner.setSelection(Prefs.city(this));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (pos != Prefs.city(MainActivity.this)) {
                    Prefs.setCity(MainActivity.this, pos);
                    AlarmScheduler.scheduleNext(MainActivity.this);
                    refresh();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        cityRow.addView(spinner, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(cityRow);

        warnBox = Ui.vertical(this);
        root.addView(warnBox);

        root.addView(Ui.text(this, "Vakte dokunursanız o namazın kılınışını görürsünüz. Sağdaki düğme o vaktin alarmını açar/kapatır.", 13, Ui.GREY, false));
        timesBox = Ui.vertical(this);
        root.addView(timesBox);

        Ui.space(root, 6);
        Button guide = Ui.button(this, "📖 Namaz nasıl kılınır? / Dualar");
        guide.setOnClickListener(v -> openDetail("rehber"));
        root.addView(guide);

        Button test = Ui.button(this, "🔔 Alarmı dene (1 dakika sonra çalar)");
        test.setOnClickListener(v -> {
            AlarmScheduler.scheduleTest(this);
            Toast.makeText(this, "Test alarmı 1 dakika sonra çalacak. Ekranı kapatıp bekleyebilirsiniz.", Toast.LENGTH_LONG).show();
        });
        root.addView(test);

        Ui.space(root, 8);
        root.addView(Ui.text(this, "Vakitler Diyanet İşleri Başkanlığı'nın hesaplama yöntemiyle, internet gerekmeden hesaplanır. Ezan okunuşuyla 1-2 dakika fark olabilir.", 12, Ui.GREY, false));

        setContentView(scroll);
    }

    private void refresh() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dayFmt = new SimpleDateFormat("d MMMM yyyy, EEEE", TR);
        dateText.setText(Cities.NAMES[Prefs.city(this)] + " • " + dayFmt.format(now.getTime()));

        long nowMs = now.getTimeInMillis();
        long[] times = Prefs.todayTimes(this, now);

        // Sıradaki vakit (bugün yoksa yarının imsakı)
        int nextIdx = -1;
        long nextTime = 0;
        for (int i = 0; i < PrayerTimes.COUNT; i++) {
            if (times[i] > nowMs) { nextIdx = i; nextTime = times[i]; break; }
        }
        if (nextIdx < 0) {
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            nextIdx = PrayerTimes.IMSAK;
            nextTime = Prefs.todayTimes(this, tomorrow)[PrayerTimes.IMSAK];
        }
        long mins = (nextTime - nowMs + 59_999) / 60_000;
        String left = mins >= 60 ? (mins / 60) + " saat " + (mins % 60) + " dakika" : mins + " dakika";
        nextText.setText("Sıradaki: " + PrayerInfo.VAKIT_NAMES[nextIdx] + " • " + left + " kaldı");

        boolean friday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", TR);
        timesBox.removeAllViews();
        for (int i = 0; i < PrayerTimes.COUNT; i++) {
            final int prayer = i;
            boolean isNext = (i == nextIdx) && nextTime == times[i];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(this, 12);
            row.setPadding(p, p, Ui.dp(this, 4), p);
            row.setBackground(Ui.rounded(isNext ? 0xFFC8E6C9 : 0xFFFFFFFF, Ui.dp(this, 10)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            row.setLayoutParams(lp);

            PrayerInfo info = PrayerInfo.forPrayer(i, friday);
            LinearLayout names = Ui.vertical(this);
            String name = (i == PrayerTimes.OGLE && friday) ? "Öğle (Cuma)" : PrayerInfo.VAKIT_NAMES[i];
            names.addView(Ui.text(this, name, 18, Ui.TEXT, true));
            String sub = (i == PrayerTimes.GUNES) ? "Namaz vakti değil" : info.summary.replaceFirst("^.*? namazı ", "");
            names.addView(Ui.text(this, sub, 12, Ui.GREY, false));
            row.addView(names, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView time = Ui.text(this, hm.format(new Date(times[i])), 22, Ui.GREEN, true);
            time.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
            row.addView(time);

            Switch sw = new Switch(this);
            sw.setChecked(Prefs.alarmOn(this, i));
            sw.setOnCheckedChangeListener((b, on) -> {
                Prefs.setAlarmOn(this, prayer, on);
                AlarmScheduler.scheduleNext(this);
                Toast.makeText(this, PrayerInfo.VAKIT_NAMES[prayer] + " alarmı " + (on ? "açıldı" : "kapatıldı"), Toast.LENGTH_SHORT).show();
            });
            row.addView(sw);

            row.setOnClickListener(v -> openDetail(info.key));
            timesBox.addView(row);
        }
        refreshWarnings();
    }

    /** Alarmın çalmasını engelleyebilecek ayarlar varsa kullanıcıya düğme göster. */
    private void refreshWarnings() {
        warnBox.removeAllViews();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (!nm.areNotificationsEnabled()) {
            addWarning("⚠️ Bildirimler kapalı. Alarm görünmez.", "Bildirimleri aç", () -> {
                Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(i);
            });
        }
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = getSystemService(AlarmManager.class);
            if (!am.canScheduleExactAlarms()) {
                addWarning("⚠️ Tam zamanlı alarm izni kapalı. Alarm birkaç dakika gecikebilir.", "İzni ver", () ->
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName()))));
            }
        }
        PowerManager pm = getSystemService(PowerManager.class);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            addWarning("💡 Bazı telefonlar pil tasarrufu için alarmları geciktirir. Alarmın her zaman çalması için pil kısıtlamasını kaldırın.",
                    "Pil kısıtlamasını kaldır", () -> {
                        try {
                            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception e) {
                            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                        }
                    });
        }
    }

    private void addWarning(String msg, String action, Runnable r) {
        LinearLayout card = Ui.card(this, 0xFFFFF3E0);
        card.addView(Ui.text(this, msg, 14, Ui.TEXT, false));
        Button b = Ui.button(this, action);
        b.setOnClickListener(v -> r.run());
        card.addView(b);
        warnBox.addView(card);
    }

    private void openDetail(String key) {
        Intent i = new Intent(this, PrayerDetailActivity.class);
        i.putExtra(PrayerDetailActivity.EXTRA_KEY, key);
        startActivity(i);
    }
}
