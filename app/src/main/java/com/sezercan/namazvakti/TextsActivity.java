package com.sezercan.namazvakti;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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
        LinearLayout root = Ui.page(this);
        int cat = getIntent().getIntExtra(EXTRA_CAT, -1);
        int item = getIntent().getIntExtra(EXTRA_ITEM, -1);
        Texts.Category[] cats = Texts.all(this);

        if (cat < 0 || cat >= cats.length) buildCategories(root, cats);
        else if (item < 0 || item >= cats[cat].items.length) buildList(root, cat, cats[cat]);
        else buildDetail(root, cat, cats[cat], item);
    }

    private void buildCategories(LinearLayout root, Texts.Category[] cats) {
        LinearLayout header = Ui.card(this, Ui.LAVENDER);
        header.addView(Ui.text(this, "🤲 Dualar ve Sûreler", 26, Ui.TEXT, true));
        header.addView(Ui.text(this, "Arapça yazılışı, okunuşu ve anlamıyla", 15, Ui.GREY, false));
        root.addView(header);

        for (int i = 0; i < cats.length; i++) {
            final int idx = i;
            Texts.Category c = cats[i];
            LinearLayout card = Ui.card(this, c.color);
            card.addView(Ui.text(this, c.name, 20, Ui.TEXT, true));
            card.addView(Ui.text(this, c.description + " • " + c.items.length + " başlık", 14, Ui.GREY, false));
            card.setOnClickListener(v -> open(idx, -1));
            root.addView(card);
        }
    }

    private void buildList(LinearLayout root, int catIdx, Texts.Category cat) {
        LinearLayout header = Ui.card(this, cat.color);
        header.addView(Ui.text(this, cat.name, 26, Ui.TEXT, true));
        header.addView(Ui.text(this, cat.description, 15, Ui.GREY, false));
        root.addView(header);

        for (int i = 0; i < cat.items.length; i++) {
            final int idx = i;
            Texts.Item it = cat.items[i];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(this, 16);
            row.setPadding(p, p, p, p);
            row.setBackground(Ui.rounded(Ui.WHITE, Ui.dp(this, 14)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            row.setLayoutParams(lp);

            TextView num = Ui.text(this, String.valueOf(i + 1), 15, Ui.PLUM, true);
            num.setGravity(Gravity.CENTER);
            num.setBackground(Ui.rounded(cat.color, Ui.dp(this, 16)));
            row.addView(num, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));

            TextView title = Ui.text(this, it.title, 18, Ui.TEXT, true);
            title.setPadding(Ui.dp(this, 12), 0, 0, 0);
            row.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(Ui.text(this, "›", 24, Ui.GREY, false));
            row.setOnClickListener(v -> open(catIdx, idx));
            root.addView(row);
        }
    }

    private void buildDetail(LinearLayout root, int catIdx, Texts.Category cat, int itemIdx) {
        Texts.Item it = cat.items[itemIdx];
        LinearLayout header = Ui.card(this, cat.color);
        header.addView(Ui.text(this, cat.name, 14, Ui.GREY, false));
        header.addView(Ui.text(this, it.title, 26, Ui.TEXT, true));
        root.addView(header);

        LinearLayout arCard = Ui.card(this, Ui.WHITE);
        arCard.addView(Ui.text(this, "Arapça", 14, Ui.PLUM, true));
        TextView ar = Ui.text(this, it.arabic, 26, Ui.TEXT, false);
        ar.setTextDirection(View.TEXT_DIRECTION_RTL);
        ar.setGravity(Gravity.END);
        ar.setLineSpacing(0, 1.6f);
        ar.setTextIsSelectable(true);
        arCard.addView(ar);
        root.addView(arCard);

        LinearLayout rd = Ui.card(this, Ui.WHITE);
        rd.addView(Ui.text(this, "Okunuşu", 14, Ui.PLUM, true));
        TextView reading = Ui.text(this, it.reading, 17, Ui.TEXT, false);
        reading.setTextIsSelectable(true);
        rd.addView(reading);
        root.addView(rd);

        LinearLayout mn = Ui.card(this, Ui.WHITE);
        mn.addView(Ui.text(this, "Anlamı", 14, Ui.PLUM, true));
        mn.addView(Ui.text(this, it.meaning, 16, Ui.TEXT, false));
        root.addView(mn);

        if (it.note != null) {
            LinearLayout note = Ui.card(this, Ui.CREAM_NOTE);
            note.addView(Ui.text(this, "ℹ️ " + it.note, 15, Ui.TEXT, false));
            root.addView(note);
        }

        if (itemIdx + 1 < cat.items.length) {
            Button next = Ui.button(this, "Sonraki: " + cat.items[itemIdx + 1].title + "  ›", cat.color);
            next.setOnClickListener(v -> { open(catIdx, itemIdx + 1); finish(); });
            root.addView(next);
        }
    }

    private void open(int cat, int item) {
        Intent i = new Intent(this, TextsActivity.class);
        i.putExtra(EXTRA_CAT, cat);
        i.putExtra(EXTRA_ITEM, item);
        startActivity(i);
    }
}
