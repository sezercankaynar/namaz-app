package com.sezercan.namazvakti;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final Locale TR = new Locale("tr", "TR");
    private static final int REQ_SOUND_FILE = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView nextName, countdown, dateText, hijriText, sourceText;
    private LinearLayout timesBox, warnBox;
    private Spinner ilceSpinner;
    private Button soundButton;
    private long nextTime;
    private int lastMinute = -1;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            int minute = (int) (now / 60_000);
            if (minute != lastMinute || now >= nextTime) {
                lastMinute = minute;
                refresh();
            }
            updateCountdown();
            handler.postDelayed(this, 1000 - (System.currentTimeMillis() % 1000));
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        lastMinute = -1;
        handler.post(tick);
        if (Diyanet.needsRefresh(this)) Diyanet.refreshAsync(this, this::refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void buildUi() {
        LinearLayout root = Ui.page(this);

        // Başlık kartı: tarih ve sıradaki vakit
        LinearLayout header = Ui.card(this, Ui.LAVENDER);
        int p = Ui.dp(this, 20);
        header.setPadding(p, p, p, p);
        header.addView(Ui.text(this, "🕌 Namaz Vakitleri", 24, Ui.TEXT, true));
        dateText = Ui.text(this, "", 15, Ui.GREY, false);
        header.addView(dateText);
        hijriText = Ui.text(this, "", 14, Ui.GREY, false);
        header.addView(hijriText);
        Ui.space(header, 14);
        nextName = Ui.text(this, "", 17, Ui.PLUM, true);
        header.addView(nextName);
        countdown = Ui.text(this, "", 34, Ui.TEXT, true);
        header.addView(countdown);
        root.addView(header);

        // Konum: il ve ilçe
        LinearLayout loc = Ui.card(this, Ui.WHITE);
        loc.addView(Ui.text(this, "📍 Konum", 15, Ui.PLUM, true));
        Spinner citySpinner = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Cities.NAMES);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(ad);
        citySpinner.setSelection(Prefs.city(this));
        citySpinner.setOnItemSelectedListener(new SimpleSelect(pos -> {
            if (pos != Prefs.city(this)) {
                Prefs.setCity(this, pos);
                fillIlce();
                locationChanged();
            }
        }));
        loc.addView(row("İl", citySpinner));
        ilceSpinner = new Spinner(this);
        fillIlce();
        ilceSpinner.setOnItemSelectedListener(new SimpleSelect(pos -> {
            String id = Districts.ids(this, Prefs.city(this))[pos];
            if (!id.equals(Prefs.ilce(this))) {
                Prefs.setIlce(this, id);
                locationChanged();
            }
        }));
        loc.addView(row("İlçe", ilceSpinner));
        sourceText = Ui.text(this, "", 13, Ui.GREY, false);
        sourceText.setPadding(0, Ui.dp(this, 6), 0, 0);
        loc.addView(sourceText);
        root.addView(loc);

        warnBox = Ui.vertical(this);
        root.addView(warnBox);

        root.addView(Ui.text(this, "Vakte dokunursanız o namazın kılınışını görürsünüz. Sağdaki düğme o vaktin alarmını açar/kapatır.", 13, Ui.GREY, false));
        timesBox = Ui.vertical(this);
        root.addView(timesBox);

        Ui.space(root, 6);
        Button guide = Ui.button(this, "📖 Namaz nasıl kılınır?", Ui.VAKIT_COLORS[0]);
        guide.setOnClickListener(v -> openDetail("rehber"));
        root.addView(guide);

        Button texts = Ui.button(this, "🤲 Dualar ve Sûreler", Ui.VAKIT_COLORS[3]);
        texts.setOnClickListener(v -> startActivity(new Intent(this, TextsActivity.class)));
        root.addView(texts);

        soundButton = Ui.button(this, "", Ui.VAKIT_COLORS[1]);
        soundButton.setOnClickListener(v -> chooseSound());
        root.addView(soundButton);
        updateSoundButton();

        Button test = Ui.button(this, "🔔 Alarmı dene", Ui.LILAC);
        test.setOnClickListener(v -> testAlarm());
        root.addView(test);

        Ui.space(root, 8);
        root.addView(Ui.text(this, "Vakitler, internet olduğunda Diyanet İşleri Başkanlığı'nın seçtiğiniz ilçe için yayınladığı resmi vakitlerden alınır ve 30 gün telefonda saklanır. İnternet yoksa vakitler Diyanet yöntemiyle telefonda hesaplanır.", 12, Ui.GREY, false));
    }

    private LinearLayout row(String label, View v) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(this, label, 16, Ui.TEXT, true);
        r.addView(t, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return r;
    }

    private void fillIlce() {
        int city = Prefs.city(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Districts.names(this, city));
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ilceSpinner.setAdapter(ad);
        String[] ids = Districts.ids(this, city);
        String cur = Prefs.ilce(this);
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(cur)) ilceSpinner.setSelection(i);
    }

    private void locationChanged() {
        AlarmScheduler.scheduleNext(this);
        refresh();
        sourceText.setText("⏳ Diyanet vakitleri indiriliyor…");
        Diyanet.refreshAsync(this, this::refresh);
    }

    private void refresh() {
        Calendar now = Times.today();
        SimpleDateFormat dayFmt = new SimpleDateFormat("d MMMM yyyy, EEEE", TR);
        dayFmt.setTimeZone(Times.TR);
        dateText.setText(dayFmt.format(now.getTime()));
        String hijri = Diyanet.hijri(this, Prefs.ilce(this), now);
        hijriText.setText(hijri != null && !hijri.isEmpty() ? hijri : "");
        hijriText.setVisibility(hijri != null && !hijri.isEmpty() ? View.VISIBLE : View.GONE);

        int days = Diyanet.daysAhead(this, Prefs.ilce(this));
        sourceText.setText(days > 0
                ? "✅ Diyanet'in resmi vakitleri • " + days + " günlük kayıtlı"
                : "⏳ Hesaplanan vakitler gösteriliyor. İnternete bağlanınca Diyanet'in resmi vakitleri otomatik alınır.");

        long nowMs = now.getTimeInMillis();
        long[] times = Times.forDay(this, now);

        int nextIdx = -1;
        nextTime = 0;
        for (int i = 0; i < PrayerTimes.COUNT; i++) {
            if (times[i] > nowMs) { nextIdx = i; nextTime = times[i]; break; }
        }
        if (nextIdx < 0) {
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            nextIdx = PrayerTimes.IMSAK;
            nextTime = Times.forDay(this, tomorrow)[PrayerTimes.IMSAK];
        }
        nextName.setText(PrayerInfo.VAKIT_NAMES[nextIdx] + " vaktine kalan");
        updateCountdown();

        boolean friday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", TR);
        hm.setTimeZone(Times.TR);
        timesBox.removeAllViews();
        for (int i = 0; i < PrayerTimes.COUNT; i++) {
            final int prayer = i;
            boolean isNext = (i == nextIdx) && nextTime == times[i];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(this, 14);
            row.setPadding(p, p, Ui.dp(this, 6), p);
            row.setBackground(isNext
                    ? Ui.rounded(Ui.VAKIT_COLORS[i], Ui.dp(this, 14), Ui.dp(this, 2), Ui.PLUM)
                    : Ui.rounded(Ui.VAKIT_COLORS[i], Ui.dp(this, 14)));
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

            TextView time = Ui.text(this, hm.format(new Date(times[i])), 22, isNext ? Ui.PLUM : Ui.TEXT, true);
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

    private void updateCountdown() {
        long left = Math.max(0, nextTime - System.currentTimeMillis());
        long s = (left + 999) / 1000;
        countdown.setText(String.format(TR, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
    }

    // ---- Alarm sesi ----

    private static final String[] SOUND_LABELS = {
            "🎐 Huzur zili (uygulamanın sesi)",
            "⏰ Telefonun alarm sesi",
            "🎵 Telefonumdan ses dosyası seç (ilahi vb.)",
            "📳 Sadece titreşim"
    };

    private void updateSoundButton() {
        int mode = Prefs.sound(this);
        String name = mode == Prefs.SES_OZEL ? "🎵 " + Prefs.customSoundName(this) : SOUND_LABELS[mode];
        soundButton.setText("Alarm sesi: " + name.replaceFirst(" \\(.*\\)$", ""));
    }

    private void chooseSound() {
        new AlertDialog.Builder(this)
                .setTitle("Alarm sesi")
                .setSingleChoiceItems(SOUND_LABELS, Prefs.sound(this), (d, which) -> {
                    d.dismiss();
                    if (which == Prefs.SES_OZEL) {
                        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("audio/*");
                        try {
                            startActivityForResult(i, REQ_SOUND_FILE);
                        } catch (Exception e) {
                            Toast.makeText(this, "Dosya seçici açılamadı", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Prefs.setSound(this, which);
                        updateSoundButton();
                        offerPreview();
                    }
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SOUND_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(AlarmSoundService.customFile(this))) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Toast.makeText(this, "Ses dosyası kopyalanamadı", Toast.LENGTH_LONG).show();
            return;
        }
        String name = "Seçilen ses";
        try (Cursor cur = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) name = cur.getString(0).replaceFirst("\\.[A-Za-z0-9]+$", "");
        } catch (Exception ignored) {}
        Prefs.setCustomSoundName(this, name);
        Prefs.setSound(this, Prefs.SES_OZEL);
        updateSoundButton();
        offerPreview();
    }

    private void offerPreview() {
        if (Prefs.sound(this) == Prefs.SES_TITRESIM) return;
        new AlertDialog.Builder(this)
                .setMessage("Seçtiğiniz sesi şimdi dinlemek ister misiniz?")
                .setPositiveButton("Dinle", (d, w) -> playNow())
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void playNow() {
        Notifier.alarm(this, AlarmScheduler.currentPrayer(this, System.currentTimeMillis()));
        Toast.makeText(this, "Durdurmak için bildirimdeki “Sesi durdur”a basın.", Toast.LENGTH_LONG).show();
    }

    private void testAlarm() {
        new AlertDialog.Builder(this)
                .setTitle("Alarmı dene")
                .setItems(new String[]{"Şimdi çal", "1 dakika sonra çal (ekranı kilitleyip bekleyin)"}, (d, which) -> {
                    if (which == 0) {
                        playNow();
                    } else {
                        AlarmScheduler.scheduleTest(this);
                        Toast.makeText(this, "Test alarmı 1 dakika sonra çalacak.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    // ---- Uyarılar ----

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
            addWarning("💡 Bazı telefonlar pil tasarrufu için alarmları geciktirir. Alarmın her zaman tam vaktinde çalması için pil kısıtlamasını kaldırın.",
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
        LinearLayout card = Ui.card(this, Ui.PEACH);
        card.addView(Ui.text(this, msg, 14, Ui.TEXT, false));
        Button b = Ui.button(this, action, Ui.WHITE);
        b.setOnClickListener(v -> r.run());
        card.addView(b);
        warnBox.addView(card);
    }

    private void openDetail(String key) {
        Intent i = new Intent(this, PrayerDetailActivity.class);
        i.putExtra(PrayerDetailActivity.EXTRA_KEY, key);
        startActivity(i);
    }

    /** Spinner seçimleri için kısa dinleyici. */
    private static final class SimpleSelect implements AdapterView.OnItemSelectedListener {
        interface OnPick { void pick(int pos); }
        private final OnPick cb;
        SimpleSelect(OnPick cb) { this.cb = cb; }
        @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) { cb.pick(pos); }
        @Override public void onNothingSelected(AdapterView<?> parent) {}
    }
}
