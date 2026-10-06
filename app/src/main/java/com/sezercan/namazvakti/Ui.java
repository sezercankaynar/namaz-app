package com.sezercan.namazvakti;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Ekranları kod ile kurmak için küçük yardımcılar. */
final class Ui {
    private Ui() {}

    static final int GREEN = 0xFF1B5E20;
    static final int GREEN_LIGHT = 0xFFE8F5E9;
    static final int TEXT = 0xFF212121;
    static final int GREY = 0xFF616161;

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
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

    static LinearLayout card(Context c, int color) {
        LinearLayout l = vertical(c);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        l.setBackground(rounded(color, dp(c, 12)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        l.setLayoutParams(lp);
        return l;
    }

    static Button button(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(0xFFFFFFFF);
        b.setBackground(rounded(GREEN, dp(c, 10)));
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 52));
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        b.setLayoutParams(lp);
        return b;
    }

    static void space(LinearLayout parent, int dp) {
        TextView t = new TextView(parent.getContext());
        t.setHeight(dp(parent.getContext(), dp));
        parent.addView(t);
    }
}
