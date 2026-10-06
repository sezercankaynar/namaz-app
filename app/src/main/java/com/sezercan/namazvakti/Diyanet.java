package com.sezercan.namazvakti;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Diyanet İşleri Başkanlığı'nın resmi vakitlerini (ilçe bazında, 30 gün) internetten alır
 * ve telefonda saklar. İnternet yoksa saklanan vakitler kullanılır.
 */
public final class Diyanet {
    private Diyanet() {}

    private static final String URL_BASE = "https://ezanvakti.emushaf.net/vakitler/";
    private static final String[] KEYS = {"Imsak", "Gunes", "Ogle", "Ikindi", "Aksam", "Yatsi"};

    private static String loadedFor;
    private static JSONObject data;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("diyanet", Context.MODE_PRIVATE);
    }

    private static String dayKey(Calendar day) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        f.setTimeZone(day.getTimeZone());
        return f.format(day.getTime());
    }

    private static synchronized JSONObject data(Context c, String ilce) {
        if (ilce.equals(loadedFor)) return data;
        SharedPreferences sp = sp(c);
        JSONObject d = null;
        if (ilce.equals(sp.getString("ilce", null))) {
            try { d = new JSONObject(sp.getString("veri", "{}")); } catch (Exception ignored) {}
        }
        loadedFor = ilce;
        data = d;
        return d;
    }

    /** O gün için Diyanet vakitleri (epoch ms) veya kayıt yoksa null. */
    public static long[] cached(Context c, String ilce, Calendar day) {
        JSONObject d = data(c, ilce);
        if (d == null) return null;
        JSONArray a = d.optJSONArray(dayKey(day));
        if (a == null || a.length() < 6) return null;
        long[] out = new long[6];
        for (int i = 0; i < 6; i++) {
            String[] hm = a.optString(i).split(":");
            if (hm.length != 2) return null;
            Calendar t = (Calendar) day.clone();
            t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0].trim()));
            t.set(Calendar.MINUTE, Integer.parseInt(hm[1].trim()));
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            out[i] = t.getTimeInMillis();
        }
        return out;
    }

    public static String hijri(Context c, String ilce, Calendar day) {
        JSONObject d = data(c, ilce);
        if (d == null) return null;
        JSONArray a = d.optJSONArray(dayKey(day));
        return (a != null && a.length() > 6) ? a.optString(6, null) : null;
    }

    /** Bugünden itibaren kaç günlük resmi vakit kayıtlı. */
    public static int daysAhead(Context c, String ilce) {
        int n = 0;
        Calendar day = Times.today();
        while (n < 60 && cached(c, ilce, day) != null) {
            n++;
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    /** Kayıt azaldıysa (veya ilçe değiştiyse) yenileme gerekir; başarısız denemeden sonra 1 saat bekler. */
    public static boolean needsRefresh(Context c) {
        String ilce = Prefs.ilce(c);
        if (daysAhead(c, ilce) >= 10) return false;
        long last = sp(c).getLong("son_deneme_" + ilce, 0);
        return System.currentTimeMillis() - last > 60 * 60 * 1000L;
    }

    /** İnternetten indirir ve kaydeder. Ağ kullandığı için arka planda çağrılmalı. */
    public static boolean refresh(Context c) {
        String ilce = Prefs.ilce(c);
        sp(c).edit().putLong("son_deneme_" + ilce, System.currentTimeMillis()).apply();
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(URL_BASE + ilce).openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(20000);
            con.setRequestProperty("Accept", "application/json");
            if (con.getResponseCode() != 200) return false;
            String body;
            try (InputStream in = con.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                body = out.toString(StandardCharsets.UTF_8.name());
            }
            JSONArray arr = new JSONArray(body);
            JSONObject result = new JSONObject();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject day = arr.getJSONObject(i);
                JSONArray v = new JSONArray();
                for (String k : KEYS) v.put(day.getString(k));
                v.put(day.optString("HicriTarihUzun", ""));
                result.put(day.getString("MiladiTarihKisa"), v);
            }
            if (result.length() == 0) return false;
            synchronized (Diyanet.class) {
                sp(c).edit().putString("ilce", ilce).putString("veri", result.toString()).apply();
                loadedFor = ilce;
                data = result;
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /** Arka planda yeniler; bitince (başarılıysa) ana iş parçacığında onDone çalışır. */
    public static void refreshAsync(Context c, Runnable onDone) {
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            boolean ok = refresh(app);
            if (ok) AlarmScheduler.scheduleNext(app);
            if (ok && onDone != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(onDone);
        }, "diyanet").start();
    }
}
