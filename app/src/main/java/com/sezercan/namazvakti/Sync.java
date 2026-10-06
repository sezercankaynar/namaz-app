package com.sezercan.namazvakti;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Arkadaşla haftalık namaz takibini paylaşır. Eşleşen herkes aynı ntfy.sh konusuna
 * kendi son 7 gününün özetini gönderir ve diğerlerininkini okur. ntfy.sh mesajları
 * yaklaşık 12 saat saklar; bu yüzden her telefon özetini sık sık yeniden gönderir ve
 * arkadaşlardan gelen son özeti kendinde saklar.
 */
public final class Sync {
    private Sync() {}

    private static final String SERVER = "https://ntfy.sh/";
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int CODE_LEN = 8;
    /** Uygulama açılışında en fazla bu sıklıkla yeniden gönderilir. */
    private static final long REPUBLISH_MS = 5 * 60 * 1000L;

    public static final class Friend {
        public final String id, name;
        public final long updated;
        private final JSONObject days;
        Friend(String id, JSONObject o) {
            this.id = id;
            name = o.optString("ad", "Arkadaş");
            updated = o.optLong("t", 0);
            days = o.optJSONObject("g") != null ? o.optJSONObject("g") : new JSONObject();
        }
        public int mask(String dayKey) {
            return days.optInt(dayKey, 0);
        }
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("takip", Context.MODE_PRIVATE);
    }

    // ---- Eşleşme ----

    public static String deviceId(Context c) {
        String id = sp(c).getString("cihaz", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sp(c).edit().putString("cihaz", id).apply();
        }
        return id;
    }

    public static String code(Context c) {
        return sp(c).getString("kod", null);
    }

    public static boolean paired(Context c) {
        return code(c) != null;
    }

    public static String myName(Context c) {
        return sp(c).getString("ad", "");
    }

    public static void setMyName(Context c, String name) {
        sp(c).edit().putString("ad", name.trim()).apply();
    }

    public static String newCode() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LEN; i++) sb.append(CODE_CHARS.charAt(r.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }

    /** Kullanıcının yazdığı kodu sadeleştirir (boşluk/tire atılır, büyük harf); geçersizse null. */
    public static String normalize(String input) {
        StringBuilder sb = new StringBuilder();
        for (char ch : input.toUpperCase(java.util.Locale.US).toCharArray()) {
            if (CODE_CHARS.indexOf(ch) >= 0) sb.append(ch);
            else if (Character.isLetterOrDigit(ch)) return null;
        }
        return sb.length() == CODE_LEN ? sb.toString() : null;
    }

    public static String pretty(String code) {
        return code.substring(0, 4) + "-" + code.substring(4);
    }

    public static void pair(Context c, String code) {
        sp(c).edit().putString("kod", code).remove("arkadaslar").putLong("son_gonderim", 0).apply();
    }

    public static void unpair(Context c) {
        sp(c).edit().remove("kod").remove("arkadaslar").remove("son_okuma").apply();
    }

    static String topic(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(("namazvakti|" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("nv-");
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- Veri ----

    /** Kendi son 7 günümüzün özeti. */
    static String snapshot(Context c) {
        try {
            JSONObject days = new JSONObject();
            for (String d : Habits.lastDays()) days.put(d, Habits.mask(c, d));
            JSONObject o = new JSONObject();
            o.put("v", 1);
            o.put("id", deviceId(c));
            o.put("ad", myName(c).isEmpty() ? "Arkadaşın" : myName(c));
            o.put("t", System.currentTimeMillis());
            o.put("g", days);
            return o.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<Friend> friends(Context c) {
        List<Friend> out = new ArrayList<>();
        try {
            JSONObject all = new JSONObject(sp(c).getString("arkadaslar", "{}"));
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String id = it.next();
                out.add(new Friend(id, all.getJSONObject(id)));
            }
        } catch (Exception ignored) {}
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    public static long lastRead(Context c) {
        return sp(c).getLong("son_okuma", 0);
    }

    /**
     * ntfy'den gelen satırları (her satır bir JSON olayı) işler, kendi kaydımız dışındaki
     * en yeni özetleri saklar. Kaç arkadaşın güncellendiğini döndürür.
     */
    static int mergeLines(Context c, List<String> lines) {
        String me = deviceId(c);
        try {
            JSONObject all = new JSONObject(sp(c).getString("arkadaslar", "{}"));
            int changed = 0;
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                JSONObject ev = new JSONObject(line);
                if (!"message".equals(ev.optString("event"))) continue;
                JSONObject snap;
                try { snap = new JSONObject(ev.optString("message")); } catch (Exception e) { continue; }
                String id = snap.optString("id", "");
                if (id.isEmpty() || id.equals(me) || snap.optJSONObject("g") == null) continue;
                JSONObject old = all.optJSONObject(id);
                if (old == null || old.optLong("t") < snap.optLong("t")) {
                    snap.remove("id");
                    all.put(id, snap);
                    changed++;
                }
            }
            sp(c).edit().putString("arkadaslar", all.toString()).apply();
            return changed;
        } catch (Exception e) {
            return 0;
        }
    }

    // ---- Ağ (arka planda çağrılmalı) ----

    public static boolean publish(Context c) {
        String code = code(c);
        if (code == null) return false;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(SERVER + topic(code)).openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            try (OutputStream out = con.getOutputStream()) {
                out.write(snapshot(c).getBytes(StandardCharsets.UTF_8));
            }
            boolean ok = con.getResponseCode() == 200;
            if (ok) sp(c).edit().putLong("son_gonderim", System.currentTimeMillis()).apply();
            return ok;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    public static boolean fetch(Context c) {
        String code = code(c);
        if (code == null) return false;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(SERVER + topic(code) + "/json?poll=1&since=12h").openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(20000);
            if (con.getResponseCode() != 200) return false;
            List<String> lines = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) lines.add(line);
            }
            if (!code.equals(code(c))) return false; // bu arada eşleşme değişti
            mergeLines(c, lines);
            sp(c).edit().putLong("son_okuma", System.currentTimeMillis()).apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /** Uygulama açılınca: gerekirse gönder, sonra oku; bitince ana iş parçacığında onDone. */
    public static void syncAsync(Context c, boolean forcePublish, Runnable onDone) {
        if (!paired(c)) return;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            long last = sp(app).getLong("son_gonderim", 0);
            if (forcePublish || System.currentTimeMillis() - last > REPUBLISH_MS) publish(app);
            fetch(app);
            if (onDone != null) new Handler(Looper.getMainLooper()).post(onDone);
        }, "takip").start();
    }

    public static void publishAsync(Context c) {
        if (!paired(c)) return;
        final Context app = c.getApplicationContext();
        new Thread(() -> publish(app), "takip").start();
    }
}
