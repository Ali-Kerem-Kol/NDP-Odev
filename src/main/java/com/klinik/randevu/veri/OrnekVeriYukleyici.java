package com.klinik.randevu.veri;

import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.icerik.DuyuruKategorisi;
import com.klinik.randevu.model.icerik.SiteAyarlari;
import com.klinik.randevu.model.kullanici.*;
import com.klinik.randevu.model.kurum.*;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Uygulama ilk açıldığında tanıtım için örnek kurum, personel, hasta ve randevuları oluşturur.
 * Çok şubeli bir hastane grubu ve tek şubeli bir diş kliniği bulunur.
 */
@Component
public class OrnekVeriYukleyici implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(OrnekVeriYukleyici.class);
    /** Tüm örnek hesapların ortak şifresi (README'de listelenir). */
    private static final String ORNEK_SIFRE = "Medora2026";
    private static final List<DayOfWeek> HAFTA_ICI = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private final KurumRepository kurumRepository;
    private final KullaniciRepository kullaniciRepository;
    private final RandevuRepository randevuRepository;
    private final DuyuruRepository duyuruRepository;
    private final SiteAyarlariRepository siteAyarlariRepository;
    private final PasswordEncoder sifreleyici;

    public OrnekVeriYukleyici(KurumRepository kurumRepository, KullaniciRepository kullaniciRepository,
                              RandevuRepository randevuRepository, DuyuruRepository duyuruRepository,
                              SiteAyarlariRepository siteAyarlariRepository, PasswordEncoder sifreleyici) {
        this.kurumRepository = kurumRepository;
        this.kullaniciRepository = kullaniciRepository;
        this.randevuRepository = randevuRepository;
        this.duyuruRepository = duyuruRepository;
        this.siteAyarlariRepository = siteAyarlariRepository;
        this.sifreleyici = sifreleyici;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (kurumRepository.count() > 0) {
            return;
        }

        kullaniciRepository.save(new SistemYoneticisi("admin@medora.com.tr", sifre(), "Sistem Yöneticisi"));

        // ---- Çok şubeli hastane ----
        Kurum hastane = new Kurum("Sakarya Sağlık Grubu", KurumTuru.HASTANE,
                "Serdivan ve Adapazarı'nda iki şubesi bulunan özel hastane grubu.");
        Sube serdivan = hastane.subeEkle("Serdivan Şubesi", "Kampüs Cad. No:1 Serdivan/Sakarya", "0264 111 11 11");
        Sube adapazari = hastane.subeEkle("Adapazarı Şubesi", "Çark Cad. No:25 Adapazarı/Sakarya", "0264 222 22 22");
        for (Sube sube : List.of(serdivan, adapazari)) {
            HAFTA_ICI.forEach(gun -> sube.calismaSaatiAyarla(gun, LocalTime.of(8, 30), LocalTime.of(18, 0)));
            sube.calismaSaatiAyarla(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        }
        Bolum dahiliye = serdivan.bolumEkle("Dahiliye", "İç hastalıkları");
        Bolum kardiyoloji = serdivan.bolumEkle("Kardiyoloji", "Kalp ve damar hastalıkları");
        Bolum radyoloji = serdivan.bolumEkle("Radyoloji", "Görüntüleme hizmetleri");
        Bolum laboratuvar = serdivan.bolumEkle("Laboratuvar", "Tahlil ve testler");
        Bolum adapazariDahiliye = adapazari.bolumEkle("Dahiliye", "İç hastalıkları");

        Hizmet genelMuayene = hastane.hizmetEkle("Genel Muayene", HizmetTuru.GENEL_MUAYENE, 20, tl(750), "Dahiliye genel muayenesi");
        Hizmet kardiyolojiMuayenesi = hastane.hizmetEkle("Kardiyoloji Muayenesi", HizmetTuru.UZMAN_MUAYENESI, 30, tl(1200), null);
        Hizmet ekg = hastane.hizmetEkle("EKG", HizmetTuru.UZMAN_MUAYENESI, 15, tl(400), "Elektrokardiyografi");
        Hizmet rontgen = hastane.hizmetEkle("Akciğer Röntgeni", HizmetTuru.RADYOLOJI, 15, tl(500), null);
        Hizmet mr = hastane.hizmetEkle("MR Görüntüleme", HizmetTuru.RADYOLOJI, 45, tl(3500), null);
        Hizmet kanTahlili = hastane.hizmetEkle("Kan Tahlili (Hemogram)", HizmetTuru.LABORATUVAR, 10, tl(300), null);
        kurumRepository.save(hastane);

        // ---- Tek şubeli klinik ----
        Kurum klinik = new Kurum("Gülümse Diş Kliniği", KurumTuru.KLINIK, "Tek şubeli ağız ve diş sağlığı kliniği.");
        Sube klinikMerkez = klinik.subeEkle("Merkez", "Atatürk Bulvarı No:10 Adapazarı/Sakarya", "0264 333 33 33");
        HAFTA_ICI.forEach(gun -> klinikMerkez.calismaSaatiAyarla(gun, LocalTime.of(9, 0), LocalTime.of(19, 0)));
        Bolum disBolumu = klinikMerkez.bolumEkle("Ağız ve Diş Sağlığı", null);
        Hizmet disMuayenesi = klinik.hizmetEkle("Diş Muayenesi", HizmetTuru.DIS_TEDAVISI, 20, tl(600), null);
        Hizmet disTemizligi = klinik.hizmetEkle("Diş Taşı Temizliği", HizmetTuru.DIS_TEDAVISI, 40, tl(1500), null);
        kurumRepository.save(klinik);

        // ---- Yöneticiler ----
        kullaniciRepository.save(new Yonetici("ahmet.yilmaz@sakaryasaglik.com.tr", sifre(), "Ahmet Yılmaz", hastane));
        kullaniciRepository.save(new Yonetici("elif.kaya@gulumsedis.com.tr", sifre(), "Elif Kaya", klinik));

        // ---- Çalışanlar ----
        Doktor ayse = doktor("ayse.demir@sakaryasaglik.com.tr", "Ayşe Demir", "İç Hastalıkları", serdivan, dahiliye, genelMuayene);
        Doktor mehmet = doktor("mehmet.ozturk@sakaryasaglik.com.tr", "Mehmet Öztürk", "Kardiyoloji", serdivan, kardiyoloji, kardiyolojiMuayenesi, ekg);
        Doktor selin = doktor("selin.kurt@sakaryasaglik.com.tr", "Selin Kurt", "İç Hastalıkları", serdivan, dahiliye, genelMuayene);
        Doktor zeynep = doktor("zeynep.arslan@sakaryasaglik.com.tr", "Zeynep Arslan", "İç Hastalıkları", adapazari, adapazariDahiliye, genelMuayene);
        Doktor can = doktor("can.sahin@gulumsedis.com.tr", "Can Şahin", "Ağız ve Diş Sağlığı", klinikMerkez, disBolumu, disMuayenesi, disTemizligi);
        SaglikPersoneli teknisyen = personel("ali.celik@sakaryasaglik.com.tr", "Ali Çelik", "Radyoloji Teknisyeni", serdivan, radyoloji, rontgen, mr);
        SaglikPersoneli laborant = personel("fatma.koc@sakaryasaglik.com.tr", "Fatma Koç", "Laborant", serdivan, laboratuvar, kanTahlili);

        for (Calisan calisan : List.of(ayse, selin, mehmet, zeynep, can, teknisyen, laborant)) {
            HAFTA_ICI.forEach(gun -> {
                calisan.uygunlukEkle(gun, LocalTime.of(9, 0), LocalTime.of(12, 0));
                calisan.uygunlukEkle(gun, LocalTime.of(13, 0), LocalTime.of(17, 0));
            });
        }
        ayse.uygunlukEkle(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        kullaniciRepository.saveAll(List.of(ayse, selin, mehmet, zeynep, can, teknisyen, laborant));

        // ---- Hastalar ----
        Hasta hasta1 = hasta("62601815964", "Deniz Aydın", LocalDate.of(1995, 4, 12), "deniz.aydin@ornekposta.com", "0532 410 22 18");
        Hasta hasta2 = hasta("18301661332", "Burak Yıldız", LocalDate.of(1988, 11, 3), "burak.yildiz@ornekposta.com", "0544 318 70 05");

        // ---- Örnek randevular ----
        LocalDate ileriGun = sonrakiHaftaIci(LocalDate.now().plusDays(1));
        LocalDate gecmisGun = oncekiHaftaIci(LocalDate.now().minusDays(1));

        randevuRepository.save(Randevu.olustur(hasta1, ayse, genelMuayene, ileriGun.atTime(10, 0), "Halsizlik şikayeti"));
        Randevu onayli = Randevu.olustur(hasta1, mehmet, ekg, ileriGun.atTime(14, 0), null);
        onayli.onayla(mehmet);
        randevuRepository.save(onayli);
        randevuRepository.save(Randevu.olustur(hasta2, ayse, genelMuayene, ileriGun.atTime(9, 0), null));

        // Geçmiş randevular: elle tamamlanan, gelmeyen, sistemin kapatacakları
        Randevu tamamlanan = Randevu.gecmisKaydiAktar(hasta1, ayse, genelMuayene, gecmisGun.atTime(11, 0), null,
                gecmisGun.minusDays(3).atTime(20, 15));
        tamamlanan.onayla(ayse, gecmisGun.minusDays(2).atTime(9, 5));
        tamamlanan.tamamla(ayse, gecmisGun.atTime(11, 22));
        randevuRepository.save(tamamlanan);
        LocalDate gelmeyenGun = gecmisGun.minusWeeks(1);
        Randevu gelmeyen = Randevu.gecmisKaydiAktar(hasta1, mehmet, kardiyolojiMuayenesi, gelmeyenGun.atTime(9, 30), null,
                gelmeyenGun.minusDays(4).atTime(18, 40));
        gelmeyen.onayla(mehmet, gelmeyenGun.minusDays(3).atTime(8, 50));
        gelmeyen.gelmediOlarakIsaretle(mehmet, gelmeyenGun.atTime(10, 5));
        randevuRepository.save(gelmeyen);
        // Zamanlayıcı açılışta bunları işler: onaylı olan otomatik tamamlanır, onaysız olan düşer
        Randevu otomatikTamamlanacak = Randevu.gecmisKaydiAktar(hasta2, ayse, genelMuayene, gecmisGun.atTime(13, 0),
                "Kontrol muayenesi", gecmisGun.minusDays(2).atTime(14, 30));
        otomatikTamamlanacak.onayla(ayse, gecmisGun.minusDays(1).atTime(10, 0));
        randevuRepository.save(otomatikTamamlanacak);
        randevuRepository.save(Randevu.gecmisKaydiAktar(hasta2, mehmet, ekg, gecmisGun.atTime(15, 0), null,
                gecmisGun.minusDays(2).atTime(21, 10)));

        icerikYukle(hastane, klinik);

        LOG.info("Ornek veriler yuklendi.");
    }

    private void icerikYukle(Kurum hastane, Kurum klinik) {
        SiteAyarlari ayarlar = SiteAyarlari.varsayilan();
        ayarlar.iletisimGuncelle("0850 222 00 00", "destek@medora.com.tr", "Sakarya", "Sistem Yöneticisi");
        siteAyarlariRepository.save(ayarlar);

        Duyuru yenilendi = new Duyuru("Online randevu sistemimiz yenilendi",
                "Doktorların iki haftalık boş saatlerini artık tek ekranda görebilir, randevunuzu birkaç dokunuşla alabilirsiniz.",
                """
                        Yeni randevu ekranında hizmeti seçtiğiniz anda o hizmeti veren tüm doktorların önümüzdeki iki haftalık boş saatleri listelenir. Takvimde gün gün arama yapmanıza gerek kalmaz.

                        Randevu talebiniz oluşturulduktan sonra ilgili doktor veya kurum yöneticisi tarafından onaylanır. Onay durumunu Randevularım sayfasından takip edebilirsiniz.

                        Randevu saatine kadar onaylanmayan talepler otomatik olarak düşer ve saat diğer hastalara açılır. Gelemeyeceğiniz randevuyu panelinizden iptal ederek başka bir hastanın o saati kullanmasına yardımcı olabilirsiniz.
                        """,
                DuyuruKategorisi.DUYURU, null, "Sistem Yöneticisi");
        yenilendi.yayinla();
        yenilendi.oneCikar();

        Duyuru grip = new Duyuru("Grip aşısı randevuları başladı",
                "Risk grubundaki hastalarımız Serdivan ve Adapazarı şubelerimizden aşı randevusu alabilir.",
                """
                        Kış aylarına hazırlık kapsamında grip aşısı uygulamaları başladı. 65 yaş üstü, kronik hastalığı olan ve sağlık çalışanı olan hastalarımızın aşı olmasını öneriyoruz.

                        Aşı uygulaması öncesinde kısa bir genel muayene yapılır. Randevu ekranından Genel Muayene hizmetini seçerek size uygun saati belirleyebilirsiniz.
                        """,
                DuyuruKategorisi.HABER, hastane, "Ahmet Yılmaz");
        grip.yayinla();

        Duyuru mr = new Duyuru("Serdivan Şubesi'nde yeni MR cihazı hizmete girdi",
                "Daha kısa çekim süresi ve daha sessiz çalışmasıyla yeni cihazımız hasta konforunu artırıyor.",
                """
                        Serdivan Şubemizin radyoloji bölümüne yeni nesil MR cihazı kuruldu. Çekim süreleri ortalama yüzde otuz kısaldı.

                        MR Görüntüleme randevuları online sistem üzerinden alınabilir. Çekim öncesinde metal eşyalarınızı çıkarmanız ve varsa önceki raporlarınızı yanınızda getirmeniz gerekir.
                        """,
                DuyuruKategorisi.HABER, hastane, "Ahmet Yılmaz");
        mr.yayinla();

        Duyuru kalp = new Duyuru("Kalp sağlığı için beş günlük alışkanlık",
                "Kardiyoloji uzmanımızın önerileriyle kalbinizi korumak için her gün yapabileceğiniz küçük değişiklikler.",
                """
                        Günde en az otuz dakika tempolu yürüyüş, kalp ve damar sağlığı için en kolay başlangıçtır.

                        Tuz tüketimini azaltmak tansiyonu düşürmeye yardımcı olur. Hazır gıdalardaki gizli tuza dikkat edin.

                        Düzenli uyku, stres yönetimi ve sigarayı bırakmak riskleri belirgin şekilde azaltır. Kırk yaşından sonra yılda bir kez kardiyoloji kontrolü önerilir.
                        """,
                DuyuruKategorisi.SAGLIK_REHBERI, hastane, "Ahmet Yılmaz");
        kalp.yayinla();

        Duyuru dis = new Duyuru("Çocuklarda diş fırçalama alışkanlığı nasıl kazandırılır?",
                "Gülümse Diş Kliniği hekimlerinden ebeveynlere pratik öneriler.",
                """
                        Diş fırçalamayı ilk süt dişi çıktığında başlatmak, alışkanlığın erken yerleşmesini sağlar.

                        Fırçalamayı oyuna dönüştürmek ve ebeveynin birlikte fırçalaması çocukların motivasyonunu artırır. Altı ayda bir diş hekimi kontrolü önerilir.
                        """,
                DuyuruKategorisi.SAGLIK_REHBERI, klinik, "Elif Kaya");
        dis.yayinla();

        Duyuru taslak = new Duyuru("Bayram tatilinde çalışma saatleri",
                "Bayram süresince şubelerimizin çalışma saatleri değişecektir.",
                "Bayram süresince acil servis hariç tüm polikliniklerimiz kapalı olacaktır. Ayrıntılı saatler yakında paylaşılacak.",
                DuyuruKategorisi.DUYURU, hastane, "Ahmet Yılmaz");

        duyuruRepository.saveAll(List.of(dis, kalp, mr, grip, yenilendi, taslak));
    }

    private Doktor doktor(String eposta, String ad, String uzmanlik, Sube sube, Bolum bolum, Hizmet... hizmetler) {
        Doktor doktor = new Doktor(eposta, sifre(), ad, uzmanlik);
        doktor.gorevlendir(sube, bolum);
        doktor.hizmetleriAyarla(List.of(hizmetler));
        return kullaniciRepository.save(doktor);
    }

    private SaglikPersoneli personel(String eposta, String ad, String gorev, Sube sube, Bolum bolum, Hizmet... hizmetler) {
        SaglikPersoneli personel = new SaglikPersoneli(eposta, sifre(), ad, gorev);
        personel.gorevlendir(sube, bolum);
        personel.hizmetleriAyarla(List.of(hizmetler));
        return kullaniciRepository.save(personel);
    }

    private Hasta hasta(String tcKimlikNo, String ad, LocalDate dogumTarihi, String eposta, String telefon) {
        Hasta hasta = new Hasta(tcKimlikNo, sifre(), ad, dogumTarihi);
        hasta.epostaGuncelle(eposta);
        hasta.telefonGuncelle(telefon);
        return kullaniciRepository.save(hasta);
    }

    private String sifre() {
        return sifreleyici.encode(ORNEK_SIFRE);
    }

    private static BigDecimal tl(int tutar) {
        return BigDecimal.valueOf(tutar);
    }

    private static LocalDate sonrakiHaftaIci(LocalDate tarih) {
        while (!HAFTA_ICI.contains(tarih.getDayOfWeek())) {
            tarih = tarih.plusDays(1);
        }
        return tarih;
    }

    private static LocalDate oncekiHaftaIci(LocalDate tarih) {
        while (!HAFTA_ICI.contains(tarih.getDayOfWeek())) {
            tarih = tarih.minusDays(1);
        }
        return tarih;
    }
}
