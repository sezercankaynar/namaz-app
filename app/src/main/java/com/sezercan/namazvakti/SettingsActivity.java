package com.sezercan.namazvakti;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Ayarlar: konum, vakit alarmları, alarm sesi, deneme ve izin uyarıları. */
public class SettingsActivity extends Activity {

    private static final int REQ_SOUND_FILE = 7;

    private TextView sourceText, soundText;
    private LinearLayout warnBox;
    private Spinner ilceSpinner;
    private RadioGroup soundGroup;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.page(this, "Vakitler", R.dimen.main_top);
        Ui.add(root, Ui.text(this, "Ayarlar", R.style.Text_PrayerTitle), 0);
        warnBox = Ui.add(root, Ui.vertical(this), 0);

        // Konum
        Ui.add(root, Ui.text(this, "Konum", R.style.Text_Label), R.dimen.gap_block);
        LinearLayout loc = Ui.card(this, R.color.surface, R.dimen.pad_card);
        Ui.add(loc, Ui.text(this, "İl", R.style.Text_SmallBold), 0);
        Spinner citySpinner = spinner(Cities.NAMES);
        citySpinner.setSelection(Prefs.city(this));
        citySpinner.setOnItemSelectedListener(new SimpleSelect(pos -> {
            if (pos != Prefs.city(this)) {
                Prefs.setCity(this, pos);
                fillIlce();
                locationChanged();
            }
        }));
        Ui.add(loc, citySpinner, R.dimen.item_gap_steps);
        Ui.add(loc, Ui.text(this, "İlçe", R.style.Text_SmallBold), R.dimen.item_gap_detail);
        ilceSpinner = Ui.add(loc, spinner(new String[0]), R.dimen.item_gap_steps);
        fillIlce();
        ilceSpinner.setOnItemSelectedListener(new SimpleSelect(pos -> {
            String id = Districts.ids(this, Prefs.city(this))[pos];
            if (!id.equals(Prefs.ilce(this))) {
                Prefs.setIlce(this, id);
                locationChanged();
            }
        }));
        sourceText = Ui.add(loc, Ui.text(this, "", R.style.Text_Small), R.dimen.item_gap_detail);
        Ui.add(root, loc, R.dimen.item_gap_detail);

        // Vakit alarmları
        Ui.add(root, Ui.text(this, "Vakit alarmları", R.style.Text_Label), R.dimen.gap_block);
        LinearLayout alarms = Ui.card(this, R.color.surface, R.dimen.pad_card);
        for (int p = 0; p < PrayerTimes.COUNT; p++) {
            final int prayer = p;
            if (p > 0) Ui.add(alarms, WeekUi.divider(this), R.dimen.item_gap_title);
            LinearLayout row = Ui.horizontal(this);
            row.setMinimumHeight(Ui.px(this, R.dimen.touch_min));
            View dot = new View(this);
            dot.setBackground(Ui.oval(this, Ui.VAKIT_COLORS[p]));
            int d = Ui.px(this, R.dimen.color_dot);
            row.addView(dot, new LinearLayout.LayoutParams(d, d));
            Ui.addRow(row, Ui.text(this, PrayerInfo.VAKIT_NAMES[p], R.style.Text_ListRow), Ui.weight1(), R.dimen.col_gap);
            Switch sw = new Switch(this, null, 0, R.style.VakitSwitch);
            sw.setContentDescription(PrayerInfo.VAKIT_NAMES[p] + " alarmı");
            if (p == PrayerTimes.GUNES) {
                sw.setChecked(false);
                sw.setEnabled(false);
            } else {
                sw.setChecked(Prefs.alarmOn(this, p));
                sw.setOnCheckedChangeListener((b, on) -> {
                    Prefs.setAlarmOn(this, prayer, on);
                    AlarmScheduler.scheduleNext(this);
                });
            }
            Ui.addRow(row, sw, Ui.wrap(), R.dimen.col_gap);
            Ui.add(alarms, row, R.dimen.item_gap_title);
        }
        Ui.add(root, alarms, R.dimen.item_gap_detail);

        // Alarm sesi
        Ui.add(root, Ui.text(this, "Alarm sesi", R.style.Text_Label), R.dimen.gap_block);
        LinearLayout soundRow = new LinearLayout(this, null, 0, R.style.SettingRow);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundText = Ui.text(this, "", R.style.Text_SettingRow);
        soundRow.addView(soundText, Ui.weight1());
        Ui.addRow(soundRow, Ui.text(this, "›", R.style.Text_Chevron), Ui.wrap(), R.dimen.col_gap);
        soundRow.setOnClickListener(v -> showSoundSheet());
        Ui.add(root, soundRow, R.dimen.item_gap_detail);
        updateSoundRow();
        Button test = Ui.add(root, Ui.button(this, "🔔 Alarmı dene", R.style.Btn_Secondary), R.dimen.gap_row);
        test.setOnClickListener(v -> showTestSheet());

        Ui.add(root, Ui.text(this, "Vakitler, internet olduğunda Diyanet İşleri Başkanlığı'nın seçtiğiniz ilçe için yayınladığı resmi vakitlerden alınır ve 30 gün telefonda saklanır. İnternet yoksa Diyanet yöntemiyle telefonda hesaplanır.", R.style.Text_Small), R.dimen.gap_block);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshWarnings();
        refreshSource();
    }

    private void refreshSource() {
        int days = Diyanet.daysAhead(this, Prefs.ilce(this));
        sourceText.setText(days > 0
                ? "✅ Diyanet'in resmi vakitleri • " + days + " günlük kayıtlı"
                : "⏳ Hesaplanan vakitler gösteriliyor. İnternete bağlanınca Diyanet'in resmi vakitleri otomatik alınır.");
    }

    /** Alarmın çalmasını engelleyebilecek kaç ayar var (ana ekrandaki menü işareti için). */
    static int warningCount(Context c) {
        int n = 0;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (!nm.areNotificationsEnabled()) n++;
        if (Build.VERSION.SDK_INT >= 34 && !nm.canUseFullScreenIntent()) n++;
        if (Build.VERSION.SDK_INT >= 31 && !c.getSystemService(AlarmManager.class).canScheduleExactAlarms()) n++;
        if (!c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName())) n++;
        return n;
    }

    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this, Spinner.MODE_DROPDOWN);
        s.setBackground(getDrawable(R.drawable.spinner_bg));
        s.setPopupBackgroundDrawable(getDrawable(R.drawable.spinner_popup));
        int ph = Ui.px(this, R.dimen.spinner_pad_h);
        s.setPadding(ph, 0, ph, 0);
        s.setDropDownVerticalOffset(Ui.px(this, R.dimen.spinner_height));
        s.setAdapter(adapter(items));
        s.setLayoutParams(new LinearLayout.LayoutParams(Ui.MATCH, Ui.px(this, R.dimen.spinner_height)));
        return s;
    }

    private ArrayAdapter<String> adapter(String[] items) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        ad.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return ad;
    }

    private void fillIlce() {
        int city = Prefs.city(this);
        ilceSpinner.setAdapter(adapter(Districts.names(this, city)));
        String[] ids = Districts.ids(this, city);
        String cur = Prefs.ilce(this);
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(cur)) ilceSpinner.setSelection(i);
    }

    private void locationChanged() {
        AlarmScheduler.scheduleNext(this);
        sourceText.setText("⏳ Diyanet vakitleri indiriliyor…");
        Diyanet.refreshAsync(this, this::refreshSource);
    }

    private String soundLabel(int mode) {
        switch (mode) {
            case Prefs.SES_TELEFON: return "⏰ Telefonun alarm sesi";
            case Prefs.SES_OZEL: return AlarmSoundService.customFile(this).exists()
                    ? "🎵 " + Prefs.customSoundName(this) : "🎵 Telefonumdan ses dosyası seç (ilahi vb.)";
            case Prefs.SES_TITRESIM: return "📳 Sadece titreşim";
            default: return "🎐 Huzur zili";
        }
    }

    private void updateSoundRow() {
        int mode = Prefs.sound(this);
        String name = mode == Prefs.SES_OZEL ? Prefs.customSoundName(this)
                : soundLabel(mode).replaceFirst("^\\S+ ", "");
        soundText.setText("Alarm sesi: " + name);
    }

    private void showSoundSheet() {
        Ui.Sheet sheet = new Ui.Sheet(this, "Alarm sesi");
        soundGroup = new RadioGroup(this);
        int[] modes = {Prefs.SES_HUZUR, Prefs.SES_TELEFON, Prefs.SES_OZEL, Prefs.SES_TITRESIM};
        for (int mode : modes) {
            RadioButton rb = new RadioButton(this, null, 0, R.style.SoundRadio);
            rb.setId(View.generateViewId());
            rb.setTag(mode);
            rb.setText(soundLabel(mode));
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(Ui.MATCH, Ui.WRAP);
            if (soundGroup.getChildCount() > 0) lp.topMargin = Ui.px(this, R.dimen.gap_list);
            soundGroup.addView(rb, lp);
            if (mode == Prefs.sound(this)) rb.setChecked(true);
            rb.setOnClickListener(v -> {
                if (mode == Prefs.SES_OZEL) {
                    pickSoundFile();
                } else {
                    Prefs.setSound(this, mode);
                    updateSoundRow();
                }
            });
        }
        Ui.add(sheet.body, soundGroup, 0);

        LinearLayout actions = Ui.horizontal(this);
        Button listen = Ui.button(this, "▶ Dinle", R.style.Btn_Secondary_Sheet);
        listen.setOnClickListener(v -> {
            if (Prefs.sound(this) == Prefs.SES_TITRESIM) {
                Toast.makeText(this, "Sadece titreşim seçili; ses çalınmaz.", Toast.LENGTH_SHORT).show();
            } else {
                Notifier.preview(this, AlarmScheduler.currentPrayer(this, System.currentTimeMillis()));
            }
        });
        actions.addView(listen, Ui.weight1());
        Button ok = Ui.button(this, "Tamam", R.style.Btn_Primary_Sheet);
        ok.setOnClickListener(v -> sheet.dialog.dismiss());
        Ui.addRow(actions, ok, Ui.weight1(), R.dimen.gap_row);
        Ui.add(sheet.body, actions, R.dimen.gap_block);

        sheet.dialog.setOnDismissListener(d -> {
            soundGroup = null;
            stopPreview();
        });
        sheet.show();
    }

    private void stopPreview() {
        if (AlarmSoundService.running) {
            startService(new Intent(this, AlarmSoundService.class).setAction(AlarmSoundService.ACTION_DISMISS));
        }
    }

    private void pickSoundFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        try {
            startActivityForResult(i, REQ_SOUND_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "Dosya seçici açılamadı", Toast.LENGTH_LONG).show();
            syncSoundRadios();
        }
    }

    /** Pencere açıksa radyo seçimini kayıtlı ayara eşitler. */
    private void syncSoundRadios() {
        if (soundGroup == null) return;
        for (int i = 0; i < soundGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) soundGroup.getChildAt(i);
            int mode = (int) rb.getTag();
            rb.setText(soundLabel(mode));
            if (mode == Prefs.sound(this)) rb.setChecked(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SOUND_FILE) return;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            boolean copied = false;
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(AlarmSoundService.customFile(this))) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                copied = true;
            } catch (Exception e) {
                Toast.makeText(this, "Ses dosyası kopyalanamadı", Toast.LENGTH_LONG).show();
            }
            if (copied) {
                String name = "Seçilen ses";
                try (Cursor cur = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cur != null && cur.moveToFirst()) name = cur.getString(0).replaceFirst("\\.[A-Za-z0-9]+$", "");
                } catch (Exception ignored) {}
                Prefs.setCustomSoundName(this, name);
                Prefs.setSound(this, Prefs.SES_OZEL);
            }
        }
        updateSoundRow();
        syncSoundRadios();
    }

    private void showTestSheet() {
        Ui.Sheet sheet = new Ui.Sheet(this, "Alarmı dene");
        Ui.add(sheet.body, Ui.text(this, "“1 dakika sonra” seçeneğinde ekranı kilitleyip bekleyin; alarm kilit ekranında tam ekran görünür.", R.style.Text_Body), 0);
        Button now = Ui.add(sheet.body, Ui.button(this, "Şimdi çal", R.style.Btn_Primary_Sheet), R.dimen.gap_block);
        now.setOnClickListener(v -> {
            sheet.dialog.dismiss();
            Notifier.preview(this, AlarmScheduler.currentPrayer(this, System.currentTimeMillis()));
            Toast.makeText(this, "Durdurmak için bildirimdeki “Sesi durdur”a basın.", Toast.LENGTH_LONG).show();
        });
        Button later = Ui.add(sheet.body, Ui.button(this, "1 dakika sonra çal", R.style.Btn_Secondary_Sheet), R.dimen.gap_row);
        later.setOnClickListener(v -> {
            sheet.dialog.dismiss();
            AlarmScheduler.scheduleTest(this);
            Toast.makeText(this, "Test alarmı 1 dakika sonra çalacak.", Toast.LENGTH_LONG).show();
        });
        sheet.show();
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
        if (Build.VERSION.SDK_INT >= 34 && !nm.canUseFullScreenIntent()) {
            addWarning("⚠️ Kilit ekranında tam ekran alarm izni kapalı. Alarm yalnızca bildirim olarak görünür.", "İzni ver", () ->
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName()))));
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
            addWarning("Bazı telefonlar pil tasarrufu için alarmları geciktirir.",
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
        LinearLayout card = Ui.card(this, R.color.warning_bg, R.dimen.pad_card);
        Ui.add(card, Ui.text(this, msg, R.style.Text_Body), 0);
        Button b = Ui.add(card, Ui.button(this, action, R.style.Btn_Primary_Small), R.dimen.item_gap_detail);
        b.setOnClickListener(v -> r.run());
        Ui.add(warnBox, card, R.dimen.gap_block);
        // İlk uyarının da üstünde blok boşluğu olsun.
        ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = Ui.px(this, R.dimen.gap_block);
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
