package com.sezercan.namazvakti;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

/** Haftalık takip: kendi ve arkadaşların 7 günlük özeti, eşleşme. */
public class TrackActivity extends Activity {

    private LinearLayout weekBox;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.page(this, "Vakitler", R.dimen.main_top);
        Ui.add(root, Ui.text(this, "Haftalık takip", R.style.Text_PrayerTitle), 0);
        Ui.add(root, Ui.text(this, "Kıldığın vakitleri ana ekrandaki “Kıldım” düğmesiyle işaretle.",
                R.style.Text_Subtitle), R.dimen.item_gap_title_large);
        weekBox = Ui.add(root, Ui.vertical(this), R.dimen.gap_block);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshWeek();
        Sync.syncAsync(this, false, this::refreshWeek);
    }

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
            refreshWeek();
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
}
