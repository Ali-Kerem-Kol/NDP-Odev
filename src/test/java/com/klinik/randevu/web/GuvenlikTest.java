package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.KurumRepository;
import com.klinik.randevu.repository.RandevuRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sayfa ve kayıt düzeyinde erişim denetimi:
 * oturumsuz kullanıcı, yanlış rol ve başkasına ait kayıt senaryoları.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuvenlikTest {

    private static final String HASTA_TC = "62601815964";
    private static final String HASTA2_TC = "18301661332";
    private static final String DR_AYSE = "ayse.demir@sakaryasaglik.com.tr";
    private static final String DR_MEHMET = "mehmet.ozturk@sakaryasaglik.com.tr";
    private static final String KLINIK_YONETICI = "elif.kaya@gulumsedis.com.tr";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RandevuRepository randevuRepository;
    @Autowired
    private KullaniciRepository kullaniciRepository;
    @Autowired
    private KurumRepository kurumRepository;

    // ---- Giriş ----

    @Test
    void hastaTcKimlikNoIleGirisYapar() throws Exception {
        mvc.perform(formLogin("/giris").userParameter("kimlik").passwordParam("sifre").user(HASTA_TC).password("Medora2026"))
                .andExpect(authenticated().withUsername(HASTA_TC))
                .andExpect(redirectedUrl("/panel"));
    }

    @Test
    void personelEpostaIleBuyukKucukHarfFarketmedenGirisYapar() throws Exception {
        mvc.perform(formLogin("/giris").userParameter("kimlik").passwordParam("sifre").user("Ayse.Demir@SakaryaSaglik.com.tr").password("Medora2026"))
                .andExpect(authenticated().withUsername(DR_AYSE));
    }

    @Test
    void yanlisSifreIleGirisYapilamaz() throws Exception {
        mvc.perform(formLogin("/giris").userParameter("kimlik").passwordParam("sifre").user(HASTA_TC).password("yanlis"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/giris?hata"));
    }

    // ---- Sayfa düzeyi ----

    @Test
    void oturumsuzKullaniciGirisSayfasinaYonlendirilir() throws Exception {
        for (String adres : new String[]{"/hasta", "/calisan", "/yonetici", "/admin", "/profil", "/panel"}) {
            mvc.perform(get(adres)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/giris"));
        }
    }

    @Test
    void herRolYalnizcaKendiPanelineGirer() throws Exception {
        mvc.perform(get("/yonetici").with(user(HASTA_TC).roles("HASTA"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user(HASTA_TC).roles("HASTA"))).andExpect(status().isForbidden());
        mvc.perform(get("/calisan").with(user(HASTA_TC).roles("HASTA"))).andExpect(status().isForbidden());
        mvc.perform(get("/hasta").with(user(DR_AYSE).roles("DOKTOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/yonetici").with(user(DR_AYSE).roles("DOKTOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user(KLINIK_YONETICI).roles("YONETICI"))).andExpect(status().isForbidden());

        mvc.perform(get("/hasta").with(user(HASTA_TC).roles("HASTA"))).andExpect(status().isOk());
        mvc.perform(get("/calisan").with(user(DR_AYSE).roles("DOKTOR"))).andExpect(status().isOk());
        mvc.perform(get("/profil").with(user(DR_AYSE).roles("DOKTOR"))).andExpect(status().isOk());
    }

    @Test
    void csrfJetonuOlmayanIstekReddedilir() throws Exception {
        Randevu randevu = hastaninBekleyenRandevusu();
        mvc.perform(post("/randevu/{id}/iptal", randevu.getId()).with(user(HASTA_TC).roles("HASTA")))
                .andExpect(status().isForbidden());
        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getDurum());
    }

    // ---- Kayıt düzeyi: başkasının verisi ----

    @Test
    void hastaBaskasininRandevusunuIptalEdemez() throws Exception {
        Randevu randevu = hastaninBekleyenRandevusu();
        mvc.perform(post("/randevu/{id}/iptal", randevu.getId()).with(user(HASTA2_TC).roles("HASTA")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("hata", "Bu randevuyu iptal etme yetkiniz yok."));
        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getDurum());
    }

    @Test
    void doktorBaskaDoktorunRandevusunuOnaylayamaz() throws Exception {
        Randevu randevu = hastaninBekleyenRandevusu();
        mvc.perform(post("/randevu/{id}/onayla", randevu.getId()).with(user(DR_MEHMET).roles("DOKTOR")).with(csrf()))
                .andExpect(flash().attribute("hata", "Bu randevu üzerinde işlem yetkiniz yok."));
        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getDurum());
    }

    @Test
    void hastaRandevusunuOnaylayamaz() throws Exception {
        Randevu randevu = hastaninBekleyenRandevusu();
        mvc.perform(post("/randevu/{id}/onayla", randevu.getId()).with(user(HASTA_TC).roles("HASTA")).with(csrf()))
                .andExpect(flash().attribute("hata", "Bu randevu üzerinde işlem yetkiniz yok."));
        assertEquals(RandevuDurumu.BEKLEMEDE, randevu.getDurum());
    }

    @Test
    void yoneticiBaskaKurumunSubesiniGoremez() throws Exception {
        Sube hastaneSubesi = kurumRepository.findAll().stream()
                .filter(k -> k.getAd().equals("Sakarya Sağlık Grubu")).findFirst().orElseThrow()
                .getSubeler().get(0);
        mvc.perform(get("/yonetici/subeler/{id}", hastaneSubesi.getId()).with(user(KLINIK_YONETICI).roles("YONETICI")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("hata"));
    }

    @Test
    void sahibiRandevusunuIptalEdebilir() throws Exception {
        Randevu randevu = hastaninBekleyenRandevusu();
        mvc.perform(post("/randevu/{id}/iptal", randevu.getId()).with(user(HASTA_TC).roles("HASTA")).with(csrf())
                        .param("neden", "Planım değişti"))
                .andExpect(flash().attribute("basari", "Randevu iptal edildi."));
        assertEquals(RandevuDurumu.IPTAL, randevu.getDurum());
    }

    private Randevu hastaninBekleyenRandevusu() {
        Hasta hasta = (Hasta) kullaniciRepository.findByGirisKimligi(HASTA_TC).orElseThrow();
        return randevuRepository.findByHastaIdOrderByBaslangicDesc(hasta.getId()).stream()
                .filter(r -> r.getDurum() == RandevuDurumu.BEKLEMEDE)
                .findFirst().orElseThrow();
    }
}
