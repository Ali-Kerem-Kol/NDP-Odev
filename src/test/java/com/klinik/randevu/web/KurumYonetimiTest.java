package com.klinik.randevu.web;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Yonetici;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.KurumTuru;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.KurumRepository;
import com.klinik.randevu.servis.RandevuServisi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sistem yöneticisinin kurum düzenleme, yayından kaldırma, silme ve yönetici hesabı işlemleri. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KurumYonetimiTest {

    private static final String ADMIN = "admin@medora.com.tr";
    private static final String HASTANE_YONETICI = "ahmet.yilmaz@sakaryasaglik.com.tr";
    private static final String KLINIK_YONETICI = "elif.kaya@gulumsedis.com.tr";
    private static final String HASTA = "62601815964";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private KurumRepository kurumRepository;
    @Autowired
    private KullaniciRepository kullaniciRepository;
    @Autowired
    private RandevuServisi randevuServisi;

    // ---- Düzenleme ----

    @Test
    void sistemYoneticisiKurumBilgileriniDuzenler() throws Exception {
        Kurum klinik = kurum("Gülümse Diş Kliniği");
        mvc.perform(post("/admin/kurum/{id}/duzenle", klinik.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                        .param("ad", "Gülümse Ağız ve Diş Sağlığı").param("tur", "POLIKLINIK").param("aciklama", "  Yeni tanıtım  "))
                .andExpect(redirectedUrl("/#kurumlar"))
                .andExpect(flash().attributeExists("basari"));
        assertEquals("Gülümse Ağız ve Diş Sağlığı", klinik.getAd());
        assertEquals(KurumTuru.POLIKLINIK, klinik.getTur());
        assertEquals("Yeni tanıtım", klinik.getAciklama());
    }

    @Test
    void kurumYoneticisiYalnizcaKendiKurumunuDuzenler() throws Exception {
        Kurum hastane = kurum("Sakarya Sağlık Grubu");
        mvc.perform(post("/yonetici/kurum").with(user(HASTANE_YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "Sakarya Sağlık Grubu").param("tur", "HASTANE").param("aciklama", "İki şubeli hastane."))
                .andExpect(redirectedUrl("/yonetici"));
        assertEquals("İki şubeli hastane.", hastane.getAciklama());

        // Başka kurumun adresine istek: admin uç noktası rol nedeniyle kapalı
        Kurum klinik = kurum("Gülümse Diş Kliniği");
        mvc.perform(post("/admin/kurum/{id}/duzenle", klinik.getId()).with(user(HASTANE_YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "Ele geçirildi").param("tur", "KLINIK"))
                .andExpect(status().isForbidden());
        assertEquals("Gülümse Diş Kliniği", klinik.getAd());
    }

    @Test
    void bosKurumAdiReddedilir() throws Exception {
        Kurum klinik = kurum("Gülümse Diş Kliniği");
        mvc.perform(post("/admin/kurum/{id}/duzenle", klinik.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                        .param("ad", " ").param("tur", "KLINIK"))
                .andExpect(flash().attribute("hata", "Kurum adı boş olamaz."));
    }

    // ---- Yayından kaldırma ----

    @Test
    void yayindanKaldirilanKurumHerkeseKapaliYeniRandevuAlmazYoneticiGorur() throws Exception {
        Kurum hastane = kurum("Sakarya Sağlık Grubu");
        mvc.perform(post("/admin/kurum/{id}/yayin", hastane.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attribute("basari", containsString("yayından kaldırıldı")));
        assertFalse(hastane.isAktif());

        mvc.perform(get("/")).andExpect(content().string(not(containsString("Sakarya Sağlık Grubu"))));
        mvc.perform(get("/hasta/randevu-al").with(user(HASTA).roles("HASTA")))
                .andExpect(content().string(not(containsString("Sakarya Sağlık Grubu"))));
        mvc.perform(get("/").with(user(ADMIN).roles("SISTEM_YONETICISI")))
                .andExpect(content().string(containsString("Sakarya Sağlık Grubu")))
                .andExpect(content().string(containsString("Yayında değil")));
        mvc.perform(get("/yonetici").with(user(HASTANE_YONETICI).roles("YONETICI")))
                .andExpect(content().string(containsString("Kurumunuz şu an yayında değil")));

        Hasta hasta = (Hasta) kullaniciRepository.findByGirisKimligi(HASTA).orElseThrow();
        Calisan drAyse = (Calisan) kullaniciRepository.findByGirisKimligi("ayse.demir@sakaryasaglik.com.tr").orElseThrow();
        LocalDate gun = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        IsKuraliIhlali hata = assertThrows(IsKuraliIhlali.class, () -> randevuServisi.randevuOlustur(
                hasta, drAyse.getId(), drAyse.getHizmetler().get(0).getId(), gun.atTime(10, 0), null));
        assertTrue(hata.getMessage().contains("yeni randevu kabul etmiyor"));

        mvc.perform(post("/admin/kurum/{id}/yayin", hastane.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attribute("basari", containsString("yeniden yayında")));
        assertTrue(hastane.isAktif());
        assertDoesNotThrow(() -> randevuServisi.randevuOlustur(
                hasta, drAyse.getId(), drAyse.getHizmetler().get(0).getId(), gun.atTime(10, 0), null));
    }

    @Test
    void kurumuYalnizcaSistemYoneticisiYayindanKaldirirVeSiler() throws Exception {
        Kurum hastane = kurum("Sakarya Sağlık Grubu");
        mvc.perform(post("/admin/kurum/{id}/yayin", hastane.getId()).with(user(HASTANE_YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/kurum/{id}/sil", hastane.getId()).with(user(HASTA).roles("HASTA")).with(csrf()))
                .andExpect(status().isForbidden());
        assertTrue(hastane.isAktif());
    }

    // ---- Silme ----

    @Test
    void randevuGecmisiOlanKurumSilinmez() throws Exception {
        Kurum hastane = kurum("Sakarya Sağlık Grubu");
        mvc.perform(post("/admin/kurum/{id}/sil", hastane.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attribute("hata", containsString("randevu kayıtları var")));
        assertTrue(kurumRepository.existsById(hastane.getId()));
    }

    @Test
    void kullanilmamisKurumYoneticisiyleBirlikteSilinir() throws Exception {
        mvc.perform(post("/admin/kurum").with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                .param("ad", "Yanlış Açılan Klinik").param("tur", "KLINIK").param("ilkSubeAdi", "Merkez"));
        Kurum yeni = kurum("Yanlış Açılan Klinik");
        mvc.perform(post("/admin/yonetici").with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                .param("kurumId", yeni.getId().toString()).param("adSoyad", "Geçici Yönetici")
                .param("eposta", "gecici@yanlis.com.tr").param("sifre", "Medora2026"));
        assertTrue(kullaniciRepository.existsByGirisKimligi("gecici@yanlis.com.tr"));

        mvc.perform(post("/admin/kurum/{id}/sil", yeni.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attribute("basari", "'Yanlış Açılan Klinik' kurumu silindi."));
        assertFalse(kurumRepository.existsById(yeni.getId()));
        assertFalse(kullaniciRepository.existsByGirisKimligi("gecici@yanlis.com.tr"), "yönetici hesabı da silinir");
    }

    // ---- Yönetici hesabı ----

    @Test
    void kapatilanYoneticiGirisYapamazYenidenAcilinca() throws Exception {
        Yonetici elif = (Yonetici) kullaniciRepository.findByGirisKimligi(KLINIK_YONETICI).orElseThrow();
        mvc.perform(post("/admin/yonetici/{id}/hesap", elif.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attribute("basari", containsString("hesabı kapatıldı")));
        assertFalse(elif.isAktif());
        mvc.perform(formLogin("/giris").userParameter("kimlik").passwordParam("sifre").user(KLINIK_YONETICI).password("Medora2026"))
                .andExpect(unauthenticated());
        mvc.perform(get("/yonetici").with(user(KLINIK_YONETICI).roles("YONETICI")))
                .andExpect(redirectedUrl("/giris?pasif"));
        mvc.perform(get("/").with(user(ADMIN).roles("SISTEM_YONETICISI")))
                .andExpect(content().string(containsString("hesap kapalı")))
                .andExpect(content().string(containsString("Hesabı aç")));

        mvc.perform(post("/admin/yonetici/{id}/hesap", elif.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attribute("basari", containsString("giriş yapabilir")));
        assertTrue(elif.isAktif());
    }

    @Test
    void hastaninHesabiYoneticiUcNoktasiylaKapatilamaz() throws Exception {
        Long hastaId = kullaniciRepository.findByGirisKimligi(HASTA).orElseThrow().getId();
        mvc.perform(post("/admin/yonetici/{id}/hesap", hastaId).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertTrue(kullaniciRepository.findById(hastaId).orElseThrow().isAktif());
    }

    private Kurum kurum(String ad) {
        return kurumRepository.findAll().stream().filter(k -> k.getAd().equals(ad)).findFirst().orElseThrow();
    }
}
