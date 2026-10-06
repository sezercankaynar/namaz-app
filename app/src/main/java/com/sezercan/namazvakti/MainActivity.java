package com.sezercan.namazvakti;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Tek sayfalık ana ekran (kaydırma yok): tarih ve geri sayım, günün mısrası, altı vakit
 * ve "Kıldım" düğmeleri, altta menü (Kılınış · Dualar · Takip · Ayarlar).
 */
public class MainActivity extends Activity {

    private static final Locale TR = new Locale("tr", "TR");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView nextLabel, countdown, dateText, subText, verseText, poetText;
    private LinearLayout timesBox;
    private View settingsBadge;
    private Poems.Poem poem;
    private String poemDay;
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
        Sync.syncAsync(this, false, null);
        settingsBadge.setVisibility(SettingsActivity.warningCount(this) > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    // ---- Ekran ----

    private void buildUi() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.color(this, R.color.bg));
        root.setFitsSystemWindows(true);
        int h = Ui.px(this, R.dimen.screen_h);
        root.setPadding(h, Ui.px(this, R.dimen.main_top), h, Ui.px(this, R.dimen.main_bottom_compact));

        // Başlık kartı: tarih, hicri tarih · ilçe, geri sayım
        LinearLayout header = Ui.card(this, R.color.header_bg, R.dimen.pad_card);
        dateText = Ui.add(header, Ui.text(this, "", R.style.Text_Date), 0);
        subText = Ui.add(header, Ui.text(this, "", R.style.Text_Hijri), R.dimen.item_gap_title);
        subText.setSingleLine(true);
        subText.setEllipsize(TextUtils.TruncateAt.END);
        nextLabel = Ui.add(header, Ui.text(this, "", R.style.Text_HeaderLabel), R.dimen.item_gap_detail);
        countdown = Ui.add(header, Ui.text(this, "", R.style.Text_CountdownMain), R.dimen.item_gap_title);
        root.addView(header);

        // Günün mısrası: kalan boşluğu doldurur, sığmayan kısım "…" ile kısalır; dokununca tamamı
        LinearLayout poemCard = Ui.card(this, R.color.poem_bg, R.dimen.pad_card);
        LinearLayout poemHead = Ui.horizontal(this);
        poemHead.addView(Ui.text(this, "Günün mısrası", R.style.Text_Label), Ui.weight1());
        poemHead.addView(Ui.text(this, "›", R.style.Text_Chevron), Ui.wrap());
        poemCard.addView(poemHead);
        verseText = Ui.text(this, "", R.style.Text_VerseCompact);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(Ui.MATCH, 0, 1);
        vlp.topMargin = Ui.px(this, R.dimen.item_gap_title_large);
        poemCard.addView(verseText, vlp);
        poetText = Ui.add(poemCard, Ui.text(this, "", R.style.Text_Poet), R.dimen.item_gap_title);
        poemCard.setClickable(true);
        poemCard.setOnClickListener(v -> showPoemSheet());
        verseText.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitVerse());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.MATCH, 0, 1);
        plp.topMargin = Ui.px(this, R.dimen.gap_category);
        root.addView(poemCard, plp);

        // Vakitler
        timesBox = Ui.vertical(this);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        tlp.topMargin = Ui.px(this, R.dimen.gap_category);
        root.addView(timesBox, tlp);

        // Alt menü
        LinearLayout tabs = Ui.horizontal(this);
        tabs.setBackground(Ui.shape(this, R.color.surface, R.dimen.radius_card, R.dimen.stroke_card, R.color.border_card));
        tabs.addView(tab("📖", "Kılınış", null, () -> openDetail("rehber")), Ui.weight1());
        tabs.addView(tab("🤲", "Dualar", null, () -> startActivity(new Intent(this, TextsActivity.class))), Ui.weight1());
        tabs.addView(tab("📊", "Takip", null, () -> startActivity(new Intent(this, TrackActivity.class))), Ui.weight1());
        settingsBadge = new View(this);
        tabs.addView(tab("⚙️", "Ayarlar", settingsBadge, () -> startActivity(new Intent(this, SettingsActivity.class))), Ui.weight1());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.px(this, R.dimen.tab_bar_h));
        blp.topMargin = Ui.px(this, R.dimen.gap_category);
        root.addView(tabs, blp);

        setContentView(root);
    }

    private View tab(String icon, String label, View badge, Runnable onClick) {
        LinearLayout t = Ui.vertical(this);
        t.setGravity(Gravity.CENTER);
        t.setBackground(getDrawable(R.drawable.btn_icon));
        t.setClickable(true);
        t.setContentDescription(label);
        t.setOnClickListener(v -> onClick.run());
        t.addView(Ui.text(this, icon, R.style.Text_TabIcon), Ui.wrap());
        LinearLayout labelRow = Ui.horizontal(this);
        labelRow.addView(Ui.text(this, label, R.style.Text_Tab), Ui.wrap());
        if (badge != null) {
            badge.setBackground(Ui.oval(this, R.color.badge_dot));
            int d = Ui.px(this, R.dimen.badge_dot);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d, d);
            lp.setMarginStart(Ui.px(this, R.dimen.item_gap_title));
            labelRow.addView(badge, lp);
            badge.setContentDescription("Kontrol edilmesi gereken ayar var");
        }
        t.addView(labelRow, Ui.wrap());
        return t;
    }

    private void refresh() {
        Calendar now = Times.today();
        SimpleDateFormat dayFmt = new SimpleDateFormat("d MMMM yyyy, EEEE", TR);
        dayFmt.setTimeZone(Times.TR);
        dateText.setText(dayFmt.format(now.getTime()));
        String hijri = Diyanet.hijri(this, Prefs.ilce(this), now);
        String place = Districts.nameOf(this, Prefs.city(this), Prefs.ilce(this)).replace(" (merkez)", "");
        subText.setText((hijri != null && !hijri.isEmpty() ? hijri + " · " : "") + "📍 " + place);

        refreshPoem(now);

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
            View row = vakitRow(i, hm.format(new Date(times[i])), isNext, friday, times[i] <= nowMs);
            Ui.add(timesBox, row, R.dimen.gap_compact);
        }
    }

    /** Tek satırlık vakit: ad · (alarm kapalıysa 🔕) · saat · Kıldım */
    private View vakitRow(int prayer, String time, boolean isNext, boolean friday, boolean entered) {
        PrayerInfo info = PrayerInfo.forPrayer(prayer, friday);
        LinearLayout row = Ui.horizontal(this);
        row.setMinimumHeight(Ui.px(this, R.dimen.row_compact_h));
        row.setPadding(Ui.px(this, R.dimen.row_pad_start), 0, Ui.px(this, R.dimen.item_gap_steps), 0);
        row.setBackground(Ui.shape(this, Ui.VAKIT_COLORS[prayer], R.dimen.radius_card, R.dimen.stroke_next,
                isNext ? R.color.accent : R.color.transparent));

        String name = (prayer == PrayerTimes.OGLE && friday) ? "Cuma" : PrayerInfo.VAKIT_NAMES[prayer];
        row.addView(Ui.text(this, name, R.style.Text_RowName), Ui.weight1());
        if (prayer != PrayerTimes.GUNES && !Prefs.alarmOn(this, prayer)) {
            TextView off = Ui.text(this, "🔕", R.style.Text_Small);
            off.setContentDescription("Alarm kapalı");
            Ui.addRow(row, off, Ui.wrap(), R.dimen.item_gap_steps);
        }
        Ui.addRow(row, Ui.text(this, time, R.style.Text_RowTime), Ui.wrap(), R.dimen.col_gap);

        LinearLayout slot = Ui.horizontal(this);
        slot.setGravity(Gravity.CENTER);
        if (prayer != PrayerTimes.GUNES && entered) {
            String today = Habits.dayKey(Times.today());
            boolean done = Habits.prayed(this, today, prayer);
            TextView pill = WeekUi.pill(this, done);
            pill.setOnClickListener(v -> {
                Habits.set(this, today, prayer, !done);
                Sync.publishAsync(this);
                refresh();
            });
            slot.addView(pill, Ui.wrap());
        }
        Ui.addRow(row, slot, new LinearLayout.LayoutParams(Ui.px(this, R.dimen.pill_slot_w), Ui.WRAP), R.dimen.item_gap_steps);

        if (prayer != PrayerTimes.GUNES) row.setOnClickListener(v -> openDetail(info.key));
        row.setContentDescription(name + " " + time);
        return row;
    }

    private void updateCountdown() {
        long left = Math.max(0, nextTime - System.currentTimeMillis());
        long s = (left + 999) / 1000;
        countdown.setText(String.format(TR, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
    }

    // ---- Günün mısrası ----

    private void refreshPoem(Calendar now) {
        String day = Habits.dayKey(now);
        if (day.equals(poemDay)) return;
        poemDay = day;
        poem = Poems.forDay(this, now);
        // Ana ekranda dizeler " / " ile tek akışta; tamamı dokununca açılan pencerede.
        verseText.setText(poem.verse.replace("\n", " / "));
        poetText.setText("— " + poem.poet);
        fitVerse();
    }

    /** Mısra kutusuna sığan satır sayısını ayarlar (son satırın altında satır aralığı gerekmez). */
    private void fitVerse() {
        int lh = verseText.getLineHeight();
        int avail = verseText.getHeight() - verseText.getPaddingTop() - verseText.getPaddingBottom();
        if (lh <= 0 || avail <= 0) return;
        android.graphics.Paint.FontMetricsInt fm = verseText.getPaint().getFontMetricsInt();
        int textH = fm.descent - fm.ascent;
        int lines = avail < textH ? 1 : 1 + (avail - textH) / lh;
        if (verseText.getMaxLines() != lines) verseText.post(() -> verseText.setMaxLines(lines));
    }

    private void showPoemSheet() {
        if (poem == null) return;
        Ui.Sheet sheet = new Ui.Sheet(this, "Günün mısrası");
        Ui.add(sheet.body, Ui.text(this, poem.verse, R.style.Text_Verse), 0);
        if (poem.original != null) {
            Ui.add(sheet.body, Ui.text(this, poem.original, R.style.Text_VerseOriginal), R.dimen.item_gap_detail);
        }
        if (poem.meaning != null) {
            Ui.add(sheet.body, Ui.text(this, "Günümüz Türkçesiyle: " + poem.meaning, R.style.Text_Small), R.dimen.item_gap_detail);
        }
        Ui.add(sheet.body, Ui.text(this, "— " + poem.poet, R.style.Text_Poet), R.dimen.gap_detail);
        Ui.add(sheet.body, Ui.text(this, poem.info, R.style.Text_Small), R.dimen.item_gap_title);
        sheet.show();
    }

    private void openDetail(String key) {
        Intent i = new Intent(this, PrayerDetailActivity.class);
        i.putExtra(PrayerDetailActivity.EXTRA_KEY, key);
        startActivity(i);
    }
}
