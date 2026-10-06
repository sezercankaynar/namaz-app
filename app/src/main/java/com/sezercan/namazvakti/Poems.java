package com.sezercan.namazvakti;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Günün mısrası (assets/siirler.json). Yalnızca telif süresi dolmuş şairler;
 * yabancı şiirlerin Türkçeleri bu uygulama için yapılmıştır, altında asıl hâli durur.
 */
public final class Poems {
    private Poems() {}

    public static final class Poem {
        public final String verse, poet, info, original, meaning;
        Poem(JSONObject o) {
            verse = o.optString("misra");
            poet = o.optString("sair");
            info = o.optString("bilgi");
            original = o.has("ozgun") ? o.optString("ozgun") : null;
            meaning = o.has("anlam") ? o.optString("anlam") : null;
        }
    }

    private static Poem[] all;

    public static synchronized Poem[] all(Context c) {
        if (all == null) {
            try {
                JSONArray a = new JSONArray(Districts.Assets.read(c, "siirler.json"));
                Poem[] r = new Poem[a.length()];
                for (int i = 0; i < a.length(); i++) r[i] = new Poem(a.getJSONObject(i));
                all = r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return all;
    }

    /** Takvim gününe göre sırayla seçilir; aynı gün herkes aynı mısrayı görür. */
    public static Poem forDay(Context c, Calendar day) {
        Poem[] p = all(c);
        long local = day.getTimeInMillis() + day.getTimeZone().getOffset(day.getTimeInMillis());
        long epochDay = Math.floorDiv(local, 86_400_000L);
        return p[(int) Math.floorMod(epochDay, (long) p.length)];
    }
}
