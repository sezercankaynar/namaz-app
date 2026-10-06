package com.sezercan.namazvakti;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Dualar ve sûreler (assets/metinler.json): kategori → başlık, Arapça, okunuş, anlam. */
public final class Texts {
    private Texts() {}

    public static final class Item {
        public final String title, arabic, reading, meaning, note;
        Item(JSONObject o) {
            title = o.optString("baslik");
            arabic = o.optString("arapca");
            reading = o.optString("okunus");
            meaning = o.optString("anlam");
            note = o.has("not") ? o.optString("not") : null;
        }
    }

    public static final class Category {
        public final String name, description;
        /** Kategori rengi (colors.xml'deki cat_* değerleri, sırasıyla). */
        public final int colorRes;
        public final Item[] items;
        Category(JSONObject o, int colorRes) throws org.json.JSONException {
            name = o.getString("kategori");
            description = o.optString("aciklama");
            this.colorRes = colorRes;
            JSONArray a = o.getJSONArray("ogeler");
            items = new Item[a.length()];
            for (int i = 0; i < a.length(); i++) items[i] = new Item(a.getJSONObject(i));
        }
    }

    private static final int[] COLORS = {
            R.color.cat_dualar, R.color.cat_fatiha, R.color.cat_kisa_sureler, R.color.cat_ayetler, R.color.cat_tesbihat
    };

    private static Category[] all;

    public static synchronized Category[] all(Context c) {
        if (all == null) {
            try {
                JSONArray a = new JSONArray(Districts.Assets.read(c, "metinler.json"));
                Category[] r = new Category[a.length()];
                for (int i = 0; i < a.length(); i++) r[i] = new Category(a.getJSONObject(i), COLORS[i % COLORS.length]);
                all = r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return all;
    }
}
