package com.sezercan.namazvakti;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 81 ilin ilçeleri ve Diyanet ilçe kimlikleri (assets/ilceler.json). İl merkezi her listenin başında. */
public final class Districts {
    private Districts() {}

    private static String[][] names, ids;

    private static synchronized void load(Context c) {
        if (names != null) return;
        try {
            JSONArray arr = new JSONArray(Assets.read(c, "ilceler.json"));
            names = new String[arr.length()][];
            ids = new String[arr.length()][];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject il = arr.getJSONObject(i);
                JSONArray list = il.getJSONArray("ilceler");
                names[i] = new String[list.length()];
                ids[i] = new String[list.length()];
                for (int j = 0; j < list.length(); j++) {
                    JSONArray it = list.getJSONArray(j);
                    names[i][j] = j == 0 ? it.getString(0) + " (merkez)" : it.getString(0);
                    ids[i][j] = it.getString(1);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String[] names(Context c, int city) {
        load(c);
        return names[city];
    }

    public static String[] ids(Context c, int city) {
        load(c);
        return ids[city];
    }

    public static String nameOf(Context c, int city, String id) {
        String[] i = ids(c, city);
        for (int j = 0; j < i.length; j++) if (i[j].equals(id)) return names[city][j];
        return names[city][0];
    }

    static final class Assets {
        static String read(Context c, String name) throws java.io.IOException {
            try (InputStream in = c.getAssets().open(name)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString(StandardCharsets.UTF_8.name());
            }
        }
    }
}
