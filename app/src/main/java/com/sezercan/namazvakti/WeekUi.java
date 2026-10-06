package com.sezercan.namazvakti;

import android.content.Context;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Haftalık takip görünümleri: "Kıldım" düğmesi ve 7 günlük nokta tablosu. */
final class WeekUi {
    private WeekUi() {}

    private static final Locale TR = new Locale("tr", "TR");
    private static final String[] DAY_SHORT = {"", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt"};

    interface MaskSource { int mask(String dayKey); }
    interface DayClick { void onDay(String dayKey); }

    /** Vakit satırındaki "Kıldım" düğmesi. Dokunma alanı 48dp, görünen hap 36dp. */
    static TextView pill(Context c, boolean on) {
        TextView t = Ui.text(c, on ? "✓ Kıldım" : "○ Kıldım", on ? R.style.Text_Pill_On : R.style.Text_Pill);
        int inset = Ui.px(c, R.dimen.pill_inset_v);
        t.setBackground(new InsetDrawable(on
                ? Ui.shape(c, R.color.accent, R.dimen.radius_card)
                : Ui.shape(c, R.color.surface, R.dimen.radius_card, R.dimen.stroke_card, R.color.border_input),
                0, inset, 0, inset));
        t.setClickable(true);
        t.setContentDescription(on ? "Kılındı olarak işaretli, kaldırmak için dokunun" : "Kıldım olarak işaretle");
        return t;
    }

    static Calendar parse(String dayKey) {
        Calendar d = Times.today();
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(Times.TR);
            d.setTime(f.parse(dayKey));
        } catch (ParseException ignored) {}
        return d;
    }

    static String longDay(String dayKey) {
        SimpleDateFormat f = new SimpleDateFormat("d MMMM, EEEE", TR);
        f.setTimeZone(Times.TR);
        return f.format(parse(dayKey).getTime());
    }

    /** Bir kişinin bloğu: ad + toplam, altında 7 gün × 5 vakit nokta tablosu. */
    static LinearLayout person(Context c, String name, String sub, MaskSource src, DayClick click) {
        String[] days = Habits.lastDays();
        String today = days[days.length - 1];
        int total = 0;
        for (String d : days) total += Habits.count(src.mask(d));

        LinearLayout block = Ui.vertical(c);
        LinearLayout head = Ui.horizontal(c);
        head.addView(Ui.text(c, name, R.style.Text_TableName), Ui.weight1());
        Ui.addRow(head, Ui.text(c, total + "/" + (Habits.DAYS * Habits.PRAYERS.length), R.style.Text_TableCount),
                Ui.wrap(), R.dimen.col_gap);
        Ui.add(block, head, 0);
        if (sub != null) Ui.add(block, Ui.text(c, sub, R.style.Text_Small), R.dimen.item_gap_title);

        LinearLayout grid = Ui.horizontal(c);
        grid.setGravity(Gravity.TOP);
        for (String d : days) {
            LinearLayout col = Ui.vertical(c);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            int pv = Ui.px(c, R.dimen.week_col_pad_v);
            col.setPadding(0, pv, 0, pv);
            if (d.equals(today)) col.setBackground(Ui.shape(c, R.color.row_selected, R.dimen.radius_spinner));
            TextView label = Ui.text(c, DAY_SHORT[parse(d).get(Calendar.DAY_OF_WEEK)],
                    d.equals(today) ? R.style.Text_SmallBold : R.style.Text_Small);
            label.setGravity(Gravity.CENTER);
            col.addView(label, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            int mask = src.mask(d);
            int size = Ui.px(c, R.dimen.week_dot);
            for (int p : Habits.PRAYERS) {
                View dot = new View(c);
                dot.setBackground(Ui.oval(c, (mask & (1 << p)) != 0 ? R.color.dot_done : R.color.dot_missed));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.topMargin = Ui.px(c, R.dimen.week_dot_gap);
                col.addView(dot, lp);
            }
            if (click != null) {
                col.setClickable(true);
                col.setContentDescription(longDay(d) + ": " + Habits.count(mask) + " vakit kılındı. Düzenlemek için dokunun");
                col.setOnClickListener(v -> click.onDay(d));
            } else {
                col.setContentDescription(longDay(d) + ": " + Habits.count(mask) + " vakit kılındı");
            }
            grid.addView(col, Ui.weight1());
        }
        Ui.add(block, grid, R.dimen.item_gap_detail);
        return block;
    }

    static View divider(Context c) {
        View div = new View(c);
        div.setBackgroundColor(Ui.color(c, R.color.divider));
        div.setLayoutParams(new LinearLayout.LayoutParams(Ui.MATCH, Ui.px(c, R.dimen.divider)));
        return div;
    }

    static String updatedText(long t) {
        if (t <= 0) return null;
        Calendar now = Times.today();
        Calendar then = Times.today();
        then.setTimeInMillis(t);
        SimpleDateFormat f = new SimpleDateFormat(
                Habits.dayKey(now).equals(Habits.dayKey(then)) ? "'bugün' HH:mm" : "d MMMM HH:mm", TR);
        f.setTimeZone(Times.TR);
        return "Son güncelleme: " + f.format(then.getTime());
    }
}
