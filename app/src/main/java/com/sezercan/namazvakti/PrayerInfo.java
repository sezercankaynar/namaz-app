package com.sezercan.namazvakti;

/** Her vakit için rekât sayıları ve kısa kılınış tarifi (Hanefî mezhebi, Diyanet). */
public final class PrayerInfo {

    public static final class Part {
        public final String title;   // örn. "2 rekât sünnet"
        public final String niyet;
        public final String steps;
        Part(String title, String niyet, String steps) {
            this.title = title; this.niyet = niyet; this.steps = steps;
        }
    }

    public final String key, name, alarmTitle, summary, note;
    public final Part[] parts;

    private PrayerInfo(String key, String name, String alarmTitle, String summary, String note, Part... parts) {
        this.key = key; this.name = name; this.alarmTitle = alarmTitle;
        this.summary = summary; this.note = note; this.parts = parts;
    }

    public static final String[] VAKIT_NAMES = {"İmsak (Sabah)", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı"};

    // ---- Kılınış tarifleri ----
    private static final String IKI =
            "1. rekât: “Allâhu Ekber” diyerek tekbir al, ellerini bağla. Sübhâneke, Eûzü-Besmele, Fâtiha ve kısa bir sûre oku. Rükû et, iki secde yap.\n"
          + "2. rekât: Ayağa kalk. Besmele, Fâtiha ve kısa bir sûre oku. Rükû, iki secde.\n"
          + "Oturuş: Ettehiyyâtü, Allâhümme salli, Allâhümme bârik ve Rabbenâ dualarını oku. Önce sağa, sonra sola selam ver.";

    private static final String DORT_SUNNET =
            "1. ve 2. rekât: İki rekâtlı namaz gibi (Sübhâneke, Fâtiha, sûre; rükû, iki secde).\n"
          + "2. rekât sonunda otur, yalnız Ettehiyyâtü oku ve kalk.\n"
          + "3. ve 4. rekât: Besmele, Fâtiha ve kısa bir sûre oku; rükû, iki secde.\n"
          + "Son oturuş: Ettehiyyâtü, Salli, Bârik, Rabbenâ; sonra selam.";

    private static final String DORT_SUNNET_GAYRI_MUEKKED =
            "1. ve 2. rekât: İki rekâtlı namaz gibi.\n"
          + "2. rekât sonunda otur; Ettehiyyâtü, Salli ve Bârik dualarını oku, kalk.\n"
          + "3. rekât: Sübhâneke, Eûzü-Besmele, Fâtiha ve sûre oku. 4. rekât: Besmele, Fâtiha ve sûre.\n"
          + "Son oturuş: Ettehiyyâtü, Salli, Bârik, Rabbenâ; sonra selam.";

    private static final String DORT_FARZ =
            "1. ve 2. rekât: İki rekâtlı namaz gibi (Fâtiha + sûre).\n"
          + "2. rekât sonunda otur, yalnız Ettehiyyâtü oku ve kalk.\n"
          + "3. ve 4. rekât: Yalnız Besmele ve Fâtiha oku (sûre okunmaz); rükû, iki secde.\n"
          + "Son oturuş: Ettehiyyâtü, Salli, Bârik, Rabbenâ; sonra selam.";

    private static final String UC_FARZ =
            "1. ve 2. rekât: Fâtiha + kısa sûre; rükû, iki secde.\n"
          + "2. rekât sonunda otur, yalnız Ettehiyyâtü oku ve kalk.\n"
          + "3. rekât: Yalnız Besmele ve Fâtiha oku; rükû, iki secde.\n"
          + "Son oturuş: Ettehiyyâtü, Salli, Bârik, Rabbenâ; sonra selam.";

    private static final String VITIR =
            "1. ve 2. rekât: Fâtiha + kısa sûre; 2. rekât sonunda otur, yalnız Ettehiyyâtü oku ve kalk.\n"
          + "3. rekât: Fâtiha ve sûreden sonra “Allâhu Ekber” deyip ellerini kulak hizasına kaldır, tekrar bağla ve Kunut dualarını oku.\n"
          + "Sonra rükû, iki secde, son oturuş (Ettehiyyâtü, Salli, Bârik, Rabbenâ) ve selam.";

    private static String niyet(String vakit, String kisim) {
        return "“Niyet ettim Allah rızası için bugünkü " + vakit + " namazının " + kisim + " kılmaya.”";
    }

    public static final PrayerInfo SABAH = new PrayerInfo("sabah", "Sabah Namazı",
            "🕌 Sabah namazı vakti girdi",
            "Sabah namazı 4 rekât: 2 sünnet + 2 farz",
            "Sabah namazının vakti güneş doğana kadar sürer.",
            new Part("2 rekât sünnet", niyet("sabah", "sünnetini"), IKI),
            new Part("2 rekât farz", niyet("sabah", "farzını"), IKI));

    public static final PrayerInfo GUNES = new PrayerInfo("gunes", "Güneş Doğuşu",
            "☀️ Güneş doğdu",
            "Sabah namazının vakti çıktı. Güneş doğarken yaklaşık 45 dakika namaz kılınmaz.",
            "Güneş vakti bir namaz vakti değildir; sabah namazının son anını gösterir. "
          + "Güneş doğduktan sonra yaklaşık 45 dakika (kerahat vakti) namaz kılınmaz.");

    public static final PrayerInfo OGLE = new PrayerInfo("ogle", "Öğle Namazı",
            "🕌 Öğle namazı vakti girdi",
            "Öğle namazı 10 rekât: 4 ilk sünnet + 4 farz + 2 son sünnet",
            null,
            new Part("4 rekât ilk sünnet", niyet("öğle", "ilk sünnetini"), DORT_SUNNET),
            new Part("4 rekât farz", niyet("öğle", "farzını"), DORT_FARZ),
            new Part("2 rekât son sünnet", niyet("öğle", "son sünnetini"), IKI));

    public static final PrayerInfo CUMA = new PrayerInfo("cuma", "Cuma Namazı",
            "🕌 Bugün Cuma — öğle vakti girdi",
            "Cuma namazı 10 rekât: 4 ilk sünnet + 2 farz (cemaatle) + 4 son sünnet",
            "Cuma namazının farzı camide cemaatle, imamın arkasında kılınır. "
          + "Cuma kılamayanlar (yolcu, hasta, kadınlar) normal öğle namazını kılar.",
            new Part("4 rekât ilk sünnet", niyet("cuma", "ilk sünnetini"), DORT_SUNNET),
            new Part("2 rekât farz (cemaatle)", "“Niyet ettim Allah rızası için bugünkü cuma namazının farzını kılmaya, uydum hazır olan imama.”",
                    "İmamla birlikte tekbir al, Sübhâneke'yi oku ve sus; imam sesli okurken dinle. "
                  + "Rükû ve secdeleri imamla birlikte yap. Oturuşta Ettehiyyâtü, Salli, Bârik ve Rabbenâ'yı oku, imamla selam ver."),
            new Part("4 rekât son sünnet", niyet("cuma", "son sünnetini"), DORT_SUNNET));

    public static final PrayerInfo IKINDI = new PrayerInfo("ikindi", "İkindi Namazı",
            "🕌 İkindi namazı vakti girdi",
            "İkindi namazı 8 rekât: 4 sünnet + 4 farz",
            null,
            new Part("4 rekât sünnet", niyet("ikindi", "sünnetini"), DORT_SUNNET_GAYRI_MUEKKED),
            new Part("4 rekât farz", niyet("ikindi", "farzını"), DORT_FARZ));

    public static final PrayerInfo AKSAM = new PrayerInfo("aksam", "Akşam Namazı",
            "🕌 Akşam namazı vakti girdi",
            "Akşam namazı 5 rekât: 3 farz + 2 sünnet",
            "Akşam namazında önce farz, sonra sünnet kılınır.",
            new Part("3 rekât farz", niyet("akşam", "farzını"), UC_FARZ),
            new Part("2 rekât sünnet", niyet("akşam", "sünnetini"), IKI));

    public static final PrayerInfo YATSI = new PrayerInfo("yatsi", "Yatsı Namazı",
            "🕌 Yatsı namazı vakti girdi",
            "Yatsı namazı 13 rekât: 4 ilk sünnet + 4 farz + 2 son sünnet + 3 vitir",
            "Vitir namazı vaciptir; yatsıdan sonra, imsak vaktine kadar kılınabilir.",
            new Part("4 rekât ilk sünnet", niyet("yatsı", "ilk sünnetini"), DORT_SUNNET_GAYRI_MUEKKED),
            new Part("4 rekât farz", niyet("yatsı", "farzını"), DORT_FARZ),
            new Part("2 rekât son sünnet", niyet("yatsı", "son sünnetini"), IKI),
            new Part("3 rekât vitir (vacip)", "“Niyet ettim Allah rızası için vitir namazını kılmaya.”", VITIR));

    public static PrayerInfo forPrayer(int prayer, boolean friday) {
        switch (prayer) {
            case PrayerTimes.IMSAK: return SABAH;
            case PrayerTimes.GUNES: return GUNES;
            case PrayerTimes.OGLE: return friday ? CUMA : OGLE;
            case PrayerTimes.IKINDI: return IKINDI;
            case PrayerTimes.AKSAM: return AKSAM;
            default: return YATSI;
        }
    }

    public static PrayerInfo byKey(String key) {
        for (PrayerInfo p : new PrayerInfo[]{SABAH, GUNES, OGLE, CUMA, IKINDI, AKSAM, YATSI}) {
            if (p.key.equals(key)) return p;
        }
        return null;
    }

    // ---- Genel rehber ve dualar ----
    public static final String[][] TEMEL_ADIMLAR = {
        {"Niyet", "Hangi namazı kılacağını kalbinden geçir (dille söylemek de güzeldir)."},
        {"İftitah tekbiri", "Kıbleye dön, ellerini kulak hizasına kaldır (kadınlar omuz hizası), “Allâhu Ekber” de. Ellerini göbek altında bağla (kadınlar göğüs üzerinde)."},
        {"Kıyam (ayakta)", "Sübhâneke (yalnız ilk rekâtta), Eûzü-Besmele, Fâtiha ve kısa bir sûre oku."},
        {"Rükû", "“Allâhu Ekber” diyerek eğil, ellerini dizlerine koy. 3 kez “Sübhâne Rabbiye'l-azîm” de. Doğrulurken “Semi'allâhu limen hamideh”, dik durunca “Rabbenâ leke'l-hamd” de."},
        {"Secde", "“Allâhu Ekber” diyerek secdeye var; alın, burun, eller, dizler ve ayak parmakları yere değsin. 3 kez “Sübhâne Rabbiye'l-a'lâ” de. Kısa bir süre otur, ikinci kez secde et."},
        {"Oturuş (ka'de)", "Her iki rekâtın sonunda oturulur. Ara oturuşta Ettehiyyâtü, son oturuşta Ettehiyyâtü, Salli, Bârik ve Rabbenâ okunur."},
        {"Selam", "Başını önce sağa “Esselâmü aleyküm ve rahmetullâh”, sonra sola aynı şekilde çevir. Namaz bitti."},
    };
}
