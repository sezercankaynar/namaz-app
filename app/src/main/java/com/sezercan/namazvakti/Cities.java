package com.sezercan.namazvakti;

/** Türkiye'nin 81 ili (il merkezi koordinatları). */
public final class Cities {
    private Cities() {}

    public static final String[] NAMES = {
        "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Aksaray", "Amasya", "Ankara", "Antalya",
        "Ardahan", "Artvin", "Aydın", "Balıkesir", "Bartın", "Batman", "Bayburt", "Bilecik",
        "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum",
        "Denizli", "Diyarbakır", "Düzce", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir",
        "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Iğdır", "Isparta", "İstanbul",
        "İzmir", "Kahramanmaraş", "Karabük", "Karaman", "Kars", "Kastamonu", "Kayseri", "Kilis",
        "Kırıkkale", "Kırklareli", "Kırşehir", "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa",
        "Mardin", "Mersin", "Muğla", "Muş", "Nevşehir", "Niğde", "Ordu", "Osmaniye",
        "Rize", "Sakarya", "Samsun", "Şanlıurfa", "Siirt", "Sinop", "Şırnak", "Sivas",
        "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Uşak", "Van", "Yalova", "Yozgat",
        "Zonguldak"
    };

    public static final double[][] COORDS = {
        {37.00, 35.32}, {37.76, 38.28}, {38.76, 30.54}, {39.72, 43.05}, {38.37, 34.03}, {40.65, 35.83}, {39.93, 32.86}, {36.89, 30.71},
        {41.11, 42.70}, {41.18, 41.82}, {37.85, 27.85}, {39.65, 27.88}, {41.64, 32.34}, {37.88, 41.13}, {40.26, 40.23}, {40.14, 29.98},
        {38.88, 40.50}, {38.40, 42.11}, {40.74, 31.61}, {37.72, 30.29}, {40.19, 29.06}, {40.15, 26.41}, {40.60, 33.62}, {40.55, 34.95},
        {37.78, 29.09}, {37.91, 40.23}, {40.84, 31.16}, {41.68, 26.56}, {38.68, 39.22}, {39.75, 39.49}, {39.90, 41.27}, {39.78, 30.52},
        {37.07, 37.38}, {40.91, 38.39}, {40.46, 39.48}, {37.58, 43.74}, {36.20, 36.16}, {39.92, 44.04}, {37.76, 30.55}, {41.01, 28.98},
        {38.42, 27.14}, {37.58, 36.94}, {41.20, 32.62}, {37.18, 33.22}, {40.60, 43.10}, {41.38, 33.78}, {38.73, 35.48}, {36.72, 37.12},
        {39.85, 33.52}, {41.73, 27.22}, {39.15, 34.17}, {40.77, 29.92}, {37.87, 32.48}, {39.42, 29.98}, {38.35, 38.31}, {38.61, 27.43},
        {37.31, 40.74}, {36.81, 34.64}, {37.22, 28.36}, {38.75, 41.51}, {38.62, 34.71}, {37.97, 34.68}, {40.98, 37.88}, {37.07, 36.25},
        {41.02, 40.52}, {40.78, 30.40}, {41.29, 36.33}, {37.16, 38.79}, {37.93, 41.94}, {42.03, 35.15}, {37.52, 42.46}, {39.75, 37.02},
        {40.98, 27.51}, {40.31, 36.55}, {41.00, 39.72}, {39.11, 39.55}, {38.68, 29.41}, {38.49, 43.38}, {40.65, 29.27}, {39.82, 34.81},
        {41.45, 31.79}
    };

    public static final int DEFAULT = 39; // İstanbul
}
