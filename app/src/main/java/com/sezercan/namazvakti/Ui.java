package com.sezercan.namazvakti;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Ekranları kod ile kurmak için yardımcılar. Bütün renk, ölçü ve yazı değerleri
 * res/values altındaki colors.xml, dimens.xml ve styles.xml dosyalarından gelir.
 */
final class Ui {
    private Ui() {}

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    /** İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı renkleri. */
    static final int[] VAKIT_COLORS = {
            R.color.imsak, R.color.gunes, R.color.ogle, R.color.ikindi, R.color.aksam, R.color.yatsi
    };

    static int color(Context c, int res) {
        return c.getColor(res);
    }

    static int px(Context c, int dimenRes) {
        return c.getResources().getDimensionPixelSize(dimenRes);
    }

    // ---- Görünümler ----

    static TextView text(Context c, CharSequence s, int style) {
        TextView t = new TextView(c, null, 0, style);
        t.setText(s);
        return t;
    }

    static Button button(Context c, CharSequence s, int style) {
        Button b = new Button(c, null, 0, style);
        b.setText(s);
        return b;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Dolgu renkli, yuvarlak köşeli zemin. */
    static GradientDrawable shape(Context c, int colorRes, int radiusRes) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(c, colorRes));
        g.setCornerRadius(px(c, radiusRes));
        return g;
    }

    static GradientDrawable shape(Context c, int colorRes, int radiusRes, int strokeRes, int strokeColorRes) {
        GradientDrawable g = shape(c, colorRes, radiusRes);
        g.setStroke(px(c, strokeRes), color(c, strokeColorRes));
        return g;
    }

    static GradientDrawable oval(Context c, int colorRes) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color(c, colorRes));
        return g;
    }

    /** Renkli kart (16dp köşe). Beyaz kartlara 1dp kenarlık eklenir. */
    static LinearLayout card(Context c, int colorRes, int padRes) {
        LinearLayout l = vertical(c);
        int p = px(c, padRes);
        l.setPadding(p, p, p, p);
        l.setBackground(colorRes == R.color.surface
                ? shape(c, colorRes, R.dimen.radius_card, R.dimen.stroke_card, R.color.border_card)
                : shape(c, colorRes, R.dimen.radius_card));
        return l;
    }

    /** Numaralı daire (44, 34 veya 26dp). */
    static TextView circle(Context c, String num, int sizeRes, int style, int colorRes) {
        TextView t = text(c, num, style);
        t.setGravity(Gravity.CENTER);
        t.setBackground(oval(c, colorRes));
        int s = px(c, sizeRes);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return t;
    }

    /** Çocuğu ekler; ilk çocuk değilse üstüne verilen boşluğu koyar. */
    static <T extends View> T add(LinearLayout parent, T v, int gapRes) {
        ViewGroup.LayoutParams old = v.getLayoutParams();
        LinearLayout.LayoutParams lp = old instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) old
                : new LinearLayout.LayoutParams(MATCH, WRAP);
        if (parent.getChildCount() > 0 && gapRes != 0) lp.topMargin = px(parent.getContext(), gapRes);
        parent.addView(v, lp);
        return v;
    }

    /** Yatay sıraya ekler; ilk çocuk değilse soluna boşluk koyar. */
    static <T extends View> T addRow(LinearLayout row, T v, LinearLayout.LayoutParams lp, int gapRes) {
        if (row.getChildCount() > 0 && gapRes != 0) lp.setMarginStart(px(row.getContext(), gapRes));
        row.addView(v, lp);
        return v;
    }

    static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, WRAP, 1);
    }

    static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(WRAP, WRAP);
    }

    // ---- Sayfa iskeleti ----

    /**
     * Sayfa kurar: isteğe bağlı üst çubuk (← başlık) ve kaydırılabilir içerik.
     * İçerik kökünü döndürür.
     */
    static LinearLayout page(Activity a, String backTitle, int topPadRes) {
        LinearLayout outer = vertical(a);
        outer.setBackgroundColor(color(a, R.color.bg));
        outer.setFitsSystemWindows(true);

        if (backTitle != null) {
            LinearLayout bar = horizontal(a);
            int h = px(a, R.dimen.screen_h) - (px(a, R.dimen.back_button) - px(a, R.dimen.ts_back)) / 2;
            bar.setPadding(Math.max(0, h), 0, px(a, R.dimen.screen_h), 0);
            Button back = button(a, "←", R.style.Btn_Back);
            back.setContentDescription("Geri");
            back.setOnClickListener(v -> a.finish());
            int b = px(a, R.dimen.back_button);
            bar.addView(back, new LinearLayout.LayoutParams(b, b));
            TextView title = text(a, backTitle, R.style.Text_TopBar);
            title.setOnClickListener(v -> a.finish());
            addRow(bar, title, weight1(), R.dimen.item_gap_title);
            outer.addView(bar, new LinearLayout.LayoutParams(MATCH, px(a, R.dimen.top_bar_height)));
        }

        ScrollView scroll = new ScrollView(a);
        scroll.setClipToPadding(false);
        LinearLayout root = vertical(a);
        int hpad = px(a, R.dimen.screen_h);
        root.setPadding(hpad, px(a, topPadRes), hpad, px(a, R.dimen.main_bottom));
        scroll.addView(root);
        outer.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1));
        a.setContentView(outer);
        return root;
    }

    // ---- Alttan açılan pencere ----

    static final class Sheet {
        final Dialog dialog;
        final LinearLayout body;

        Sheet(Activity a, String title) {
            dialog = new Dialog(a, R.style.SheetDialog);
            FrameLayout scrim = new FrameLayout(a);
            scrim.setBackgroundColor(color(a, R.color.scrim));
            scrim.setFitsSystemWindows(true);
            scrim.setOnClickListener(v -> dialog.dismiss());

            LinearLayout sheet = vertical(a);
            sheet.setClickable(true);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(color(a, R.color.bg));
            float r = px(a, R.dimen.radius_sheet);
            bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            sheet.setBackground(bg);
            int h = px(a, R.dimen.screen_h);
            sheet.setPadding(h, px(a, R.dimen.sheet_pad_top), h, px(a, R.dimen.sheet_pad_bottom));

            View handle = new View(a);
            handle.setBackground(shape(a, R.color.border_input, R.dimen.handle_h));
            LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(px(a, R.dimen.handle_w), px(a, R.dimen.handle_h));
            hl.gravity = Gravity.CENTER_HORIZONTAL;
            sheet.addView(handle, hl);
            add(sheet, text(a, title, R.style.Text_SheetTitle), R.dimen.gap_category);
            body = vertical(a);
            add(sheet, body, R.dimen.gap_category);

            scrim.addView(sheet, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));
            dialog.setContentView(scrim);
            Window w = dialog.getWindow();
            if (w != null) w.setLayout(MATCH, MATCH);
        }

        void show() {
            dialog.show();
        }
    }
}
