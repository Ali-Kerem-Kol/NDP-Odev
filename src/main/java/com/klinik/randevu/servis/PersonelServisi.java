package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.KayitBulunamadiHatasi;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kurum.Bolum;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.repository.BolumRepository;
import com.klinik.randevu.repository.CalisanRepository;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.repository.HizmetRepository;
import com.klinik.randevu.repository.RandevuRepository;
import com.klinik.randevu.repository.SubeRepository;
import com.klinik.randevu.web.form.CalisanFormu;
import com.klinik.randevu.web.form.CalisanGuncellemeFormu;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Doktor ve sağlık personeli kaydı, hizmet ataması ve uygunluk takvimi. */
@Service
@Transactional
public class PersonelServisi {

    private final CalisanRepository calisanRepository;
    private final SubeRepository subeRepository;
    private final BolumRepository bolumRepository;
    private final HizmetRepository hizmetRepository;
    private final KullaniciServisi kullaniciServisi;
    private final PasswordEncoder sifreleyici;
    private final RandevuRepository randevuRepository;

    public PersonelServisi(CalisanRepository calisanRepository, SubeRepository subeRepository,
                           BolumRepository bolumRepository, HizmetRepository hizmetRepository,
                           KullaniciServisi kullaniciServisi, PasswordEncoder sifreleyici,
                           RandevuRepository randevuRepository) {
        this.calisanRepository = calisanRepository;
        this.subeRepository = subeRepository;
        this.bolumRepository = bolumRepository;
        this.hizmetRepository = hizmetRepository;
        this.kullaniciServisi = kullaniciServisi;
        this.sifreleyici = sifreleyici;
        this.randevuRepository = randevuRepository;
    }

    public Calisan calisanEkle(Kullanici yapan, CalisanFormu form) {
        if (form.getSubeId() == null) {
            throw new IsKuraliIhlali("Personelin çalışacağı şubeyi seçin.");
        }
        Sube sube = subeRepository.findById(form.getSubeId())
                .orElseThrow(() -> new KayitBulunamadiHatasi("Şube", form.getSubeId()));
        if (!yapan.kurumuYonetebilirMi(sube.getKurum())) {
            throw new YetkisizIslemHatasi("Bu şubeye çalışan ekleme yetkiniz yok.");
        }
        KullaniciServisi.sifreKontrolEt(form.getSifre());
        kullaniciServisi.epostaKullanilabilirMi(form.getEposta(), null);
        Bolum bolum = form.getBolumId() == null ? null : bolumRepository.findById(form.getBolumId())
                .orElseThrow(() -> new KayitBulunamadiHatasi("Bölüm", form.getBolumId()));

        Calisan calisan = form.getTip().olustur(form.getEposta(), sifreleyici.encode(form.getSifre()),
                form.getAdSoyad(), form.getUnvanBilgisi());
        calisan.gorevlendir(sube, bolum);
        calisan.telefonGuncelle(form.getTelefon());
        return calisanRepository.save(calisan);
    }

    @Transactional(readOnly = true)
    public Calisan calisanBul(Long calisanId) {
        return calisanRepository.findById(calisanId)
                .orElseThrow(() -> new KayitBulunamadiHatasi("Çalışan", calisanId));
    }

    @Transactional(readOnly = true)
    public Calisan yonetilenCalisanBul(Kullanici yapan, Long calisanId) {
        Calisan calisan = calisanBul(calisanId);
        if (!yapan.calisaniYonetebilirMi(calisan)) {
            throw new YetkisizIslemHatasi("Bu çalışanın bilgilerini düzenleme yetkiniz yok.");
        }
        return calisan;
    }

    /**
     * Ad, unvan, şube, bölüm ve telefonu günceller. Şube değişikliği, ileri tarihli aktif
     * randevusu olan personelde engellenir: o randevular eski şubede kalır ve hasta yanlış adrese gider.
     */
    public void calisanGuncelle(Kullanici yapan, Long calisanId, CalisanGuncellemeFormu form) {
        Calisan calisan = kurumYetkisiyleBul(yapan, calisanId);
        if (form.getSubeId() == null) {
            throw new IsKuraliIhlali("Personelin çalışacağı şubeyi seçin.");
        }
        Sube sube = subeRepository.findById(form.getSubeId())
                .orElseThrow(() -> new KayitBulunamadiHatasi("Şube", form.getSubeId()));
        if (!yapan.kurumuYonetebilirMi(sube.getKurum())) {
            throw new YetkisizIslemHatasi("Personel yalnızca kendi kurumunuzun şubelerine atanabilir.");
        }
        if (!sube.equals(calisan.getSube())) {
            long ileriRandevu = ileriTarihliAktifRandevuSayisi(calisan);
            if (ileriRandevu > 0) {
                throw new IsKuraliIhlali(calisan.getGorunenAd() + " personelinin " + ileriRandevu
                        + " ileri tarihli randevusu var. Şube değiştirmeden önce bu randevuları iptal edin.");
            }
        }
        Bolum bolum = form.getBolumId() == null ? null : bolumRepository.findById(form.getBolumId())
                .orElseThrow(() -> new KayitBulunamadiHatasi("Bölüm", form.getBolumId()));
        calisan.adSoyadDuzelt(form.getAdSoyad());
        calisan.unvaniGuncelle(form.getUnvanBilgisi());
        calisan.gorevlendir(sube, bolum);
        calisan.telefonGuncelle(form.getTelefon());
    }

    /** Ayrılan personel silinmez, pasifleştirilir: geçmiş randevuları ve kayıtları korunur. */
    public void pasiflestir(Kullanici yapan, Long calisanId) {
        Calisan calisan = kurumYetkisiyleBul(yapan, calisanId);
        long ileriRandevu = ileriTarihliAktifRandevuSayisi(calisan);
        if (ileriRandevu > 0) {
            throw new IsKuraliIhlali(calisan.getGorunenAd() + " personelinin " + ileriRandevu
                    + " ileri tarihli randevusu var. Pasifleştirmeden önce bu randevuları iptal edin.");
        }
        calisan.pasiflestir();
    }

    public void aktiflestir(Kullanici yapan, Long calisanId) {
        kurumYetkisiyleBul(yapan, calisanId).aktiflestir();
    }

    @Transactional(readOnly = true)
    public long ileriTarihliAktifRandevuSayisi(Calisan calisan) {
        return randevuRepository.countByCalisanIdAndDurumInAndBaslangicAfter(calisan.getId(),
                RandevuDurumu.aktifDurumlar(), LocalDateTime.now());
    }

    private Calisan kurumYetkisiyleBul(Kullanici yapan, Long calisanId) {
        Calisan calisan = calisanBul(calisanId);
        if (!yapan.kurumuYonetebilirMi(calisan.getKurum())) {
            throw new YetkisizIslemHatasi("Bu personeli düzenleme yetkiniz yok.");
        }
        return calisan;
    }

    /** Hizmet ataması kurum yetkisi ister; çalışan kendi hizmetlerini değiştiremez. */
    public void hizmetleriAta(Kullanici yapan, Long calisanId, List<Long> hizmetIdleri) {
        Calisan calisan = calisanBul(calisanId);
        if (!yapan.kurumuYonetebilirMi(calisan.getKurum())) {
            throw new YetkisizIslemHatasi("Hizmet atamasını yalnızca kurum yöneticisi yapabilir.");
        }
        List<Hizmet> hizmetler = hizmetIdleri == null ? List.of() : hizmetRepository.findAllById(hizmetIdleri);
        calisan.hizmetleriAyarla(hizmetler);
    }

    public void uygunlukEkle(Kullanici yapan, Long calisanId, DayOfWeek gun, LocalTime baslangic, LocalTime bitis) {
        yonetilenCalisanBul(yapan, calisanId).uygunlukEkle(gun, baslangic, bitis);
    }

    public void uygunlukKaldir(Kullanici yapan, Long calisanId, Long dilimId) {
        yonetilenCalisanBul(yapan, calisanId).uygunlukKaldir(dilimId);
    }

    @Transactional(readOnly = true)
    public List<Calisan> kurumCalisanlari(Long kurumId) {
        return calisanRepository.findBySubeKurumIdOrderByAdSoyadAsc(kurumId);
    }

    @Transactional(readOnly = true)
    public List<Calisan> hizmetVerenler(Long subeId, Long hizmetId) {
        return calisanRepository.hizmetVerenler(subeId, hizmetId);
    }
}
