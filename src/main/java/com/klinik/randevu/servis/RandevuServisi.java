package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.KayitBulunamadiHatasi;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kullanici.UygunlukDilimi;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.repository.CalisanRepository;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.RandevuRepository;
import com.klinik.randevu.servis.kural.DolulukBilgisi;
import com.klinik.randevu.servis.kural.RandevuKurali;
import com.klinik.randevu.servis.kural.RandevuTalebi;
import com.klinik.randevu.servis.kural.TakvimDolulukBilgisi;
import com.klinik.randevu.servis.kural.VeritabaniDolulukBilgisi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Randevu oluşturma, müsaitlik takvimi ve onay akışı.
 * <p>
 * Doğrulama kuralları {@link RandevuKurali} arayüzünün tüm uygulamaları olarak
 * Spring tarafından enjekte edilir; servis kuralların ne olduğunu bilmez.
 * Ekranda gösterilen boş saatler, kaydederken çalışan kuralların aynısından
 * geçirilerek üretilir; bu yüzden gösterilen her saat gerçekten alınabilir.
 */
@Service
@Transactional
public class RandevuServisi {

    /** En kısa saat aralığı; daha uzun hizmetlerde aralık hizmet süresi kadardır. */
    public static final int ASGARI_SAAT_ADIMI_DAKIKA = 15;

    private final RandevuRepository randevuRepository;
    private final PersonelServisi personelServisi;
    private final KurumServisi kurumServisi;
    private final List<RandevuKurali> kurallar;
    private final VeritabaniDolulukBilgisi veritabaniDolulugu;
    private final CalisanRepository calisanRepository;
    private final KullaniciRepository kullaniciRepository;

    public RandevuServisi(RandevuRepository randevuRepository, PersonelServisi personelServisi,
                          KurumServisi kurumServisi, List<RandevuKurali> kurallar,
                          VeritabaniDolulukBilgisi veritabaniDolulugu, CalisanRepository calisanRepository,
                          KullaniciRepository kullaniciRepository) {
        this.randevuRepository = randevuRepository;
        this.personelServisi = personelServisi;
        this.kurumServisi = kurumServisi;
        this.kurallar = kurallar;
        this.veritabaniDolulugu = veritabaniDolulugu;
        this.calisanRepository = calisanRepository;
        this.kullaniciRepository = kullaniciRepository;
    }

    /**
     * Kontrol et-sonra-kaydet arasında başka bir talebin araya girmemesi için önce çalışanın,
     * sonra hastanın satırı kilitlenir. Kilit sırası hep aynı olduğundan iki işlem birbirini
     * karşılıklı bekleyemez (deadlock oluşmaz). Kilit işlem bitince bırakılır.
     */
    public Randevu randevuOlustur(Hasta hasta, Long calisanId, Long hizmetId, LocalDateTime baslangic, String hastaNotu) {
        Calisan calisan = calisanRepository.kilitleyerekBul(calisanId)
                .orElseThrow(() -> new KayitBulunamadiHatasi("Çalışan", calisanId));
        kullaniciRepository.hastayiKilitle(hasta.getId())
                .orElseThrow(() -> new KayitBulunamadiHatasi("Hasta", hasta.getId()));
        Hizmet hizmet = kurumServisi.hizmetBul(hizmetId);
        RandevuTalebi talep = RandevuTalebi.of(hasta, calisan, hizmet, baslangic);
        kurallariUygula(talep, veritabaniDolulugu);
        return randevuRepository.save(Randevu.olustur(hasta, calisan, hizmet, baslangic, hastaNotu));
    }

    /** Randevu kaydetmeden önce özet ekranında talebin hâlâ geçerli olduğunu doğrular. */
    @Transactional(readOnly = true)
    public RandevuTalebi talebiDogrula(Hasta hasta, Long calisanId, Long hizmetId, LocalDateTime baslangic) {
        RandevuTalebi talep = RandevuTalebi.of(hasta, personelServisi.calisanBul(calisanId),
                kurumServisi.hizmetBul(hizmetId), baslangic);
        kurallariUygula(talep, veritabaniDolulugu);
        return talep;
    }

    /**
     * Çalışanın {@code gunSayisi} günlük boş saat takvimi. Aralıktaki randevular tek
     * sorguda yüklenir, her aday saat bellekte tüm kurallardan geçirilir.
     * Hiç boş saati olmayan günler listeye girmez.
     */
    @Transactional(readOnly = true)
    public List<GunlukMusaitlik> musaitlikTakvimi(Hasta hasta, Calisan calisan, Hizmet hizmet,
                                                  LocalDate ilkGun, int gunSayisi) {
        LocalDate sonGun = ilkGun.plusDays(gunSayisi);
        DolulukBilgisi doluluk = new TakvimDolulukBilgisi(randevuRepository.aktifRandevular(
                calisan.getId(), hasta.getId(), ilkGun.atStartOfDay(), sonGun.atStartOfDay(),
                RandevuDurumu.aktifDurumlar()));
        int adim = Math.max(hizmet.getSureDakika(), ASGARI_SAAT_ADIMI_DAKIKA);

        List<GunlukMusaitlik> takvim = new ArrayList<>();
        for (LocalDate gun = ilkGun; gun.isBefore(sonGun); gun = gun.plusDays(1)) {
            List<LocalTime> saatler = new ArrayList<>();
            for (UygunlukDilimi dilim : calisan.gunlukUygunluklar(gun.getDayOfWeek())) {
                int ilkDakika = dilim.getBaslangic().toSecondOfDay() / 60;
                int sonDakika = dilim.getBitis().toSecondOfDay() / 60;
                for (int dakika = ilkDakika; dakika + hizmet.getSureDakika() <= sonDakika; dakika += adim) {
                    LocalTime saat = LocalTime.ofSecondOfDay(dakika * 60L);
                    if (kurallaraUyuyorMu(RandevuTalebi.of(hasta, calisan, hizmet, gun.atTime(saat)), doluluk)) {
                        saatler.add(saat);
                    }
                }
            }
            if (!saatler.isEmpty()) {
                takvim.add(new GunlukMusaitlik(gun, List.copyOf(saatler)));
            }
        }
        return takvim;
    }

    @Transactional(readOnly = true)
    public List<LocalTime> uygunSaatler(Hasta hasta, Long calisanId, Long hizmetId, LocalDate tarih) {
        return musaitlikTakvimi(hasta, personelServisi.calisanBul(calisanId), kurumServisi.hizmetBul(hizmetId), tarih, 1)
                .stream().findFirst().map(GunlukMusaitlik::saatler).orElse(List.of());
    }

    private void kurallariUygula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        kurallar.forEach(kural -> kural.dogrula(talep, doluluk));
    }

    private boolean kurallaraUyuyorMu(RandevuTalebi talep, DolulukBilgisi doluluk) {
        try {
            kurallariUygula(talep, doluluk);
            return true;
        } catch (IsKuraliIhlali ihlal) {
            return false;
        }
    }

    // ---- Onay mekanizması ----

    public void onayla(Long randevuId, Kullanici yapan) {
        yonetilenRandevu(randevuId, yapan).onayla(yapan);
    }

    public void reddet(Long randevuId, Kullanici yapan, String neden) {
        yonetilenRandevu(randevuId, yapan).reddet(yapan, neden);
    }

    public void tamamla(Long randevuId, Kullanici yapan) {
        yonetilenRandevu(randevuId, yapan).tamamla(yapan);
    }

    public void gelmedi(Long randevuId, Kullanici yapan) {
        yonetilenRandevu(randevuId, yapan).gelmediOlarakIsaretle(yapan);
    }

    public void iptalEt(Long randevuId, Kullanici yapan, String neden) {
        Randevu randevu = randevuBul(randevuId);
        if (!yapan.randevuyuIptalEdebilirMi(randevu)) {
            throw new YetkisizIslemHatasi("Bu randevuyu iptal etme yetkiniz yok.");
        }
        randevu.iptalEt(yapan, neden);
    }

    /**
     * Zamanı geçen randevuların durumunu günceller (zamanlayıcı her dakika çağırır):
     * saati gelip onaylanmamış talepler düşer, bitişinden tolerans kadar sonra hâlâ
     * işaretlenmemiş onaylı randevular tamamlanmış sayılır.
     */
    public ZamanIslemSonucu suresiDolanlariIsle(LocalDateTime simdi, Duration tamamlamaToleransi) {
        int dusen = 0;
        for (Randevu r : randevuRepository.findByDurumAndBaslangicLessThanEqual(RandevuDurumu.BEKLEMEDE, simdi)) {
            if (r.zamanAsiminiUygula(simdi)) {
                dusen++;
            }
        }
        int tamamlanan = 0;
        for (Randevu r : randevuRepository.findByDurumAndBitisBefore(RandevuDurumu.ONAYLANDI, simdi.minus(tamamlamaToleransi))) {
            if (r.otomatikTamamla(simdi, tamamlamaToleransi)) {
                tamamlanan++;
            }
        }
        return new ZamanIslemSonucu(dusen, tamamlanan);
    }

    public record ZamanIslemSonucu(int zamanAsimi, int otomatikTamamlanan) {
        public boolean isBos() {
            return zamanAsimi == 0 && otomatikTamamlanan == 0;
        }
    }

    /** Randevu detayını yalnızca ilgili kişiler görebilir (hasta, çalışan, kurum yöneticisi, sistem yöneticisi). */
    @Transactional(readOnly = true)
    public Randevu gorulebilirRandevu(Long randevuId, Kullanici kullanici) {
        Randevu randevu = randevuBul(randevuId);
        if (!kullanici.randevuyuGorebilirMi(randevu)) {
            throw new YetkisizIslemHatasi("Bu randevuyu görüntüleme yetkiniz yok.");
        }
        return randevu;
    }

    private Randevu yonetilenRandevu(Long randevuId, Kullanici yapan) {
        Randevu randevu = randevuBul(randevuId);
        if (!yapan.randevuyuYonetebilirMi(randevu)) {
            throw new YetkisizIslemHatasi("Bu randevu üzerinde işlem yetkiniz yok.");
        }
        return randevu;
    }

    // ---- Sorgular ----

    @Transactional(readOnly = true)
    public Randevu randevuBul(Long randevuId) {
        return randevuRepository.findById(randevuId)
                .orElseThrow(() -> new KayitBulunamadiHatasi("Randevu", randevuId));
    }

    @Transactional(readOnly = true)
    public List<Randevu> hastaRandevulari(Hasta hasta) {
        return randevuRepository.findByHastaIdOrderByBaslangicDesc(hasta.getId());
    }

    @Transactional(readOnly = true)
    public List<Randevu> calisanRandevulari(Calisan calisan, RandevuDurumu durum) {
        return randevuRepository.findByCalisanIdOrderByBaslangicAsc(calisan.getId()).stream()
                .filter(r -> durum == null || r.getDurum() == durum)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Randevu> kurumRandevulari(Long kurumId, RandevuDurumu durum) {
        return randevuRepository.findBySubeKurumIdOrderByBaslangicDesc(kurumId).stream()
                .filter(r -> durum == null || r.getDurum() == durum)
                .toList();
    }

    @Transactional(readOnly = true)
    public long bekleyenRandevuSayisi(Long kurumId) {
        return randevuRepository.countBySubeKurumIdAndDurum(kurumId, RandevuDurumu.BEKLEMEDE);
    }
}
