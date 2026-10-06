package com.sezercan.namazvakti;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Dualar ve Sûreler: kategoriler → başlıklar → Arapça yazılış, okunuş ve anlam. */
public class TextsActivity extends Activity {

    static final String EXTRA_CAT = "kategori";
    static final String EXTRA_ITEM = "oge";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int cat = getIntent().getIntExtra(EXTRA_CAT, -1);
        int item = getIntent().getIntExtra(EXTRA_ITEM, -1);
        Texts.Category[] cats = Texts.all(this);

        if (cat < 0 || cat >= cats.length) {
            buildCategories(Ui.page(this, "Vakitler", R.dimen.main_top), cats);
        } else if (item < 0 || item >= cats[cat].items.length) {
            buildList(Ui.page(this, "Dualar ve Sûreler", R.dimen.main_top), cat, cats[cat]);
        } else {
            buildDetail(Ui.page(this, cats[cat].name, R.dimen.main_top), cat, cats[cat], item);
        }
    }

    private void buildCategories(LinearLayout root, Texts.Category[] cats) {
        Ui.add(root, Ui.text(this, "Dualar ve Sûreler", R.style.Text_PrayerTitle), 0);
        Ui.add(root, Ui.text(this, "Arapça yazılışı, okunuşu ve anlamıyla", R.style.Text_Subtitle), R.dimen.item_gap_title_large);

        for (int i = 0; i < cats.length; i++) {
            final int idx = i;
            Texts.Category c = cats[i];
            LinearLayout card = Ui.horizontal(this);
            card.setMinimumHeight(Ui.px(this, R.dimen.category_min));
            int v = Ui.px(this, R.dimen.cat_pad_v), h = Ui.px(this, R.dimen.cat_pad_h);
            card.setPadding(h, v, h, v);
            card.setBackground(Ui.shape(this, c.colorRes, R.dimen.radius_card));
            LinearLayout texts = Ui.vertical(this);
            Ui.add(texts, Ui.text(this, c.name, R.style.Text_CategoryTitle), 0);
            Ui.add(texts, Ui.text(this, c.description + " • " + c.items.length + " başlık", R.style.Text_Subtitle), R.dimen.item_gap_title);
            card.addView(texts, Ui.weight1());
            Ui.addRow(card, Ui.text(this, "›", R.style.Text_ChevronLarge), Ui.wrap(), R.dimen.col_gap);
            card.setOnClickListener(v2 -> open(idx, -1));
            Ui.add(root, card, i == 0 ? R.dimen.gap_block : R.dimen.gap_category);
        }
    }

    private void buildList(LinearLayout root, int catIdx, Texts.Category cat) {
        Ui.add(root, Ui.text(this, cat.name, R.style.Text_PrayerTitle), 0);
        Ui.add(root, Ui.text(this, cat.description, R.style.Text_Subtitle), R.dimen.item_gap_title_large);

        for (int i = 0; i < cat.items.length; i++) {
            final int idx = i;
            LinearLayout row = Ui.horizontal(this);
            row.setMinimumHeight(Ui.px(this, R.dimen.list_row_min));
            int p = Ui.px(this, R.dimen.pad_card);
            row.setPadding(p, Ui.px(this, R.dimen.item_gap_title), p, Ui.px(this, R.dimen.item_gap_title));
            row.setBackground(Ui.shape(this, R.color.surface, R.dimen.radius_card, R.dimen.stroke_card, R.color.border_card));
            row.addView(Ui.circle(this, String.valueOf(i + 1), R.dimen.num_medium, R.style.Text_NumMedium, cat.colorRes));
            Ui.addRow(row, Ui.text(this, cat.items[i].title, R.style.Text_ListRow), Ui.weight1(), R.dimen.col_gap);
            Ui.addRow(row, Ui.text(this, "›", R.style.Text_Chevron), Ui.wrap(), R.dimen.col_gap);
            row.setOnClickListener(v -> open(catIdx, idx));
            Ui.add(root, row, i == 0 ? R.dimen.gap_detail : R.dimen.gap_list);
        }
    }

    private void buildDetail(LinearLayout root, int catIdx, Texts.Category cat, int itemIdx) {
        Texts.Item it = cat.items[itemIdx];
        Ui.add(root, Ui.text(this, cat.name, R.style.Text_Label), 0);
        Ui.add(root, Ui.text(this, it.title, R.style.Text_TextDetailTitle), R.dimen.item_gap_title);

        // Arapça (besmele ayrı satırda, ortalı)
        LinearLayout ar = Ui.card(this, R.color.card_arabic, R.dimen.pad_detail);
        Ui.add(ar, Ui.text(this, "Arapça", R.style.Text_Label), 0);
        String arabic = it.arabic;
        int nl = arabic.indexOf('\n');
        if (nl > 0 && arabic.startsWith("بِسْمِ")) {
            Ui.add(ar, Ui.text(this, arabic.substring(0, nl), R.style.Text_Besmele), R.dimen.item_gap_detail);
            arabic = arabic.substring(nl + 1);
        }
        Ui.add(ar, Ui.text(this, arabic, R.style.Text_Arabic), R.dimen.item_gap_detail);
        Ui.add(root, ar, R.dimen.gap_detail);

        LinearLayout rd = Ui.card(this, R.color.card_reading, R.dimen.pad_detail);
        Ui.add(rd, Ui.text(this, "Okunuşu", R.style.Text_Label), 0);
        TextView reading = Ui.add(rd, Ui.text(this, it.reading, R.style.Text_Reading), R.dimen.item_gap_detail);
        reading.setTextIsSelectable(true);
        Ui.add(root, rd, R.dimen.gap_detail);

        LinearLayout mn = Ui.card(this, R.color.card_meaning, R.dimen.pad_detail);
        Ui.add(mn, Ui.text(this, "Anlamı", R.style.Text_Label), 0);
        Ui.add(mn, Ui.text(this, it.meaning, R.style.Text_Reading), R.dimen.item_gap_detail);
        Ui.add(root, mn, R.dimen.gap_detail);

        if (it.note != null) {
            LinearLayout note = Ui.card(this, R.color.info_bg, R.dimen.pad_card);
            Ui.add(note, Ui.text(this, "ℹ️ " + it.note, R.style.Text_Body), 0);
            Ui.add(root, note, R.dimen.gap_detail);
        }

        if (itemIdx + 1 < cat.items.length) {
            Button next = Ui.button(this, "Sonraki: " + cat.items[itemIdx + 1].title + " ›", R.style.Btn_Primary);
            next.setOnClickListener(v -> { open(catIdx, itemIdx + 1); finish(); });
            Ui.add(root, next, R.dimen.gap_detail);
        }
    }

    private void open(int cat, int item) {
        Intent i = new Intent(this, TextsActivity.class);
        i.putExtra(EXTRA_CAT, cat);
        i.putExtra(EXTRA_ITEM, item);
        startActivity(i);
    }
}
