package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.KayitBulunamadiHatasi;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kurum.*;
import com.klinik.randevu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Kurum, şube, bölüm, çalışma saati ve hizmet tanımları.
 * Her değiştirici işlem, işlemi yapan kullanıcının o kurum üzerinde yetkisi
 * olup olmadığını {@link Kullanici#kurumuYonetebilirMi(Kurum)} ile sorar.
 */
@Service
@Transactional
public class KurumServisi {

    private final KurumRepository kurumRepository;
    private final SubeRepository subeRepository;
    private final BolumRepository bolumRepository;
    private final HizmetRepository hizmetRepository;
    private final RandevuRepository randevuRepository;
    private final CalisanRepository calisanRepository;
    private final KullaniciRepository kullaniciRepository;
    private final DuyuruRepository duyuruRepository;

    public KurumServisi(KurumRepository kurumRepository, SubeRepository subeRepository,
                        BolumRepository bolumRepository, HizmetRepository hizmetRepository,
                        RandevuRepository randevuRepository, CalisanRepository calisanRepository,
                        KullaniciRepository kullaniciRepository, DuyuruRepository duyuruRepository) {
        this.kurumRepository = kurumRepository;
        this.subeRepository = subeRepository;
        this.bolumRepository = bolumRepository;
        this.hizmetRepository = hizmetRepository;
        this.randevuRepository = randevuRepository;
        this.calisanRepository = calisanRepository;
        this.kullaniciRepository = kullaniciRepository;
        this.duyuruRepository = duyuruRepository;
    }

    // ---- Kurum ----

    /**
     * Kurum ilk şubesiyle birlikte tek işlemde oluşturulur; şube adı geçersizse kurum da kaydedilmez.
     * Şubesiz bir kurumda randevu alınamayacağı için ilk şube zorunludur.
     */
    public Kurum kurumOlustur(Kullanici yapan, String ad, KurumTuru tur, String aciklama, String ilkSubeAdi) {
        kurumTanimlayabilmeli(yapan, "Yeni kurumu yalnızca sistem yöneticisi tanımlayabilir.");
        Kurum kurum = new Kurum(ad, tur, aciklama);
        kurum.subeEkle(ilkSubeAdi, null, null);
        return kurumRepository.save(kurum);
    }

    /** Kurumun adı, türü ve açıklaması: sistem yöneticisi her kurumda, kurum yöneticisi kendi kurumunda değiştirir. */
    public void kurumGuncelle(Kullanici yapan, Long kurumId, String ad, KurumTuru tur, String aciklama) {
        Kurum kurum = kurumBul(kurumId);
        yetkiKontrolEt(yapan, kurum);
        kurum.bilgileriGuncelle(ad, tur, aciklama);
    }

    /** Yayından kaldırma geri alınabilir; geçmiş ve mevcut randevular korunur. */
    public Kurum kurumYayinDurumuDegistir(Kullanici yapan, Long kurumId) {
        kurumTanimlayabilmeli(yapan, "Kurumu yalnızca sistem yöneticisi yayından kaldırabilir.");
        Kurum kurum = kurumBul(kurumId);
        if (kurum.isAktif()) {
            kurum.yayindanKaldir();
        } else {
            kurum.yayinaAl();
        }
        return kurum;
    }

    /**
     * Kalıcı silme yalnızca hiç kullanılmamış (yanlışlıkla açılmış) kurum içindir:
     * randevu geçmişi ya da personeli olan kurum silinmez, yayından kaldırılır.
     * Kurumla birlikte şubeleri, hizmetleri, kurum duyuruları ve yönetici hesapları da silinir.
     */
    public String kurumSil(Kullanici yapan, Long kurumId) {
        kurumTanimlayabilmeli(yapan, "Kurumu yalnızca sistem yöneticisi silebilir.");
        Kurum kurum = kurumBul(kurumId);
        if (randevuRepository.existsBySubeKurumId(kurumId)) {
            throw new IsKuraliIhlali("'" + kurum.getAd() + "' kurumunun randevu kayıtları var; silinemez. "
                    + "Yeni randevuları durdurmak için yayından kaldırın.");
        }
        if (calisanRepository.existsBySubeKurumId(kurumId)) {
            throw new IsKuraliIhlali("'" + kurum.getAd() + "' kurumunda kayıtlı personel var; silinemez. "
                    + "Yayından kaldırabilir ya da önce personeli kaldırabilirsiniz.");
        }
        duyuruRepository.deleteAll(duyuruRepository.findByKurumIdOrderByGuncellemeZamaniDesc(kurumId));
        kullaniciRepository.deleteAll(kullaniciRepository.kurumYoneticileri(kurumId));
        kurumRepository.delete(kurum);
        return kurum.getAd();
    }

    @Transactional(readOnly = true)
    public List<Kurum> tumKurumlar() {
        return kurumRepository.findAllByOrderByAdAsc();
    }

    /** Herkese açık sayfalar ve randevu alma için yalnızca yayındaki kurumlar. */
    @Transactional(readOnly = true)
    public List<Kurum> yayindakiKurumlar() {
        return kurumRepository.findByAktifTrueOrderByAdAsc();
    }

    @Transactional(readOnly = true)
    public Kurum kurumBul(Long kurumId) {
        return kurumRepository.findById(kurumId).orElseThrow(() -> new KayitBulunamadiHatasi("Kurum", kurumId));
    }

    // ---- Şube ve çalışma saatleri ----

    public Sube subeEkle(Kullanici yapan, Long kurumId, String ad, String adres, String telefon) {
        Kurum kurum = kurumBul(kurumId);
        yetkiKontrolEt(yapan, kurum);
        return subeRepository.save(kurum.subeEkle(ad, adres, telefon));
    }

    @Transactional(readOnly = true)
    public Sube subeBul(Long subeId) {
        return subeRepository.findById(subeId).orElseThrow(() -> new KayitBulunamadiHatasi("Şube", subeId));
    }

    @Transactional(readOnly = true)
    public Sube yonetilenSubeBul(Kullanici yapan, Long subeId) {
        Sube sube = subeBul(subeId);
        yetkiKontrolEt(yapan, sube.getKurum());
        return sube;
    }

    @Transactional(readOnly = true)
    public List<Sube> subeler(Long kurumId) {
        return subeRepository.findByKurumIdOrderByAdAsc(kurumId);
    }

    public void subeGuncelle(Kullanici yapan, Long subeId, String ad, String adres, String telefon) {
        yonetilenSubeBul(yapan, subeId).bilgileriGuncelle(ad, adres, telefon);
    }

    /**
     * Yalnızca boş şube kaldırılır: personeli ya da randevu geçmişi olan şube silinmez,
     * kurumun son şubesi de kaldırılamaz (bkz. {@link Kurum#subeKaldir}).
     */
    public String subeSil(Kullanici yapan, Long subeId) {
        Sube sube = yonetilenSubeBul(yapan, subeId);
        if (calisanRepository.existsBySubeId(subeId)) {
            throw new IsKuraliIhlali("'" + sube.getAd() + "' şubesinde kayıtlı personel var. Önce personeli başka şubeye taşıyın ya da pasifleştirin.");
        }
        if (randevuRepository.existsBySubeId(subeId)) {
            throw new IsKuraliIhlali("'" + sube.getAd() + "' şubesinin randevu geçmişi var; kayıtlar korunmak zorunda olduğu için silinemez.");
        }
        sube.getKurum().subeKaldir(sube);
        return sube.getAd();
    }

    public void calismaSaatiAyarla(Kullanici yapan, Long subeId, DayOfWeek gun, LocalTime acilis, LocalTime kapanis) {
        yonetilenSubeBul(yapan, subeId).calismaSaatiAyarla(gun, acilis, kapanis);
    }

    public void calismaSaatiKaldir(Kullanici yapan, Long subeId, DayOfWeek gun) {
        yonetilenSubeBul(yapan, subeId).calismaSaatiKaldir(gun);
    }

    // ---- Bölüm ----

    public Bolum bolumEkle(Kullanici yapan, Long subeId, String ad, String aciklama) {
        Sube sube = yonetilenSubeBul(yapan, subeId);
        return bolumRepository.save(sube.bolumEkle(ad, aciklama));
    }

    public Sube bolumGuncelle(Kullanici yapan, Long bolumId, String ad, String aciklama) {
        Bolum bolum = yonetilenBolumBul(yapan, bolumId);
        bolum.getSube().bolumGuncelle(bolum, ad, aciklama);
        return bolum.getSube();
    }

    /** Personeli olan bölüm silinmez; personel önce başka bölüme alınır. */
    public Sube bolumSil(Kullanici yapan, Long bolumId) {
        Bolum bolum = yonetilenBolumBul(yapan, bolumId);
        if (calisanRepository.existsByBolumId(bolumId)) {
            throw new IsKuraliIhlali("'" + bolum.getAd() + "' bölümünde çalışan personel var; önce personeli başka bölüme alın.");
        }
        Sube sube = bolum.getSube();
        sube.bolumKaldir(bolum);
        return sube;
    }

    private Bolum yonetilenBolumBul(Kullanici yapan, Long bolumId) {
        Bolum bolum = bolumRepository.findById(bolumId).orElseThrow(() -> new KayitBulunamadiHatasi("Bölüm", bolumId));
        yetkiKontrolEt(yapan, bolum.getSube().getKurum());
        return bolum;
    }

    @Transactional(readOnly = true)
    public List<Bolum> bolumler(Long kurumId) {
        return bolumRepository.findBySubeKurumIdOrderBySubeAdAscAdAsc(kurumId);
    }

    // ---- Hizmet ----

    public Hizmet hizmetEkle(Kullanici yapan, Long kurumId, String ad, HizmetTuru tur, int sureDakika,
                             BigDecimal ucret, String aciklama) {
        Kurum kurum = kurumBul(kurumId);
        yetkiKontrolEt(yapan, kurum);
        return hizmetRepository.save(kurum.hizmetEkle(ad, tur, sureDakika, ucret, aciklama));
    }

    public void hizmetGuncelle(Kullanici yapan, Long hizmetId, int sureDakika, BigDecimal ucret) {
        Hizmet hizmet = hizmetBul(hizmetId);
        yetkiKontrolEt(yapan, hizmet.getKurum());
        hizmet.setSureDakika(sureDakika);
        hizmet.setUcret(ucret);
    }

    /** Aşırı yükleme: tanım bilgileriyle birlikte tam güncelleme. */
    public void hizmetGuncelle(Kullanici yapan, Long hizmetId, String ad, HizmetTuru tur, int sureDakika,
                               BigDecimal ucret, String aciklama) {
        hizmetGuncelle(yapan, hizmetId, sureDakika, ucret);
        hizmetBul(hizmetId).tanimiGuncelle(ad, tur, aciklama);
    }

    public void hizmetAktiflikDegistir(Kullanici yapan, Long hizmetId) {
        Hizmet hizmet = hizmetBul(hizmetId);
        yetkiKontrolEt(yapan, hizmet.getKurum());
        hizmet.aktiflikDegistir();
    }

    @Transactional(readOnly = true)
    public Hizmet hizmetBul(Long hizmetId) {
        return hizmetRepository.findById(hizmetId).orElseThrow(() -> new KayitBulunamadiHatasi("Hizmet", hizmetId));
    }

    @Transactional(readOnly = true)
    public List<Hizmet> hizmetler(Long kurumId) {
        return hizmetRepository.findByKurumIdOrderByAdAsc(kurumId);
    }

    @Transactional(readOnly = true)
    public List<Hizmet> aktifHizmetler(Long kurumId) {
        return hizmetRepository.findByKurumIdAndAktifTrueOrderByAdAsc(kurumId);
    }

    private static void kurumTanimlayabilmeli(Kullanici yapan, String mesaj) {
        if (!yapan.kurumTanimlayabilirMi()) {
            throw new YetkisizIslemHatasi(mesaj);
        }
    }

    private void yetkiKontrolEt(Kullanici yapan, Kurum kurum) {
        if (!yapan.kurumuYonetebilirMi(kurum)) {
            throw new YetkisizIslemHatasi("'" + kurum.getAd() + "' kurumu üzerinde işlem yetkiniz yok.");
        }
    }
}
