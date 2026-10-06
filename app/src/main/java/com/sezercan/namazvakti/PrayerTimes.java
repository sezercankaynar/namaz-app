package com.sezercan.namazvakti;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Namaz vakitlerini internet gerekmeden astronomik hesapla bulur.
 * Diyanet İşleri Başkanlığı yöntemi: İmsak 18°, Yatsı 17°, İkindi asr-ı evvel
 * ve Diyanet'in temkin (ihtiyat) dakikaları.
 */
public final class PrayerTimes {

    public static final int IMSAK = 0, GUNES = 1, OGLE = 2, IKINDI = 3, AKSAM = 4, YATSI = 5;
    public static final int COUNT = 6;

    private static final double FAJR_ANGLE = 18.0;
    private static final double ISHA_ANGLE = 17.0;
    private static final double SUN_ANGLE = 0.833;
    // Diyanet temkin süreleri (dakika): imsak, güneş, öğle, ikindi, akşam, yatsı
    private static final int[] TEMKIN = {0, -7, 5, 4, 7, 0};

    private PrayerTimes() {}

    /** Verilen gün için vakitleri, epoch milisaniye olarak döndürür. */
    public static long[] forDay(Calendar day, double lat, double lng) {
        int y = day.get(Calendar.YEAR), m = day.get(Calendar.MONTH) + 1, d = day.get(Calendar.DAY_OF_MONTH);
        TimeZone tz = day.getTimeZone();
        Calendar noon = (Calendar) day.clone();
        noon.set(Calendar.HOUR_OF_DAY, 12);
        noon.set(Calendar.MINUTE, 0);
        noon.set(Calendar.SECOND, 0);
        noon.set(Calendar.MILLISECOND, 0);
        double tzHours = tz.getOffset(noon.getTimeInMillis()) / 3600000.0;

        double[] hours = compute(y, m, d, lat, lng, tzHours);

        Calendar midnight = (Calendar) noon.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        long base = midnight.getTimeInMillis();
        long[] out = new long[COUNT];
        for (int i = 0; i < COUNT; i++) {
            double minutes = hours[i] * 60.0 + TEMKIN[i];
            out[i] = base + Math.round(minutes) * 60000L;
        }
        return out;
    }

    /** Yerel saat cinsinden (ondalıklı saat) vakitler. */
    static double[] compute(int y, int m, int d, double lat, double lng, double tz) {
        double jDate = julian(y, m, d) - lng / (15.0 * 24.0);
        double[] t = {5, 6, 12, 13, 18, 18};
        for (int iter = 0; iter < 2; iter++) {
            double[] p = new double[COUNT];
            for (int i = 0; i < COUNT; i++) p[i] = t[i] / 24.0;
            double[] r = new double[COUNT];
            r[IMSAK] = sunAngleTime(jDate, lat, FAJR_ANGLE, p[IMSAK], true);
            r[GUNES] = sunAngleTime(jDate, lat, SUN_ANGLE, p[GUNES], true);
            r[OGLE] = midDay(jDate, p[OGLE]);
            r[IKINDI] = asrTime(jDate, lat, 1, p[IKINDI]);
            r[AKSAM] = sunAngleTime(jDate, lat, SUN_ANGLE, p[AKSAM], false);
            r[YATSI] = sunAngleTime(jDate, lat, ISHA_ANGLE, p[YATSI], false);
            t = r;
        }
        double[] out = new double[COUNT];
        for (int i = 0; i < COUNT; i++) {
            double v = t[i] + tz - lng / 15.0;
            if (Double.isNaN(v)) v = (i == IMSAK) ? out[GUNES] - 1.5 : 21; // aşırı enlem koruması
            out[i] = v;
        }
        if (Double.isNaN(t[IMSAK])) out[IMSAK] = out[GUNES] - 1.5;
        if (Double.isNaN(t[YATSI])) out[YATSI] = out[AKSAM] + 1.5;
        return out;
    }

    private static double julian(int year, int month, int day) {
        if (month <= 2) { year -= 1; month += 12; }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    /** {sapma (derece), zaman denklemi (saat)} */
    private static double[] sunPosition(double jd) {
        double D = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * D);
        double q = fixAngle(280.459 + 0.98564736 * D);
        double L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * D;
        double ra = darctan2(dcos(e) * dsin(L), dcos(L)) / 15.0;
        double eqt = q / 15.0 - fixHour(ra);
        double decl = darcsin(dsin(e) * dsin(L));
        return new double[]{decl, eqt};
    }

    private static double midDay(double jDate, double time) {
        double eqt = sunPosition(jDate + time)[1];
        return fixHour(12 - eqt);
    }

    private static double sunAngleTime(double jDate, double lat, double angle, double time, boolean beforeNoon) {
        double decl = sunPosition(jDate + time)[0];
        double noon = midDay(jDate, time);
        double t = (1.0 / 15.0) * darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat)));
        return noon + (beforeNoon ? -t : t);
    }

    private static double asrTime(double jDate, double lat, double factor, double time) {
        double decl = sunPosition(jDate + time)[0];
        double angle = -darccot(factor + dtan(Math.abs(lat - decl)));
        return sunAngleTime(jDate, lat, angle, time, false);
    }

    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double dtan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double darcsin(double x) { return Math.toDegrees(Math.asin(x)); }
    private static double darccos(double x) { return Math.toDegrees(Math.acos(x)); }
    private static double darctan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    private static double darccot(double x) { return Math.toDegrees(Math.atan(1.0 / x)); }
    private static double fixAngle(double a) { a = a - 360.0 * Math.floor(a / 360.0); return a < 0 ? a + 360 : a; }
    private static double fixHour(double a) { a = a - 24.0 * Math.floor(a / 24.0); return a < 0 ? a + 24 : a; }
}
