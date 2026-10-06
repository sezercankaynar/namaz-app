# Namaz Vakti (Android)

Namaz vakitlerinde alarm gibi uyarı veren, bildirime dokununca o vaktin
kaç rekât olduğunu ve kısaca nasıl kılındığını gösteren basit bir uygulama.

- Vakitler Diyanet yöntemiyle telefonda hesaplanır (internet gerekmez).
- 81 il seçilebilir.
- Her vakit için alarm ayrı ayrı açılıp kapatılabilir (Güneş varsayılan kapalı).
- Cuma günü öğle bildirimi Cuma namazı bilgisini gösterir.
- "Namaz nasıl kılınır? / Dualar" ekranında temel hareketler ve dualar var.

## Kurulum

Hazır APK: [`apk/NamazVakti.apk`](apk/NamazVakti.apk)

1. APK'yı telefona indir, dosyaya dokun.
2. "Bilinmeyen kaynaklardan yüklemeye izin ver" sorulursa izin ver.
3. Uygulamayı aç, bildirim iznine "İzin ver" de, şehrini seç.

## Geliştirici notu

`./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
Her push'ta GitHub Actions da APK üretir.
