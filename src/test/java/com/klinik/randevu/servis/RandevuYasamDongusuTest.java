package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.model.randevu.RandevuHareketi;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.RandevuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

/** Randevunun zamana bağlı durum geçişleri ve durum geçmişi. */
@SpringBootTest
@Transactional
class RandevuYasamDongusuTest {

    private static final Duration TOLERANS = Duration.ofHours(2);

    @Autowired
    private RandevuServisi randevuServisi;
    @Autowired
    private RandevuRepository randevuRepository;
    @Autowired
    private KullaniciRepository kullaniciRepository;

    private Hasta hasta;
    private Calisan drAyse;
    private Hizmet genelMuayene;
    private LocalDate gun;

    @BeforeEach
    void hazirla() {
        hasta = (Hasta) kullanici("62601815964");
        drAyse = (Calisan) kullanici("ayse.demir@sakaryasaglik.com.tr");
        genelMuayene = drAyse.getHizmetler().get(0);
        gun = LocalDate.now().plusWeeks(3).with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
    }

    @Test
    void saatiGelipOnaylanmayanTalepDuser() {
        Randevu randevu = olustur(gun.atTime(10, 0));

        randevuServisi.suresiDolanlariIsle(gun.atTime(9, 59), TOLERANS);
        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getDurum(), "saatten önce dokunulmaz");

        randevuServisi.suresiDolanlariIsle(gun.atTime(10, 0), TOLERANS);
        assertEquals(RandevuDurumu.ZAMAN_ASIMI, randevu.getDurum());
        assertFalse(randevu.getDurum().isAktif(), "düşen talep takvimde yer kaplamaz");
        assertEquals(RandevuHareketi.SISTEM, sonHareket(randevu).getYapan());
    }

    @Test
    void onayliRandevuToleransDolunaKadarAcikKalirSonraOtomatikTamamlanir() {
        Randevu randevu = olustur(gun.atTime(10, 0));
        randevuServisi.onayla(randevu.getId(), drAyse);

        // Bitiş 10:20; tolerans 2 saat -> 12:20'ye kadar personel "gelmedi" işaretleyebilir
        randevuServisi.suresiDolanlariIsle(gun.atTime(12, 19), TOLERANS);
        assertEquals(RandevuDurumu.ONAYLANDI, randevu.getDurum());

        randevuServisi.suresiDolanlariIsle(gun.atTime(12, 21), TOLERANS);
        assertEquals(RandevuDurumu.TAMAMLANDI, randevu.getDurum());
        assertEquals(RandevuHareketi.SISTEM, sonHareket(randevu).getYapan());
        assertTrue(sonHareket(randevu).getAciklama().contains("otomatik"));
    }

    @Test
    void kapananRandevularaZamanlayiciDokunmaz() {
        Randevu reddedilen = olustur(gun.atTime(10, 0));
        randevuServisi.reddet(reddedilen.getId(), drAyse, "Uygun değil");

        randevuServisi.suresiDolanlariIsle(gun.plusDays(1).atStartOfDay(), TOLERANS);
        assertEquals(RandevuDurumu.REDDEDILDI, reddedilen.getDurum());
        assertEquals(2, reddedilen.getHareketler().size(), "kapanmış randevuya yeni hareket eklenmez");
    }

    @Test
    void ornekVerideGecmisBekleyenVeOnayliRandevularIslenir() {
        RandevuServisi.ZamanIslemSonucu sonuc = randevuServisi.suresiDolanlariIsle(LocalDateTime.now(), TOLERANS);
        assertTrue(sonuc.zamanAsimi() >= 1, "örnek veride geçmiş bir onaysız talep var");
        assertTrue(sonuc.otomatikTamamlanan() >= 1, "örnek veride geçmiş bir onaylı randevu var");
    }

    @Test
    void gelmediYalnizcaRandevuSaatiGectiktenSonraIsaretlenir() {
        Randevu randevu = olustur(gun.atTime(10, 0));
        randevuServisi.onayla(randevu.getId(), drAyse);

        IsKuraliIhlali hata = assertThrows(IsKuraliIhlali.class, () -> randevuServisi.gelmedi(randevu.getId(), drAyse));
        assertTrue(hata.getMessage().contains("saati gelmeden"));
        assertFalse(randevu.isGelmediIsaretlenebilir());
    }

    @Test
    void herDurumDegisikligiKiminYaptigiylaGecmiseYazilir() {
        Randevu randevu = olustur(gun.atTime(10, 0));
        randevuServisi.onayla(randevu.getId(), drAyse);
        randevuServisi.iptalEt(randevu.getId(), hasta, "Şehir dışındayım");

        assertEquals(3, randevu.getHareketler().size());
        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getHareketler().get(0).getDurum());
        assertEquals(hasta.getAdSoyad(), randevu.getHareketler().get(0).getYapan());
        assertEquals("Dr. Ayşe Demir", randevu.getHareketler().get(1).getYapan());
        assertEquals("Şehir dışındayım", randevu.getHareketler().get(2).getAciklama());
    }

    @Test
    void randevuDetayiniYalnizcaIlgililerGorur() {
        Randevu randevu = olustur(gun.atTime(10, 0));

        assertDoesNotThrow(() -> randevuServisi.gorulebilirRandevu(randevu.getId(), hasta));
        assertDoesNotThrow(() -> randevuServisi.gorulebilirRandevu(randevu.getId(), drAyse));
        assertDoesNotThrow(() -> randevuServisi.gorulebilirRandevu(randevu.getId(), kullanici("ahmet.yilmaz@sakaryasaglik.com.tr")));
        assertDoesNotThrow(() -> randevuServisi.gorulebilirRandevu(randevu.getId(), kullanici("admin@medora.com.tr")));

        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.gorulebilirRandevu(randevu.getId(), kullanici("18301661332")));
        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.gorulebilirRandevu(randevu.getId(), kullanici("mehmet.ozturk@sakaryasaglik.com.tr")));
        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.gorulebilirRandevu(randevu.getId(), kullanici("elif.kaya@gulumsedis.com.tr")));
    }

    @Test
    void baslamisRandevuIptalEdilemezGelmediYaDaTamamlandiIsaretlenir() {
        LocalDate dun = LocalDate.now().minusDays(1);
        Randevu gecmis = randevuRepository.save(Randevu.gecmisKaydiAktar(hasta, drAyse, genelMuayene,
                dun.atTime(10, 0), null, dun.minusDays(2).atTime(9, 0)));
        gecmis.onayla(drAyse, dun.minusDays(1).atTime(9, 0));

        assertFalse(gecmis.isIptalEdilebilir(), "iptal düğmesi gösterilmemeli");
        IsKuraliIhlali hata = assertThrows(IsKuraliIhlali.class,
                () -> randevuServisi.iptalEt(gecmis.getId(), drAyse, "geç kaldı"));
        assertTrue(hata.getMessage().contains("Başlamış randevu iptal edilemez"));

        randevuServisi.gelmedi(gecmis.getId(), drAyse);
        assertEquals(RandevuDurumu.GELMEDI, gecmis.getDurum());
    }

    @Test
    void gecmisKayitAktariminda_zamanlarTutarliOlmali() {
        LocalDate dun = LocalDate.now().minusDays(1);
        assertThrows(IsKuraliIhlali.class, () -> Randevu.gecmisKaydiAktar(hasta, drAyse, genelMuayene,
                dun.atTime(10, 0), null, dun.atTime(11, 0)), "talep randevudan sonra olamaz");
        assertThrows(IsKuraliIhlali.class, () -> Randevu.gecmisKaydiAktar(hasta, drAyse, genelMuayene,
                gun.atTime(10, 0), null, LocalDateTime.now().plusDays(1)), "talep gelecekte olamaz");

        Randevu r = Randevu.gecmisKaydiAktar(hasta, drAyse, genelMuayene, dun.atTime(10, 0), null, dun.minusDays(2).atTime(9, 0));
        assertThrows(IsKuraliIhlali.class, () -> r.onayla(drAyse, dun.minusDays(3).atTime(9, 0)), "onay talepten önce olamaz");
        assertThrows(IsKuraliIhlali.class, () -> r.onayla(drAyse, LocalDateTime.now().plusHours(1)), "onay gelecekte olamaz");
    }

    @Test
    void ornekVerideGecmisRandevularinGecmisiKronolojiktir() {
        for (Randevu r : randevuRepository.findAll()) {
            assertFalse(r.getOlusturmaZamani().isAfter(r.getBaslangic()) && r.getBaslangic().isBefore(LocalDateTime.now().minusHours(3)),
                    "geçmiş randevunun talebi randevudan sonra görünmemeli: " + r.getId());
            for (int i = 1; i < r.getHareketler().size(); i++) {
                assertFalse(r.getHareketler().get(i).getZaman().isBefore(r.getHareketler().get(i - 1).getZaman()),
                        "hareketler zaman sırasında olmalı: randevu " + r.getId());
            }
        }
    }

    private Randevu olustur(LocalDateTime zaman) {
        return randevuServisi.randevuOlustur(hasta, drAyse.getId(), genelMuayene.getId(), zaman, null);
    }

    private RandevuHareketi sonHareket(Randevu randevu) {
        return randevu.getHareketler().get(randevu.getHareketler().size() - 1);
    }

    private Kullanici kullanici(String kimlik) {
        return kullaniciRepository.findByGirisKimligi(kimlik).orElseThrow();
    }
}
