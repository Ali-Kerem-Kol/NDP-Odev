package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.web.form.HastaKayitFormu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class KullaniciServisiTest {

    @Autowired
    private KullaniciServisi kullaniciServisi;
    @Autowired
    private KullaniciRepository kullaniciRepository;
    @Autowired
    private PasswordEncoder sifreleyici;

    @Test
    void hastaTcKimlikNoIleKaydolurVeSifresiOzetlenerekSaklanir() {
        Hasta hasta = kullaniciServisi.hastaKaydet(form("28609139020"));

        assertEquals("28609139020", hasta.getGirisKimligi());
        assertNotEquals("GuvenliSifre1", hasta.getSifre());
        assertTrue(sifreleyici.matches("GuvenliSifre1", hasta.getSifre()));
    }

    @Test
    void ayniTcIleIkinciHesapAcilamaz() {
        IsKuraliIhlali hata = assertThrows(IsKuraliIhlali.class, () -> kullaniciServisi.hastaKaydet(form("62601815964")));
        assertTrue(hata.getMessage().contains("yeni hesap oluşturulamadı"));
    }

    @Test
    void gecersizTcIleKayitYapilamaz() {
        assertThrows(IsKuraliIhlali.class, () -> kullaniciServisi.hastaKaydet(form("12345678901")));
    }

    @Test
    void personelEpostasiniDegistirinceGirisKimligiDeDegisir() {
        Kullanici doktor = kullanici("ayse.demir@sakaryasaglik.com.tr");
        kullaniciServisi.iletisimBilgisiGuncelle(doktor, "Ayse.Demir2@SakaryaSaglik.com.tr", "0264 000 00 00");

        assertEquals("ayse.demir2@sakaryasaglik.com.tr", doktor.getGirisKimligi());
        assertTrue(kullaniciRepository.findByGirisKimligi("ayse.demir2@sakaryasaglik.com.tr").isPresent());
    }

    @Test
    void personelBaskasininEpostasiniAlamaz() {
        Kullanici doktor = kullanici("ayse.demir@sakaryasaglik.com.tr");
        assertThrows(IsKuraliIhlali.class,
                () -> kullaniciServisi.iletisimBilgisiGuncelle(doktor, "mehmet.ozturk@sakaryasaglik.com.tr", null));
    }

    @Test
    void hastaEpostasiniDegistirinceGirisKimligiTcOlarakKalir() {
        Kullanici hasta = kullanici("62601815964");
        kullaniciServisi.iletisimBilgisiGuncelle(hasta, "yeni@ornekposta.com", null);

        assertEquals("62601815964", hasta.getGirisKimligi());
        assertEquals("yeni@ornekposta.com", hasta.getEposta());
    }

    @Test
    void sifreDegistirmekIcinMevcutSifreDogruOlmali() {
        Kullanici hasta = kullanici("62601815964");

        assertThrows(IsKuraliIhlali.class,
                () -> kullaniciServisi.sifreDegistir(hasta, "yanlis-sifre", "YeniSifre2026", "YeniSifre2026"));
        assertThrows(IsKuraliIhlali.class,
                () -> kullaniciServisi.sifreDegistir(hasta, "Medora2026", "YeniSifre2026", "Farkli2026"));

        kullaniciServisi.sifreDegistir(hasta, "Medora2026", "YeniSifre2026", "YeniSifre2026");
        assertTrue(sifreleyici.matches("YeniSifre2026", hasta.getSifre()));
    }

    private HastaKayitFormu form(String tc) {
        HastaKayitFormu form = new HastaKayitFormu();
        form.setTcKimlikNo(tc);
        form.setAdSoyad("Selin Er");
        form.setDogumTarihi(LocalDate.of(2000, 1, 1));
        form.setTelefon("0555 111 22 33");
        form.setSifre("GuvenliSifre1");
        form.setSifreTekrar("GuvenliSifre1");
        form.setKvkkOnay(true);
        return form;
    }

    private Kullanici kullanici(String kimlik) {
        return kullaniciRepository.findByGirisKimligi(kimlik).orElseThrow();
    }
}
