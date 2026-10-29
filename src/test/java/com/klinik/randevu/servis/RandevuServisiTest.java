package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.repository.KullaniciRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Örnek verilerle (OrnekVeriYukleyici) çalışan entegrasyon testleri.
 * Her test kendi işleminde çalışır ve sonunda geri alınır.
 */
@SpringBootTest
@Transactional
class RandevuServisiTest {

    @Autowired
    private RandevuServisi randevuServisi;
    @Autowired
    private KullaniciRepository kullaniciRepository;

    private Hasta hasta;
    private Hasta hasta2;
    private Calisan drAyse;
    private Calisan drMehmet;
    private Hizmet genelMuayene;
    /** İki hafta sonraki Çarşamba: örnek randevularla karışmaz. */
    private LocalDate gun;

    @BeforeEach
    void hazirla() {
        hasta = (Hasta) kullanici("62601815964");
        hasta2 = (Hasta) kullanici("18301661332");
        drAyse = (Calisan) kullanici("ayse.demir@sakaryasaglik.com.tr");
        drMehmet = (Calisan) kullanici("mehmet.ozturk@sakaryasaglik.com.tr");
        genelMuayene = drAyse.getHizmetler().get(0);
        gun = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
    }

    @Test
    void gecerliRandevuOnayBekliyorDurumundaOlusturulur() {
        Randevu randevu = olustur(hasta, drAyse, gun.atTime(10, 0));

        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getDurum());
        assertEquals(gun.atTime(10, 20), randevu.getBitis());
        assertEquals(genelMuayene.getUcret(), randevu.getUcret());
        assertEquals(drAyse.getSube().getId(), randevu.getSube().getId());
    }

    @Test
    void ayniCalisanaCakisanRandevuVerilmez() {
        olustur(hasta, drAyse, gun.atTime(10, 0));

        IsKuraliIhlali hata = assertThrows(IsKuraliIhlali.class, () -> olustur(hasta2, drAyse, gun.atTime(10, 10)));
        assertTrue(hata.getMessage().contains("dolu"));
        // Bitişik randevu serbest
        assertDoesNotThrow(() -> olustur(hasta2, drAyse, gun.atTime(10, 20)));
    }

    @Test
    void reddedilenRandevununSaatiTekrarAlinabilir() {
        Randevu randevu = olustur(hasta, drAyse, gun.atTime(10, 0));
        randevuServisi.reddet(randevu.getId(), drAyse, "Uygun değil");

        assertDoesNotThrow(() -> olustur(hasta2, drAyse, gun.atTime(10, 0)));
    }

    @Test
    void hastaAyniAndaIkiRandevuAlamaz() {
        olustur(hasta, drAyse, gun.atTime(10, 0));
        Hizmet ekg = drMehmet.getHizmetler().stream().filter(h -> h.getAd().equals("EKG")).findFirst().orElseThrow();

        assertThrows(IsKuraliIhlali.class,
                () -> randevuServisi.randevuOlustur(hasta, drMehmet.getId(), ekg.getId(), gun.atTime(10, 10), null));
    }

    @Test
    void uygunlukDisindakiSaateRandevuAlinamaz() {
        // Öğle arası 12:00-13:00
        assertThrows(IsKuraliIhlali.class, () -> olustur(hasta, drAyse, gun.atTime(12, 10)));
        // Dilimin sonunu aşan randevu
        assertThrows(IsKuraliIhlali.class, () -> olustur(hasta, drAyse, gun.atTime(11, 50)));
        // Pazar günü
        LocalDate pazar = gun.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        assertThrows(IsKuraliIhlali.class, () -> olustur(hasta, drAyse, pazar.atTime(10, 0)));
    }

    @Test
    void calisanVermedigiHizmeteRandevuAlinamaz() {
        assertThrows(IsKuraliIhlali.class, () -> olustur(hasta, drMehmet, gun.atTime(10, 0)));
    }

    @Test
    void gecmisZamanaRandevuAlinamaz() {
        LocalDate gecen = LocalDate.now().minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        assertThrows(IsKuraliIhlali.class, () -> olustur(hasta, drAyse, gecen.atTime(10, 0)));
    }

    @Test
    void uygunSaatlerDoluSaatleriVeOgleArasiniIcermez() {
        olustur(hasta2, drAyse, gun.atTime(9, 0));
        List<LocalTime> saatler = randevuServisi.uygunSaatler(hasta, drAyse.getId(), genelMuayene.getId(), gun);

        // 20 dakikalık hizmet: saatler 20 dakika aralıklı üretilir
        assertFalse(saatler.contains(LocalTime.of(9, 0)), "09:00 dolu");
        assertTrue(saatler.contains(LocalTime.of(9, 20)));
        assertTrue(saatler.contains(LocalTime.of(11, 40)), "11:40 + 20 dk = 12:00, dilimin sonu");
        assertFalse(saatler.contains(LocalTime.of(12, 0)), "öğle arası");
        assertTrue(saatler.contains(LocalTime.of(13, 0)));
        assertTrue(saatler.stream().allMatch(s -> s.getMinute() % 20 == 0));
    }

    @Test
    void onayYetkisiRoleGoreBelirlenir() {
        Randevu randevu = olustur(hasta, drAyse, gun.atTime(10, 0));

        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.onayla(randevu.getId(), hasta));
        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.onayla(randevu.getId(), drMehmet));
        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.onayla(randevu.getId(), kullanici("elif.kaya@gulumsedis.com.tr")));

        randevuServisi.onayla(randevu.getId(), kullanici("ahmet.yilmaz@sakaryasaglik.com.tr"));
        assertEquals(RandevuDurumu.ONAYLANDI, randevu.getDurum());
    }

    @Test
    void hastaYalnizcaKendiRandevusunuIptalEdebilir() {
        Randevu randevu = olustur(hasta, drAyse, gun.atTime(10, 0));

        assertThrows(YetkisizIslemHatasi.class, () -> randevuServisi.iptalEt(randevu.getId(), hasta2, null));
        randevuServisi.iptalEt(randevu.getId(), hasta, "Planım değişti");
        assertEquals(RandevuDurumu.IPTAL, randevu.getDurum());
    }

    @Test
    void gelecektekiRandevuTamamlanamaz() {
        Randevu randevu = olustur(hasta, drAyse, gun.atTime(10, 0));
        randevuServisi.onayla(randevu.getId(), drAyse);

        assertThrows(IsKuraliIhlali.class, () -> randevuServisi.tamamla(randevu.getId(), drAyse));
    }

    private Randevu olustur(Hasta h, Calisan c, LocalDateTime zaman) {
        return randevuServisi.randevuOlustur(h, c.getId(), genelMuayene.getId(), zaman, null);
    }

    private Kullanici kullanici(String kullaniciAdi) {
        return kullaniciRepository.findByGirisKimligi(kullaniciAdi).orElseThrow();
    }
}
