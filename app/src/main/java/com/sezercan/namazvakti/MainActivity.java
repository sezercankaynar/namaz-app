package com.sezercan.namazvakti;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
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
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final Locale TR = new Locale("tr", "TR");
    private static final int REQ_SOUND_FILE = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView nextLabel, countdown, dateText, hijriText, sourceText, soundText;
    private LinearLayout timesBox, warnBox, weekBox;
    private Spinner ilceSpinner;
    private RadioGroup soundGroup;
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
        Sync.syncAsync(this, false, this::refreshWeek);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    // ---- Ekran ----

    private void buildUi() {
        LinearLayout root = Ui.page(this, null, R.dimen.main_top);

        // Başlık kartı
        LinearLayout header = Ui.card(this, R.color.header_bg, R.dimen.pad_header);
        Ui.add(header, Ui.text(this, "🕌 Namaz Vakitleri", R.style.Text_AppTitle), 0);
        dateText = Ui.add(header, Ui.text(this, "", R.style.Text_Date), R.dimen.item_gap_title_large);
        hijriText = Ui.add(header, Ui.text(this, "", R.style.Text_Hijri), R.dimen.item_gap_title);
        nextLabel = Ui.add(header, Ui.text(this, "", R.style.Text_HeaderLabel), R.dimen.gap_block);
        countdown = Ui.add(header, Ui.text(this, "", R.style.Text_Countdown), R.dimen.item_gap_title);
        Ui.add(root, header, 0);

        // Konum kartı
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
        Ui.add(root, loc, R.dimen.gap_block);

        warnBox = Ui.add(root, Ui.vertical(this), 0);
        timesBox = Ui.add(root, Ui.vertical(this), R.dimen.gap_block);
        weekBox = Ui.add(root, Ui.vertical(this), R.dimen.gap_block);

        // Düğmeler
        LinearLayout buttons = Ui.vertical(this);
        Button guide = Ui.add(buttons, Ui.button(this, "📖 Namaz nasıl kılınır?", R.style.Btn_Primary), 0);
        guide.setOnClickListener(v -> openDetail("rehber"));
        Button texts = Ui.add(buttons, Ui.button(this, "🤲 Dualar ve Sûreler", R.style.Btn_Primary), R.dimen.gap_row);
        texts.setOnClickListener(v -> startActivity(new Intent(this, TextsActivity.class)));

        LinearLayout soundRow = new LinearLayout(this, null, 0, R.style.SettingRow);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundText = Ui.text(this, "", R.style.Text_SettingRow);
        soundRow.addView(soundText, Ui.weight1());
        Ui.addRow(soundRow, Ui.text(this, "›", R.style.Text_Chevron), Ui.wrap(), R.dimen.col_gap);
        soundRow.setOnClickListener(v -> showSoundSheet());
        Ui.add(buttons, soundRow, R.dimen.gap_row);
        updateSoundRow();

        Button test = Ui.add(buttons, Ui.button(this, "🔔 Alarmı dene", R.style.Btn_Secondary), R.dimen.gap_row);
        test.setOnClickListener(v -> showTestSheet());
        Ui.add(root, buttons, R.dimen.gap_block);

        Ui.add(root, Ui.text(this, "Vakitler, internet olduğunda Diyanet İşleri Başkanlığı'nın seçtiğiniz ilçe için yayınladığı resmi vakitlerden alınır ve 30 gün telefonda saklanır. İnternet yoksa Diyanet yöntemiyle telefonda hesaplanır.", R.style.Text_Small), R.dimen.gap_block);
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
        boolean hasHijri = hijri != null && !hijri.isEmpty();
        hijriText.setText(hasHijri ? hijri : "");
        hijriText.setVisibility(hasHijri ? View.VISIBLE : View.GONE);

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
        nextLabel.setText(PrayerInfo.VAKIT_NAMES[nextIdx] + " vaktine kalan");
        updateCountdown();

        boolean friday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", TR);
        hm.setTimeZone(Times.TR);
        timesBox.removeAllViews();
        for (int i = 0; i < PrayerTimes.COUNT; i++) {
            boolean isNext = (i == nextIdx) && nextTime == times[i];
            boolean entered = times[i] <= nowMs;
            Ui.add(timesBox, vakitRow(i, hm.format(new Date(times[i])), isNext, friday, entered), R.dimen.gap_row);
        }
        refreshWeek();
        refreshWarnings();
    }

    /** Vakit satırı: [ad (+ek) + SIRADAKİ | saat] / [rekât özeti]  ·  anahtar */
    private View vakitRow(int prayer, String time, boolean isNext, boolean friday, boolean entered) {
        PrayerInfo info = PrayerInfo.forPrayer(prayer, friday);
        LinearLayout row = Ui.horizontal(this);
        row.setPadding(Ui.px(this, R.dimen.row_pad_start), Ui.px(this, R.dimen.row_pad_v),
                Ui.px(this, R.dimen.row_pad_end), Ui.px(this, R.dimen.row_pad_v));
        row.setBackground(Ui.shape(this, Ui.VAKIT_COLORS[prayer], R.dimen.radius_card, R.dimen.stroke_next,
                isNext ? R.color.accent : R.color.transparent));

        LinearLayout left = Ui.vertical(this);
        LinearLayout first = Ui.horizontal(this);
        SpannableStringBuilder name = new SpannableStringBuilder(PrayerInfo.VAKIT_NAMES[prayer]);
        String suffix = prayer == PrayerTimes.IMSAK ? "(Sabah)" : (prayer == PrayerTimes.OGLE && friday ? "(Cuma)" : null);
        if (suffix != null) {
            int start = name.length() + 1;
            name.append(' ').append(suffix);
            name.setSpan(new TextAppearanceSpan(this, R.style.Text_Small), start, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        TextView nameView = Ui.text(this, name, R.style.Text_CardTitle);
        first.addView(nameView, Ui.wrap());
        if (isNext) {
            TextView badge = Ui.text(this, "SIRADAKİ", R.style.Text_Badge);
            badge.setBackground(Ui.shape(this, R.color.accent, R.dimen.radius_badge));
            Ui.addRow(first, badge, Ui.wrap(), R.dimen.item_gap_steps);
        }
        first.addView(new View(this), Ui.weight1());
        Ui.addRow(first, Ui.text(this, time, R.style.Text_RowTime), Ui.wrap(), R.dimen.col_gap);
        left.addView(first);
        String sub = prayer == PrayerTimes.GUNES ? "Namaz vakti değil" : info.shortSummary();
        LinearLayout second = Ui.horizontal(this);
        second.addView(Ui.text(this, sub, R.style.Text_Small), Ui.weight1());
        if (prayer != PrayerTimes.GUNES && entered) {
            // Vakit girdiyse "Kıldım" işareti
            String today = Habits.dayKey(Times.today());
            boolean done = Habits.prayed(this, today, prayer);
            TextView pill = WeekUi.pill(this, done);
            pill.setOnClickListener(v -> {
                Habits.set(this, today, prayer, !done);
                Sync.publishAsync(this);
                refresh();
            });
            Ui.addRow(second, pill, Ui.wrap(), R.dimen.col_gap);
        }
        Ui.add(left, second, R.dimen.item_gap_title);
        row.addView(left, Ui.weight1());

        Switch sw = new Switch(this, null, 0, R.style.VakitSwitch);
        sw.setContentDescription(PrayerInfo.VAKIT_NAMES[prayer] + " alarmı");
        if (prayer == PrayerTimes.GUNES) {
            sw.setChecked(false);
            sw.setEnabled(false);
        } else {
            sw.setChecked(Prefs.alarmOn(this, prayer));
            sw.setOnCheckedChangeListener((b, on) -> {
                Prefs.setAlarmOn(this, prayer, on);
                AlarmScheduler.scheduleNext(this);
                Toast.makeText(this, PrayerInfo.VAKIT_NAMES[prayer] + " alarmı " + (on ? "açıldı" : "kapatıldı"), Toast.LENGTH_SHORT).show();
            });
            row.setOnClickListener(v -> openDetail(info.key));
        }
        Ui.addRow(row, sw, Ui.wrap(), R.dimen.col_gap);
        return row;
    }

    private void updateCountdown() {
        long left = Math.max(0, nextTime - System.currentTimeMillis());
        long s = (left + 999) / 1000;
        countdown.setText(String.format(TR, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
    }

    // ---- Haftalık takip ----

    private void refreshWeek() {
        if (weekBox == null) return;
        weekBox.removeAllViews();
        LinearLayout card = Ui.card(this, R.color.surface, R.dimen.pad_card);
        Ui.add(card, Ui.text(this, "Haftalık özet", R.style.Text_Label), 0);
        Ui.add(card, WeekUi.person(this, "Sen", null, d -> Habits.mask(this, d), this::showDaySheet),
                R.dimen.item_gap_detail);

        boolean paired = Sync.paired(this);
        for (Sync.Friend f : Sync.friends(this)) {
            Ui.add(card, WeekUi.divider(this), R.dimen.gap_detail);
            Ui.add(card, WeekUi.person(this, f.name, WeekUi.updatedText(f.updated), f::mask, null),
                    R.dimen.gap_detail);
        }

        if (!paired) {
            Ui.add(card, Ui.text(this, "Bir arkadaşınla eşleşirsen birbirinizin haftalık özetini görebilirsiniz.",
                    R.style.Text_Small), R.dimen.gap_detail);
            Button pair = Ui.add(card, Ui.button(this, "👥 Arkadaşınla eşleş", R.style.Btn_Secondary), R.dimen.item_gap_detail);
            pair.setOnClickListener(v -> showPairSheet());
        } else {
            if (Sync.friends(this).isEmpty()) {
                Ui.add(card, Ui.text(this, "Arkadaşının katılması bekleniyor. Kodu ona gönderdin mi?",
                        R.style.Text_Small), R.dimen.gap_detail);
            }
            LinearLayout row = new LinearLayout(this, null, 0, R.style.SettingRow);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(Ui.text(this, "👥 Kod: " + Sync.pretty(Sync.code(this)), R.style.Text_SettingRow), Ui.weight1());
            Ui.addRow(row, Ui.text(this, "›", R.style.Text_Chevron), Ui.wrap(), R.dimen.col_gap);
            row.setOnClickListener(v -> showCodeSheet());
            Ui.add(card, row, R.dimen.gap_detail);
        }
        Ui.add(card, Ui.text(this, "Noktalar yukarıdan aşağı: İmsak, Öğle, İkindi, Akşam, Yatsı. Kendi günlerinden birine dokunarak o günü düzeltebilirsin.", R.style.Text_Small),
                R.dimen.item_gap_detail);
        Ui.add(weekBox, card, 0);
    }

    /** Kendi geçmiş bir günümüzü düzeltme penceresi. */
    private void showDaySheet(String dayKey) {
        Ui.Sheet sheet = new Ui.Sheet(this, WeekUi.longDay(dayKey));
        for (int p : Habits.PRAYERS) {
            LinearLayout row = Ui.horizontal(this);
            row.setMinimumHeight(Ui.px(this, R.dimen.sound_row_min));
            row.addView(Ui.text(this, PrayerInfo.VAKIT_NAMES[p], R.style.Text_ListRow), Ui.weight1());
            Switch sw = new Switch(this, null, 0, R.style.VakitSwitch);
            sw.setContentDescription(PrayerInfo.VAKIT_NAMES[p] + " kılındı");
            sw.setChecked(Habits.prayed(this, dayKey, p));
            sw.setOnCheckedChangeListener((b, on) -> Habits.set(this, dayKey, p, on));
            Ui.addRow(row, sw, Ui.wrap(), R.dimen.col_gap);
            Ui.add(sheet.body, row, 0);
        }
        Button ok = Ui.add(sheet.body, Ui.button(this, "Tamam", R.style.Btn_Primary_Sheet), R.dimen.gap_block);
        ok.setOnClickListener(v -> sheet.dialog.dismiss());
        sheet.dialog.setOnDismissListener(d -> {
            Sync.publishAsync(this);
            refresh();
        });
        sheet.show();
    }

    private EditText input(String text, String hint, int inputType) {
        EditText e = new EditText(this);
        e.setTextAppearance(R.style.Text_Input);
        e.setHintTextColor(Ui.color(this, R.color.text_secondary));
        e.setBackground(Ui.shape(this, R.color.bg, R.dimen.radius_spinner, R.dimen.stroke_card, R.color.border_input));
        int ph = Ui.px(this, R.dimen.input_pad_h);
        e.setPadding(ph, 0, ph, 0);
        e.setSingleLine(true);
        e.setInputType(inputType);
        // Tek satır ayarı en küçük yüksekliği sıfırladığı için en sonda verilir.
        e.setMinHeight(Ui.px(this, R.dimen.input_height));
        e.setGravity(android.view.Gravity.CENTER_VERTICAL);
        e.setText(text);
        e.setHint(hint);
        return e;
    }

    private void showPairSheet() {
        Ui.Sheet sheet = new Ui.Sheet(this, "Arkadaşınla eşleş");
        if (sheet.dialog.getWindow() != null) {
            sheet.dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        Ui.add(sheet.body, Ui.text(this, "Adın", R.style.Text_Label), 0);
        EditText name = Ui.add(sheet.body, input(Sync.myName(this), "Örn. Sezer",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS), R.dimen.item_gap_steps);
        Ui.add(sheet.body, Ui.text(this, "Arkadaşının ekranında bu isim görünür.", R.style.Text_Small), R.dimen.item_gap_title);

        Button create = Ui.add(sheet.body, Ui.button(this, "Yeni kod oluştur", R.style.Btn_Primary_Sheet), R.dimen.gap_block);
        create.setOnClickListener(v -> {
            if (!saveName(name)) return;
            Sync.pair(this, Sync.newCode());
            sheet.dialog.dismiss();
            Sync.syncAsync(this, true, this::refreshWeek);
            refreshWeek();
            showCodeSheet();
        });

        Ui.add(sheet.body, Ui.text(this, "Arkadaşın kod oluşturduysa", R.style.Text_Label), R.dimen.gap_block);
        EditText code = Ui.add(sheet.body, input("", "ABCD-2345",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS),
                R.dimen.item_gap_steps);
        Button join = Ui.add(sheet.body, Ui.button(this, "Koda katıl", R.style.Btn_Secondary_Sheet), R.dimen.gap_row);
        join.setOnClickListener(v -> {
            if (!saveName(name)) return;
            String c = Sync.normalize(code.getText().toString());
            if (c == null) {
                Toast.makeText(this, "Kod 8 karakter olmalı (örn. ABCD-2345).", Toast.LENGTH_LONG).show();
                return;
            }
            Sync.pair(this, c);
            sheet.dialog.dismiss();
            Toast.makeText(this, "Eşleşildi. Arkadaşının özeti birkaç saniye içinde görünür.", Toast.LENGTH_LONG).show();
            Sync.syncAsync(this, true, this::refreshWeek);
            refreshWeek();
        });
        Ui.add(sheet.body, Ui.text(this, "Paylaşılan tek bilgi: adın ve son 7 günde hangi vakitleri kıldığın.",
                R.style.Text_Small), R.dimen.gap_block);
        sheet.show();
    }

    private boolean saveName(EditText name) {
        String n = name.getText().toString().trim();
        if (n.isEmpty()) {
            Toast.makeText(this, "Lütfen adını yaz.", Toast.LENGTH_SHORT).show();
            name.requestFocus();
            return false;
        }
        Sync.setMyName(this, n);
        return true;
    }

    private void showCodeSheet() {
        String code = Sync.code(this);
        if (code == null) return;
        Ui.Sheet sheet = new Ui.Sheet(this, "Eşleşme kodu");
        Ui.add(sheet.body, Ui.text(this, Sync.pretty(code), R.style.Text_Code), 0);
        Ui.add(sheet.body, Ui.text(this, "Bu kodu arkadaşına gönder. O da uygulamada “Arkadaşınla eşleş → Koda katıl” bölümüne bu kodu yazsın. Aynı kodu birden fazla arkadaşın kullanabilir.",
                R.style.Text_Body), R.dimen.gap_detail);
        Button share = Ui.add(sheet.body, Ui.button(this, "📤 Kodu paylaş", R.style.Btn_Primary_Sheet), R.dimen.gap_block);
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                    "Namaz Vakti uygulamasında namazlarımızı birlikte takip edelim. Eşleşme kodum: " + Sync.pretty(code));
            startActivity(Intent.createChooser(i, "Kodu paylaş"));
        });
        Button unpair = Ui.add(sheet.body, Ui.button(this, "Eşleşmeyi kaldır", R.style.Btn_Secondary_Sheet), R.dimen.gap_row);
        unpair.setOnClickListener(v -> {
            if (!"onay".equals(unpair.getTag())) {
                unpair.setTag("onay");
                unpair.setText("Emin misin? Kaldırmak için tekrar dokun");
                return;
            }
            Sync.unpair(this);
            sheet.dialog.dismiss();
            refreshWeek();
        });
        sheet.show();
    }

    // ---- Alarm sesi ----

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
