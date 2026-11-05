# Tasarım belgesi

Bu belge sistemin nesne modelini dört diyagramla anlatır. Diyagramlar [Mermaid](https://mermaid.js.org)
ile yazılmıştır; GitHub bu dosyayı açtığınızda çizer.

1. [Alan modeli: sınıf diyagramı](#1-alan-modeli-sınıf-diyagramı)
2. [Randevu kuralları: strateji deseni](#2-randevu-kuralları-strateji-deseni)
3. [Randevu durumları: durum diyagramı](#3-randevu-durumları-durum-diyagramı)
4. [Randevu alma: sıra diyagramı](#4-randevu-alma-sıra-diyagramı)
5. [Katmanlar](#5-katmanlar)

---

## 1. Alan modeli: sınıf diyagramı

Tüm varlıklar kimliği ve eşitlik kuralını `TemelVarlik` soyut sınıfından alır. Kullanıcı ağacında
yetki soruları (`randevuyuYonetebilirMi`, `kurumTanimlayabilirMi`...) ve arayüzün ihtiyaç duyduğu
bilgiler (`getHesapMenusu`, `getProfilBilgileri`...) üst sınıfta tanımlanır, her alt sınıf kendine göre ezer.

```mermaid
classDiagram
    direction TB

    class TemelVarlik {
        <<abstract>>
        -Long id
        +getId() Long
        +yeniMi() bool
        +equals(Object) bool
        +hashCode() int
        +toString() String
    }

    class Kullanici {
        <<abstract>>
        -String girisKimligi
        -String sifre
        -String adSoyad
        -boolean aktif
        +getRol()* Rol
        +getPanelYolu()* String
        +getGorunenAd() String
        +randevuAlabilirMi() bool
        +randevuyuYonetebilirMi(Randevu) bool
        +randevuyuIptalEdebilirMi(Randevu) bool
        +kurumuYonetebilirMi(Kurum) bool
        +kurumTanimlayabilirMi() bool
        +getHesapMenusu() List~MenuBaglantisi~
        +getCalismaSekmeleri() List~MenuBaglantisi~
        +getProfilBilgileri() List~ProfilBilgisi~
        +calismaSekmesiMi(String) bool
    }
    class Hasta {
        -LocalDate dogumTarihi
        +getMaskeliTcKimlikNo() String
    }
    class Calisan {
        <<abstract>>
        +getUnvan()* String
        +unvaniGuncelle(String)*
        +hizmetVerebilirMi(Hizmet) bool
        +musaitMi(bas, bit) bool
        +uygunlukEkle(gun, bas, bit) UygunlukDilimi
    }
    class Doktor {
        -String uzmanlikAlani
    }
    class SaglikPersoneli {
        -String gorev
    }
    class Yonetici
    class SistemYoneticisi

    TemelVarlik <|-- Kullanici
    Kullanici <|-- Hasta
    Kullanici <|-- Calisan
    Kullanici <|-- Yonetici
    Kullanici <|-- SistemYoneticisi
    Calisan <|-- Doktor
    Calisan <|-- SaglikPersoneli

    class Kurum {
        -String ad
        -KurumTuru tur
        -boolean aktif
        +subeEkle(ad, adres, tel) Sube
        +subeKaldir(Sube)
        +hizmetEkle(...) Hizmet
        +bilgileriGuncelle(ad, tur, aciklama)
        +yayindanKaldir()
    }
    class Sube {
        -String ad
        -String adres
        +acikMi(bas, bit) bool
        +calismaSaatiAyarla(gun, acilis, kapanis)
        +bolumEkle(ad, aciklama) Bolum
        +bolumGuncelle(Bolum, ad, aciklama)
    }
    class Bolum
    class CalismaSaati {
        <<embeddable>>
        -DayOfWeek gun
        -LocalTime acilis
        -LocalTime kapanis
        +kapsar(bas, bit) bool
    }
    class Hizmet {
        -String ad
        -int sureDakika
        -BigDecimal ucret
        -boolean aktif
        +tanimiGuncelle(ad, tur, aciklama)
    }
    class UygunlukDilimi {
        -DayOfWeek gun
        -LocalTime baslangic
        -LocalTime bitis
        +kapsar(bas, bit) bool
        +cakisir(UygunlukDilimi) bool
    }

    TemelVarlik <|-- Kurum
    TemelVarlik <|-- Sube
    TemelVarlik <|-- Bolum
    TemelVarlik <|-- Hizmet
    TemelVarlik <|-- UygunlukDilimi

    Kurum "1" *-- "1..*" Sube : şubeleri
    Kurum "1" *-- "*" Hizmet : hizmetleri
    Sube "1" *-- "*" Bolum
    Sube "1" *-- "*" CalismaSaati
    Calisan "*" --> "1" Sube : görev yeri
    Calisan "*" --> "0..1" Bolum
    Calisan "*" o-- "*" Hizmet : verdiği hizmetler
    Calisan "1" *-- "*" UygunlukDilimi
    Yonetici "*" --> "1" Kurum

    class Randevu {
        -LocalDateTime baslangic
        -LocalDateTime bitis
        -int sureDakika
        -BigDecimal ucret
        -RandevuDurumu durum
        -Long surum
        +olustur(...)$ Randevu
        +onayla(Kullanici)
        +onayla(Kullanici, LocalDateTime)
        +reddet(Kullanici, String)
        +iptalEt(Kullanici, String)
        +tamamla(Kullanici)
        +cakisir(bas, bit) bool
    }
    class RandevuHareketi {
        -RandevuDurumu durum
        -LocalDateTime zaman
        -String yapan
        -String aciklama
    }
    class RandevuDurumu {
        <<enumeration>>
        BEKLEMEDE
        ONAYLANDI
        REDDEDILDI
        IPTAL
        TAMAMLANDI
        GELMEDI
        ZAMAN_ASIMI
        +gecebilir(RandevuDurumu) bool
    }

    TemelVarlik <|-- Randevu
    TemelVarlik <|-- RandevuHareketi
    Randevu --> Hasta
    Randevu --> Calisan
    Randevu --> Hizmet
    Randevu --> Sube
    Randevu --> RandevuDurumu
    Randevu "1" *-- "*" RandevuHareketi : geçmiş
```

**Okuma notları**

- `*--` (bileşim): parça bütün olmadan yaşayamaz. Şube, kurum silinince silinir; yapıcısı paket erişimlidir,
  yalnızca `kurum.subeEkle(...)` ile oluşturulur.
- `o--` (birleştirme): çalışan ile hizmet birbirinden bağımsız yaşar; çalışan pasifleşse de hizmet kalır.
- Randevu, ücret ve süreyi oluşturulduğu anki değerleriyle **kopyalar**. Hizmetin ücreti sonradan değişse de
  eski randevu etkilenmez.
- `onayla(Kullanici)` ve `onayla(Kullanici, LocalDateTime)` **aşırı yüklemedir** (derleme zamanı çok biçimliliği).
  `getUnvan()` gibi ezilen metotlar ise çalışma zamanı çok biçimliliğidir.

---

## 2. Randevu kuralları: strateji deseni

Randevu kaydedilmeden önce yedi kural sırayla çalışır. `RandevuServisi` kuralların hangileri olduğunu bilmez:
Spring, `RandevuKurali` arayüzünü uygulayan tüm sınıfları `@Order` sırasıyla bir liste olarak verir.
Yeni bir kural eklemek için mevcut koda dokunmadan yeni bir sınıf yazmak yeter (Açık/Kapalı ilkesi).

Kuralların bir kısmı "o saatte başka randevu var mı?" diye sorar. Bu soruya iki farklı strateji cevap verir:
randevu **kaydedilirken** veritabanına bakan `VeritabaniDolulukBilgisi`, **takvim çizilirken** iki haftalık
randevuları tek sorguda yükleyip bellekte cevaplayan `TakvimDolulukBilgisi`. Böylece ekranda gösterilen her boş
saat, kayıtta çalışan kuralların aynısından geçmiş olur.

```mermaid
classDiagram
    direction LR
    class RandevuServisi {
        -List~RandevuKurali~ kurallar
        +randevuOlustur(hasta, calisanId, hizmetId, zaman, not) Randevu
        +musaitlikTakvimi(hasta, calisan, hizmet, ilkGun, gun) List
    }
    class RandevuKurali {
        <<interface>>
        +dogrula(RandevuTalebi, DolulukBilgisi)
    }
    class DolulukBilgisi {
        <<interface>>
        +calisanDoluMu(id, bas, bit) bool
        +hastaDoluMu(id, bas, bit) bool
    }
    class RandevuTalebi {
        <<record>>
        hasta
        calisan
        hizmet
        baslangic
        bitis
    }

    RandevuServisi --> "7" RandevuKurali
    RandevuServisi ..> DolulukBilgisi
    RandevuKurali ..> RandevuTalebi
    RandevuKurali ..> DolulukBilgisi

    RandevuKurali <|.. KurumYayindaKurali : 0
    RandevuKurali <|.. GelecekZamanKurali : 1
    RandevuKurali <|.. HizmetYetkinlikKurali : 2
    RandevuKurali <|.. CalismaSaatiKurali : 3
    RandevuKurali <|.. UygunlukKurali : 4
    RandevuKurali <|.. CalisanCakismaKurali : 5
    RandevuKurali <|.. HastaCakismaKurali : 6

    DolulukBilgisi <|.. VeritabaniDolulukBilgisi
    DolulukBilgisi <|.. TakvimDolulukBilgisi
```

| Sıra | Kural | Ne zaman reddeder |
|---|---|---|
| 0 | `KurumYayindaKurali` | Kurum yayından kaldırılmışsa |
| 1 | `GelecekZamanKurali` | Saat geçmişte ise |
| 2 | `HizmetYetkinlikKurali` | Hizmet randevuya kapalıysa ya da çalışan bu hizmeti vermiyorsa |
| 3 | `CalismaSaatiKurali` | Şube o saatte kapalıysa |
| 4 | `UygunlukKurali` | Saat çalışanın uygunluk dilimlerinin dışındaysa |
| 5 | `CalisanCakismaKurali` | Çalışanın o saatte başka etkin randevusu varsa |
| 6 | `HastaCakismaKurali` | Hastanın o saatte başka etkin randevusu varsa |

---

## 3. Randevu durumları: durum diyagramı

Geçişleri `RandevuDurumu.gecebilir(...)` tanımlar, `Randevu` sınıfının metotları uygular. İzin verilmeyen
bir geçiş (ör. reddedilmiş randevuyu onaylamak) nesnenin kendisi tarafından engellenir. Her geçiş kim yaptı,
ne zaman ve neden bilgisiyle bir `RandevuHareketi` olarak saklanır.

```mermaid
stateDiagram-v2
    [*] --> BEKLEMEDE : hasta talep oluşturur
    BEKLEMEDE --> ONAYLANDI : doktor / yönetici onaylar
    BEKLEMEDE --> REDDEDILDI : doktor / yönetici reddeder
    BEKLEMEDE --> IPTAL : hasta ya da personel iptal eder
    BEKLEMEDE --> ZAMAN_ASIMI : saat geldi, hâlâ onay yok (sistem)
    ONAYLANDI --> TAMAMLANDI : personel işaretler ya da bitişten 2 saat sonra sistem
    ONAYLANDI --> GELMEDI : saat geçtikten sonra personel işaretler
    ONAYLANDI --> IPTAL : hasta (başlamadan) ya da personel iptal eder
    REDDEDILDI --> [*]
    IPTAL --> [*]
    TAMAMLANDI --> [*]
    GELMEDI --> [*]
    ZAMAN_ASIMI --> [*]
```

Yalnızca `BEKLEMEDE` ve `ONAYLANDI` durumları takvimde yer kaplar ve çakışma kontrolüne girer
(`RandevuDurumu.isAktif()`). Reddedilen ya da iptal edilen randevunun saati hemen başka hastaya açılır.

---

## 4. Randevu alma: sıra diyagramı

Aynı anda iki hasta aynı saati seçerse yalnızca biri kabul edilir: kurallar çalışmadan önce çalışanın ve
hastanın satırı kilitlenir. Kilit sırası hep aynı olduğundan iki işlem birbirini bekleyip kilitlenmez.

```mermaid
sequenceDiagram
    actor Hasta
    participant HC as HastaController
    participant RS as RandevuServisi
    participant Depo as Depolar (JPA)
    participant K as RandevuKurali (7 adet)
    participant R as Randevu

    Hasta->>HC: Kurum, şube, hizmet ve doktor seçer
    HC->>RS: musaitlikTakvimi(...)
    RS->>Depo: 14 günlük randevular (tek sorgu)
    RS->>K: her aday saat için dogrula(talep, TakvimDolulukBilgisi)
    RS-->>HC: yalnızca geçerli saatler
    HC-->>Hasta: Boş saatler kutucuk olarak

    Hasta->>HC: Saate dokunur, özeti onaylar (POST)
    HC->>RS: randevuOlustur(hasta, calisanId, hizmetId, zaman, not)
    RS->>Depo: çalışanı kilitle (SELECT ... FOR UPDATE)
    RS->>Depo: hastayı kilitle
    RS->>K: dogrula(talep, VeritabaniDolulukBilgisi)
    alt kural ihlali
        K-->>RS: IsKuraliIhlali
        RS-->>HC: hata mesajı
        HC-->>Hasta: Seçim ekranına dönüş ve açıklama
    else tüm kurallar geçti
        RS->>R: Randevu.olustur(...) [BEKLEMEDE]
        RS->>Depo: save(randevu)
        HC-->>Hasta: Randevularım: "Onay bekliyor"
    end

    Note over Hasta,R: Doktor ya da kurum yöneticisi onayladığında randevu.onayla(yapan) ONAYLANDI'ya geçer
```

---

## 5. Katmanlar

```mermaid
flowchart LR
    T[Tarayıcı] -->|HTTP| G[guvenlik<br/>Spring Security, giriş kilidi]
    G --> W[web<br/>Controller, form, hata yakalayıcı]
    W -->|Thymeleaf| V[templates<br/>sayfalar]
    W --> S[servis<br/>iş akışı, yetki, kurallar]
    S --> M[model<br/>varlıklar ve iş kuralları]
    S --> D[repository<br/>Spring Data JPA]
    D --> DB[(H2)]
```

- **model** yalnızca kendi kurallarını bilir; veritabanına ya da web'e bağımlı değildir (birim testleri veritabanı
  olmadan çalışır: `AlanModeliTest`, `NdpIlkeleriTest`).
- **servis** işlemi yapan kullanıcıyı parametre olarak alır ve yetkiyi o nesneye sorar.
- **web** iş kuralı içermez; formu servise iletir, sonucu sayfaya taşır.
