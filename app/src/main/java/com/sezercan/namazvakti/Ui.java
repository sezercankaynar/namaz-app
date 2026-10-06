package com.sezercan.namazvakti;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Ekranları kod ile kurmak için küçük yardımcılar ve pastel renk paleti. */
final class Ui {
    private Ui() {}

    // Pastel palet
    static final int BG = 0xFFFFF9F5;          // krem
    static final int LAVENDER = 0xFFE4D9F5;    // lavanta
    static final int LILAC = 0xFFCDB8E8;       // leylak (düğmeler)
    static final int PLUM = 0xFF6B5B95;        // koyu leylak (vurgu yazılar)
    static final int TEXT = 0xFF3E3557;
    static final int GREY = 0xFF7A7090;
    static final int WHITE = 0xFFFFFFFF;
    static final int PEACH = 0xFFFFE0CC;
    static final int CREAM_NOTE = 0xFFFFF4DE;

    /** İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı için ayrı pastel tonlar. */
    static final int[] VAKIT_COLORS = {
        0xFFD6EAF8, // gök mavisi
        0xFFFFF1C9, // tereyağı sarısı
        0xFFFFE0CC, // şeftali
        0xFFFDE2E4, // pembe
        0xFFE4D9F5, // lavanta
        0xFFD9DEF7, // çivit pastel
    };

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Açık renk durum çubuğu ve kaydırılabilir sayfa kökü kurar. */
    @SuppressWarnings("deprecation")
    static LinearLayout page(Activity a) {
        a.getWindow().setStatusBarColor(BG);
        a.getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController ic = a.getWindow().getInsetsController();
            if (ic != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                ic.setSystemBarsAppearance(light, light);
            }
        } else {
            a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        ScrollView scroll = new ScrollView(a);
        scroll.setBackgroundColor(BG);
        scroll.setFitsSystemWindows(true);
        LinearLayout root = vertical(a);
        int p = dp(a, 16);
        root.setPadding(p, p, p, p);
        scroll.addView(root);
        a.setContentView(scroll);
        return root;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.15f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static GradientDrawable rounded(int color, float radiusPx, int strokePx, int strokeColor) {
        GradientDrawable g = rounded(color, radiusPx);
        g.setStroke(strokePx, strokeColor);
        return g;
    }

    static LinearLayout card(Context c, int color) {
        LinearLayout l = vertical(c);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        l.setBackground(rounded(color, dp(c, 16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        l.setLayoutParams(lp);
        return l;
    }

    static Button button(Context c, String s) {
        return button(c, s, LILAC);
    }

    static Button button(Context c, String s, int color) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(TEXT);
        b.setStateListAnimator(null);
        b.setBackground(rounded(color, dp(c, 14)));
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 54));
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        b.setLayoutParams(lp);
        return b;
    }

    static void space(LinearLayout parent, int dp) {
        View v = new View(parent.getContext());
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(parent.getContext(), dp)));
    }
}
