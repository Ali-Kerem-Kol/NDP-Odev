package com.klinik.randevu.model;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Doktor;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kullanici.SaglikPersoneli;
import com.klinik.randevu.model.kurum.HizmetTuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.KurumTuru;
import com.klinik.randevu.model.kurum.Sube;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Veritabanı olmadan alan nesnelerinin kurallarını sınar. */
class AlanModeliTest {

    private final LocalDate pazartesi = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Test
    void subeYalnizcaCalismaSaatleriIcindeAciktir() {
        Sube sube = new Kurum("Test", KurumTuru.KLINIK, null).subeEkle("Merkez", null, null);
        sube.calismaSaatiAyarla(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertTrue(sube.acikMi(pazartesi.atTime(9, 0), pazartesi.atTime(9, 30)));
        assertTrue(sube.acikMi(pazartesi.atTime(16, 30), pazartesi.atTime(17, 0)));
        assertFalse(sube.acikMi(pazartesi.atTime(16, 45), pazartesi.atTime(17, 15)));
        assertFalse(sube.acikMi(pazartesi.plusDays(1).atTime(10, 0), pazartesi.plusDays(1).atTime(10, 30)));
    }

    @Test
    void ayniGunIcinCalismaSaatiYenidenAyarlanabilir() {
        Sube sube = new Kurum("Test", KurumTuru.KLINIK, null).subeEkle("Merkez", null, null);
        sube.calismaSaatiAyarla(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        sube.calismaSaatiAyarla(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0));

        assertEquals(1, sube.getCalismaSaatleri().size());
        assertEquals(LocalTime.of(10, 0), sube.getCalismaSaatleri().get(0).getAcilis());
    }

    @Test
    void hataliSaatAraligiReddedilir() {
        Sube sube = new Kurum("Test", KurumTuru.KLINIK, null).subeEkle("Merkez", null, null);
        assertThrows(IsKuraliIhlali.class,
                () -> sube.calismaSaatiAyarla(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(9, 0)));
    }

    @Test
    void hizmetSuresiVeUcretiDogrulanir() {
        Kurum kurum = new Kurum("Test", KurumTuru.KLINIK, null);
        assertThrows(IsKuraliIhlali.class,
                () -> kurum.hizmetEkle("Muayene", HizmetTuru.GENEL_MUAYENE, 0, BigDecimal.TEN, null));
        assertThrows(IsKuraliIhlali.class,
                () -> kurum.hizmetEkle("Muayene", HizmetTuru.GENEL_MUAYENE, 20, BigDecimal.valueOf(-1), null));
    }

    @Test
    void cakisanUygunlukDilimiEklenemez() {
        Doktor doktor = new Doktor("test@ornek.com", "x", "Test Doktor", "Dahiliye");
        doktor.uygunlukEkle(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThrows(IsKuraliIhlali.class,
                () -> doktor.uygunlukEkle(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0)));
        // Bitişik dilim çakışma sayılmaz
        assertDoesNotThrow(() -> doktor.uygunlukEkle(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(13, 0)));
        // Farklı gün serbest
        assertDoesNotThrow(() -> doktor.uygunlukEkle(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
    }

    @Test
    void calisanYalnizcaUygunlukDilimiIcindeMusaittir() {
        Doktor doktor = new Doktor("test@ornek.com", "x", "Test Doktor", "Dahiliye");
        doktor.uygunlukEkle(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));

        LocalDateTime on = pazartesi.atTime(10, 0);
        assertTrue(doktor.musaitMi(on, on.plusMinutes(30)));
        assertFalse(doktor.musaitMi(pazartesi.atTime(11, 45), pazartesi.atTime(12, 15)));
        assertFalse(doktor.musaitMi(on.plusDays(1), on.plusDays(1).plusMinutes(30)));
    }

    @Test
    void cokBicimlilik_herKullaniciTuruKendiDavranisiniSergiler() {
        List<Kullanici> kullanicilar = List.of(
                new Doktor("ayse@ornek.com", "x", "Ayşe Demir", "Kardiyoloji"),
                new SaglikPersoneli("ali@ornek.com", "x", "Ali Çelik", "Radyoloji Teknisyeni"));

        assertEquals("Dr. Ayşe Demir", kullanicilar.get(0).getGorunenAd());
        assertEquals("Ali Çelik", kullanicilar.get(1).getGorunenAd());
        assertEquals("Kardiyoloji Uzmanı", ((Calisan) kullanicilar.get(0)).getUnvan());
        assertEquals("Radyoloji Teknisyeni", ((Calisan) kullanicilar.get(1)).getUnvan());
        kullanicilar.forEach(k -> assertEquals("/calisan", k.getPanelYolu()));
    }

    @Test
    void tarihEtiketiYakinGunlerdeGunAdiUzakTarihlerdeTarihGosterir() {
        com.klinik.randevu.yardimci.TarihYardimcisi t = new com.klinik.randevu.yardimci.TarihYardimcisi();
        LocalDate bugun = LocalDate.now();
        assertEquals("Bugün", t.tarihEtiketi(bugun));
        assertEquals("Yarın", t.tarihEtiketi(bugun.plusDays(1)));
        assertEquals("Dün", t.tarihEtiketi(bugun.minusDays(1)));
        assertEquals(com.klinik.randevu.yardimci.TarihYardimcisi.gunAdiniGetir(bugun.plusDays(3).getDayOfWeek()),
                t.tarihEtiketi(bugun.plusDays(3)));
        assertEquals(t.gunAy(bugun.minusDays(5)), t.tarihEtiketi(bugun.minusDays(5)), "eski tarih gün adıyla değil tarihle gösterilir");
        assertEquals(t.gunAy(bugun.plusDays(10)), t.tarihEtiketi(bugun.plusDays(10)));
    }
}
