package com.klinik.randevu.model;

import com.klinik.randevu.model.kullanici.*;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.KurumTuru;
import com.klinik.randevu.model.kurum.Sube;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nesneye dayalı tasarımın kendisini sınar: aynı mesaja her kullanıcı türünün kendi cevabını vermesi
 * (çok biçimlilik), ortak üst sınıftan gelen eşitlik sözleşmesi (kalıtım) ve kimliğin dışarıdan
 * değiştirilememesi (kapsülleme).
 */
class NdpIlkeleriTest {

    private final Kurum kurum = new Kurum("Test Hastanesi", KurumTuru.HASTANE, null);
    private final Sube sube = kurum.subeEkle("Merkez", null, null);

    private final Hasta hasta = new Hasta("62601815964", "x", "Deniz Aydın", LocalDate.of(1995, 4, 12));
    private final Doktor doktor = calisan(new Doktor("ayse@test.com", "x", "Ayşe Demir", "İç Hastalıkları"));
    private final SaglikPersoneli teknisyen = calisan(new SaglikPersoneli("ali@test.com", "x", "Ali Çelik", "Radyoloji Teknisyeni"));
    private final Yonetici yonetici = new Yonetici("ahmet@test.com", "x", "Ahmet Yılmaz", kurum);
    private final SistemYoneticisi admin = new SistemYoneticisi("admin@test.com", "x", "Sistem Yöneticisi");

    // ---- Çok biçimlilik: aynı soru, türe göre farklı cevap ----

    @Test
    void herKullaniciTuruKendiPanelYolunuVerir() {
        List<Kullanici> kullanicilar = List.of(hasta, doktor, teknisyen, yonetici, admin);
        List<String> yollar = kullanicilar.stream().map(Kullanici::getPanelYolu).toList();
        assertEquals(List.of("/hasta", "/calisan", "/calisan", "/yonetici", "/"), yollar);
    }

    @Test
    void hesapMenusunuKullaniciNesnesiUretir() {
        assertEquals(List.of("Randevularım", "Randevu al"), etiketler(hasta.getHesapMenusu()));
        assertEquals(List.of("Randevularım", "Çalışma saatlerim"), etiketler(doktor.getHesapMenusu()));
        assertEquals(List.of("Kurum yönetimi", "Kurum duyuruları"), etiketler(yonetici.getHesapMenusu()));
        assertEquals(List.of("Kurumlar ve yöneticiler", "Duyuruları yönet"), etiketler(admin.getHesapMenusu()));
    }

    @Test
    void calismaSekmeleriYalnizcaPersonelVeKurumYoneticisindeVardir() {
        assertTrue(hasta.getCalismaSekmeleri().isEmpty());
        assertTrue(admin.getCalismaSekmeleri().isEmpty());
        assertEquals(5, yonetici.getCalismaSekmeleri().size());
        assertTrue(yonetici.calismaSekmesiMi("calisanlar"));
        assertFalse(doktor.calismaSekmesiMi("calisanlar"));
        assertTrue(teknisyen.calismaSekmesiMi("calisma-saatleri"));
        assertEquals("Test Hastanesi", yonetici.getCalismaAlaniBasligi());
        assertEquals("Test Hastanesi - Merkez", doktor.getCalismaAlaniBasligi());
    }

    @Test
    void profilBilgileriTureGoreDegisir() {
        assertEquals("62*******64", hasta.getProfilBilgileri().get(0).deger(), "hasta kendi T.C.'sini de maskeli görür");
        assertTrue(hasta.getProfilBilgileri().get(1).deger().startsWith("12 Nisan 1995"));
        assertEquals("İç Hastalıkları Uzmanı · Test Hastanesi - Merkez", doktor.getProfilBilgileri().get(0).deger());
        assertEquals("Radyoloji Teknisyeni · Test Hastanesi - Merkez", teknisyen.getProfilBilgileri().get(0).deger());
        assertEquals("Test Hastanesi", yonetici.getProfilBilgileri().get(0).deger());
        assertTrue(admin.getProfilBilgileri().isEmpty());
    }

    @Test
    void yetkiSorulariniHerTurKendiCevaplar() {
        assertTrue(admin.kurumTanimlayabilirMi());
        for (Kullanici k : List.of(hasta, doktor, teknisyen, yonetici)) {
            assertFalse(k.kurumTanimlayabilirMi(), k.getRol() + " kurum tanımlayamaz");
        }
        assertTrue(hasta.randevuAlabilirMi());
        assertFalse(doktor.randevuAlabilirMi());
        assertTrue(yonetici.kurumuYonetebilirMi(kurum));
        assertFalse(yonetici.kurumuYonetebilirMi(new Kurum("Başka", KurumTuru.KLINIK, null)));
    }

    @Test
    void soyutCalisanTuruneGoreFarkliUnvanVerir() {
        List<Calisan> calisanlar = List.of(doktor, teknisyen);
        assertEquals(List.of("İç Hastalıkları Uzmanı", "Radyoloji Teknisyeni"), calisanlar.stream().map(Calisan::getUnvan).toList());
        assertEquals(List.of("Dr. Ayşe Demir", "Ali Çelik"), calisanlar.stream().map(Calisan::getGorunenAd).toList());
    }

    // ---- Kalıtım: TemelVarlik eşitlik sözleşmesi ----

    @Test
    void kaydedilmemisVarlikYalnizcaKendisineEsittir() {
        Kurum digeri = new Kurum("Test Hastanesi", KurumTuru.HASTANE, null);
        assertTrue(kurum.yeniMi());
        assertEquals(kurum, kurum);
        assertNotEquals(kurum, digeri, "aynı ada sahip olmak eşitlik için yetmez");
    }

    @Test
    void ayniTurVeAyniKimlikEsitlikDemektir() {
        Kurum ayniKayit = new Kurum("Adı farklı yüklenmiş", KurumTuru.KLINIK, null);
        ReflectionTestUtils.setField(kurum, "id", 7L);
        ReflectionTestUtils.setField(ayniKayit, "id", 7L);
        ReflectionTestUtils.setField(sube, "id", 7L);

        assertEquals(kurum, ayniKayit);
        assertEquals(kurum.hashCode(), ayniKayit.hashCode());
        assertNotEquals(kurum, sube, "kimlik aynı olsa da tür farklıysa eşit değildir");
        assertEquals("Kurum#7", kurum.toString());
    }

    @Test
    void kimlikAldiktanSonraKumedekiYeriDegismez() {
        Set<Kurum> kume = new HashSet<>(Set.of(kurum));
        ReflectionTestUtils.setField(kurum, "id", 42L);
        assertTrue(kume.contains(kurum), "hashCode kimliğe bağlı olsaydı nesne kümede kaybolurdu");
    }

    // ---- Kapsülleme ----

    @Test
    void kimlikDisaridanAtanamazYalnizcaOkunur() {
        boolean setterVar = java.util.Arrays.stream(TemelVarlik.class.getMethods())
                .anyMatch(m -> m.getName().equals("setId"));
        assertFalse(setterVar);
        assertThrows(UnsupportedOperationException.class, () -> kurum.getSubeler().add(sube),
                "koleksiyonlar değiştirilemez görünüm olarak verilir");
    }

    private <T extends Calisan> T calisan(T c) {
        c.gorevlendir(sube, null);
        return c;
    }

    private static List<String> etiketler(List<MenuBaglantisi> menu) {
        return menu.stream().map(MenuBaglantisi::etiket).toList();
    }
}
