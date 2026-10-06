package com.sezercan.namazvakti;

import android.content.Context;
import android.graphics.Color;

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
        public final int color;
        public final Item[] items;
        Category(JSONObject o) throws org.json.JSONException {
            name = o.getString("kategori");
            description = o.optString("aciklama");
            color = Color.parseColor(o.optString("renk", "#E4D9F5"));
            JSONArray a = o.getJSONArray("ogeler");
            items = new Item[a.length()];
            for (int i = 0; i < a.length(); i++) items[i] = new Item(a.getJSONObject(i));
        }
    }

    private static Category[] all;

    public static synchronized Category[] all(Context c) {
        if (all == null) {
            try {
                JSONArray a = new JSONArray(Districts.Assets.read(c, "metinler.json"));
                Category[] r = new Category[a.length()];
                for (int i = 0; i < a.length(); i++) r[i] = new Category(a.getJSONObject(i));
                all = r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return all;
    }
}
