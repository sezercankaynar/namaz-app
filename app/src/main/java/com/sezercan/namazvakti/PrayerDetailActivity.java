package com.sezercan.namazvakti;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Bir namazın rekâtlarını ve kısaca nasıl kılındığını gösterir. "rehber" anahtarı genel rehberi açar. */
public class PrayerDetailActivity extends Activity {

    public static final String EXTRA_KEY = "anahtar";

    private static final int[] PART_COLORS = {
            R.color.part_sunnet, R.color.part_farz, R.color.part_son_sunnet, R.color.part_vitir
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Bildirimden açıldıysa çalan alarm sesini durdur.
        if (AlarmSoundService.running) {
            startService(new Intent(this, AlarmSoundService.class).setAction(AlarmSoundService.ACTION_DISMISS));
        }
        LinearLayout root = Ui.page(this, "Vakitler", R.dimen.main_top);
        PrayerInfo info = PrayerInfo.byKey(getIntent().getStringExtra(EXTRA_KEY));
        if (info == null) buildGuide(root); else buildPrayer(root, info);
    }

    private void buildPrayer(LinearLayout root, PrayerInfo info) {
        LinearLayout header = Ui.card(this, Ui.VAKIT_COLORS[info.prayerIndex()], R.dimen.pad_detail);
        Ui.add(header, Ui.text(this, info.name, R.style.Text_PrayerTitle), 0);
        Ui.add(header, Ui.text(this, info.parts.length > 0 ? info.shortSummary() : info.summary, R.style.Text_Summary),
                R.dimen.item_gap_title_large);
        Ui.add(root, header, 0);

        if (info.note != null) {
            LinearLayout note = Ui.card(this, R.color.info_bg, R.dimen.pad_card);
            Ui.add(note, Ui.text(this, "ℹ️ " + info.note, R.style.Text_Body), 0);
            Ui.add(root, note, R.dimen.gap_detail);
        }

        int n = 1;
        for (PrayerInfo.Part part : info.parts) {
            LinearLayout card = Ui.card(this, PART_COLORS[part.kind], R.dimen.pad_detail);
            Ui.add(card, Ui.text(this, (n++) + ") " + part.title, R.style.Text_CardTitle), 0);
            Ui.add(card, Ui.text(this, "Niyet:", R.style.Text_Label), R.dimen.item_gap_detail);
            Ui.add(card, Ui.text(this, part.niyet, R.style.Text_Niyet), R.dimen.item_gap_title);
            Ui.add(card, Ui.text(this, "Kılınışı:", R.style.Text_Label), R.dimen.item_gap_detail);
            int s = 1;
            for (String step : part.stepList()) {
                LinearLayout row = Ui.horizontal(this);
                row.setGravity(android.view.Gravity.TOP);
                row.addView(Ui.circle(this, String.valueOf(s++), R.dimen.num_small, R.style.Text_NumSmall, R.color.surface));
                Ui.addRow(row, Ui.text(this, step, R.style.Text_Body), Ui.weight1(), R.dimen.col_gap);
                Ui.add(card, row, R.dimen.item_gap_steps);
            }
            Ui.add(root, card, R.dimen.gap_detail);
        }

        if (info.parts.length > 0) {
            LinearLayout buttons = Ui.vertical(this);
            Button guide = Ui.add(buttons, Ui.button(this, "📖 Bir rekâtın adımları", R.style.Btn_Primary), 0);
            guide.setOnClickListener(v -> startActivity(new Intent(this, PrayerDetailActivity.class)
                    .putExtra(EXTRA_KEY, "rehber")));
            Ui.add(buttons, textsButton(R.style.Btn_Secondary), R.dimen.gap_row);
            Ui.add(root, buttons, R.dimen.gap_detail);
        }
    }

    private void buildGuide(LinearLayout root) {
        Ui.add(root, Ui.text(this, "Namaz Nasıl Kılınır?", R.style.Text_PrayerTitle), 0);
        Ui.add(root, Ui.text(this, "Rekât sayıları ve bir rekâtın adımları (Hanefî mezhebi)", R.style.Text_Subtitle),
                R.dimen.item_gap_title_large);

        Ui.add(root, Ui.text(this, "Vakitlere göre rekâtlar", R.style.Text_Label), R.dimen.gap_detail);
        LinearLayout table = Ui.card(this, R.color.surface, R.dimen.pad_card);
        table.setPadding(table.getPaddingLeft(), Ui.px(this, R.dimen.item_gap_title), table.getPaddingRight(), Ui.px(this, R.dimen.item_gap_title));
        Object[][] rows = {
                {R.color.imsak, "Sabah", "2 sünnet + 2 farz", 4},
                {R.color.ogle, "Öğle", "4 sünnet + 4 farz + 2 sünnet", 10},
                {R.color.ikindi, "İkindi", "4 sünnet + 4 farz", 8},
                {R.color.aksam, "Akşam", "3 farz + 2 sünnet", 5},
                {R.color.yatsi, "Yatsı", "4 sünnet + 4 farz + 2 sünnet + 3 vitir", 13},
                {R.color.ogle, "Cuma", "4 sünnet + 2 farz + 4 sünnet", 10},
        };
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                View div = new View(this);
                div.setBackgroundColor(Ui.color(this, R.color.divider));
                table.addView(div, new LinearLayout.LayoutParams(Ui.MATCH, Ui.px(this, R.dimen.divider)));
            }
            LinearLayout row = Ui.horizontal(this);
            int v = Ui.px(this, R.dimen.row_pad_v);
            row.setPadding(0, v, 0, v);
            View dot = new View(this);
            dot.setBackground(Ui.oval(this, (int) rows[i][0]));
            int d = Ui.px(this, R.dimen.color_dot);
            row.addView(dot, new LinearLayout.LayoutParams(d, d));
            LinearLayout names = Ui.vertical(this);
            Ui.add(names, Ui.text(this, (String) rows[i][1], R.style.Text_TableName), 0);
            Ui.add(names, Ui.text(this, (String) rows[i][2], R.style.Text_Small), R.dimen.item_gap_title);
            Ui.addRow(row, names, Ui.weight1(), R.dimen.col_gap);
            Ui.addRow(row, Ui.text(this, rows[i][3] + " rekât", R.style.Text_TableCount), Ui.wrap(), R.dimen.col_gap);
            table.addView(row);
        }
        Ui.add(root, table, R.dimen.item_gap_detail);

        Ui.add(root, Ui.text(this, "Bir rekâtın adımları", R.style.Text_Label), R.dimen.gap_detail);
        int n = 1;
        boolean first = true;
        for (String[] s : PrayerInfo.TEMEL_ADIMLAR) {
            LinearLayout card = Ui.card(this, R.color.surface, R.dimen.pad_card);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(Ui.circle(this, String.valueOf(n++), R.dimen.num_large, R.style.Text_NumLarge, R.color.step_circle));
            LinearLayout texts = Ui.vertical(this);
            Ui.add(texts, Ui.text(this, s[0], R.style.Text_StepTitle), 0);
            Ui.add(texts, Ui.text(this, s[1], R.style.Text_Body), R.dimen.item_gap_title);
            Ui.addRow(card, texts, Ui.weight1(), R.dimen.col_gap);
            Ui.add(root, card, first ? R.dimen.item_gap_detail : R.dimen.gap_detail);
            first = false;
        }

        Ui.add(root, textsButton(R.style.Btn_Secondary), R.dimen.gap_detail);
    }

    private Button textsButton(int style) {
        Button b = Ui.button(this, "🤲 Dualar ve Sûreler", style);
        b.setOnClickListener(v -> startActivity(new Intent(this, TextsActivity.class)));
        return b;
    }
}
