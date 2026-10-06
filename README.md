# Namaz Vakti (Android)

Namaz vakitlerinde alarm gibi uyarı veren, bildirime dokununca o vaktin
kaç rekât olduğunu ve kısaca nasıl kılındığını gösteren basit bir uygulama.

- Vakitler Diyanet'in ilçe bazındaki resmi vakitlerinden alınır (30 gün saklanır);
  internet yoksa Diyanet yöntemiyle telefonda hesaplanır.
- 81 il ve 869 ilçe seçilebilir; saniye saniye geri sayım, hicri tarih.
- Alarm sesi: uygulamanın "Huzur zili", telefonun alarm sesi, telefondan seçilen
  bir ses dosyası (ör. ilahi) ya da sadece titreşim.
- Dualar ve Sûreler: Namaz duaları, Fâtiha, kısa sûreler, âyetler ve tesbihat;
  her biri Arapça yazılış, okunuş ve anlamıyla.
- Pastel renkli arayüz.
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
