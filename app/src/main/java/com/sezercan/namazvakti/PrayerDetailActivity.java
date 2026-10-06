package com.sezercan.namazvakti;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Bir namazın rekâtlarını ve kısaca nasıl kılındığını gösterir. "rehber" anahtarı genel rehberi açar. */
public class PrayerDetailActivity extends Activity {

    public static final String EXTRA_KEY = "anahtar";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.GREEN);
        String key = getIntent().getStringExtra(EXTRA_KEY);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF7F7F2);
        scroll.setFitsSystemWindows(true);
        LinearLayout root = Ui.vertical(this);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        PrayerInfo info = PrayerInfo.byKey(key);
        if (info == null) buildGuide(root); else buildPrayer(root, info);

        Ui.space(root, 8);
        Button home = Ui.button(this, "🕌 Vakitlere dön");
        home.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
        root.addView(home);

        setContentView(scroll);
    }

    private void buildPrayer(LinearLayout root, PrayerInfo info) {
        LinearLayout header = Ui.card(this, Ui.GREEN);
        header.addView(Ui.text(this, info.name, 26, 0xFFFFFFFF, true));
        header.addView(Ui.text(this, info.summary, 17, 0xFFFFFFFF, false));
        root.addView(header);

        if (info.note != null) {
            LinearLayout note = Ui.card(this, 0xFFFFF8E1);
            note.addView(Ui.text(this, "ℹ️ " + info.note, 15, Ui.TEXT, false));
            root.addView(note);
        }

        int n = 1;
        for (PrayerInfo.Part part : info.parts) {
            LinearLayout card = Ui.card(this, 0xFFFFFFFF);
            card.addView(Ui.text(this, (n++) + ") " + part.title, 20, Ui.GREEN, true));
            Ui.space(card, 4);
            card.addView(Ui.text(this, "Niyet:", 14, Ui.GREY, true));
            card.addView(Ui.text(this, part.niyet, 15, Ui.TEXT, false));
            Ui.space(card, 6);
            card.addView(Ui.text(this, "Kılınışı:", 14, Ui.GREY, true));
            card.addView(Ui.text(this, part.steps, 15, Ui.TEXT, false));
            root.addView(card);
        }

        if (info.parts.length > 0) {
            Button guide = Ui.button(this, "📖 Okunan dualar ve temel hareketler");
            guide.setOnClickListener(v -> {
                Intent i = new Intent(this, PrayerDetailActivity.class);
                i.putExtra(EXTRA_KEY, "rehber");
                startActivity(i);
            });
            root.addView(guide);
        }
    }

    private void buildGuide(LinearLayout root) {
        LinearLayout header = Ui.card(this, Ui.GREEN);
        header.addView(Ui.text(this, "Namaz Nasıl Kılınır?", 26, 0xFFFFFFFF, true));
        header.addView(Ui.text(this, "Her rekâtta yapılanlar ve okunan dualar (Hanefî mezhebi)", 15, 0xFFFFFFFF, false));
        root.addView(header);

        LinearLayout rekat = Ui.card(this, Ui.GREEN_LIGHT);
        rekat.addView(Ui.text(this, "Vakitlere göre rekât sayıları", 18, Ui.GREEN, true));
        rekat.addView(Ui.text(this,
                "Sabah: 2 sünnet + 2 farz\n"
              + "Öğle: 4 sünnet + 4 farz + 2 sünnet\n"
              + "İkindi: 4 sünnet + 4 farz\n"
              + "Akşam: 3 farz + 2 sünnet\n"
              + "Yatsı: 4 sünnet + 4 farz + 2 sünnet + 3 vitir\n"
              + "Cuma: 4 sünnet + 2 farz + 4 sünnet", 15, Ui.TEXT, false));
        root.addView(rekat);

        root.addView(Ui.text(this, "Bir rekâtın adımları", 20, Ui.GREEN, true));
        int n = 1;
        for (String[] s : PrayerInfo.TEMEL_ADIMLAR) {
            LinearLayout card = Ui.card(this, 0xFFFFFFFF);
            card.addView(Ui.text(this, (n++) + ". " + s[0], 17, Ui.TEXT, true));
            card.addView(Ui.text(this, s[1], 15, Ui.TEXT, false));
            root.addView(card);
        }

        Ui.space(root, 8);
        root.addView(Ui.text(this, "Namazda okunan dualar", 20, Ui.GREEN, true));
        for (String[] s : PrayerInfo.DUALAR) {
            LinearLayout card = Ui.card(this, 0xFFFFFFFF);
            card.addView(Ui.text(this, s[0], 17, Ui.TEXT, true));
            card.addView(Ui.text(this, s[1], 15, Ui.TEXT, false));
            root.addView(card);
        }
    }
}
