package com.klinik.randevu.web;

import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.repository.DuyuruRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Ana sayfa, haberler ve içerik yönetimi yetkileri. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IcerikTest {

    private static final String ADMIN = "admin@medora.com.tr";
    private static final String HASTANE_YONETICI = "ahmet.yilmaz@sakaryasaglik.com.tr";
    private static final String KLINIK_YONETICI = "elif.kaya@gulumsedis.com.tr";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DuyuruRepository duyuruRepository;

    @Test
    void anaSayfaOneCikanIcerikVeKurumlariGosterir() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Online randevu sistemimiz yenilendi")))
                .andExpect(content().string(containsString("Öne çıkan")))
                .andExpect(content().string(containsString("id=\"kurumlar\"")))
                .andExpect(content().string(not(containsString("class=\"bilgi-bandi\""))))
                .andExpect(content().string(not(containsString("Bayram tatilinde"))));
    }

    @Test
    void oturumAcmisKullaniciDaAnaSayfayiGorur() throws Exception {
        mvc.perform(get("/").with(user("62601815964").roles("HASTA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Randevularım")))
                .andExpect(content().string(not(containsString("Panel"))));
    }

    @Test
    void taslakHerkeseKapaliYoneticisineAcik() throws Exception {
        Duyuru taslak = duyuru("Bayram tatilinde çalışma saatleri");
        mvc.perform(get("/haberler/{id}", taslak.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/haberler/{id}", taslak.getId()).with(user(KLINIK_YONETICI).roles("YONETICI")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/haberler/{id}", taslak.getId()).with(user(HASTANE_YONETICI).roles("YONETICI")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("taslak")));
    }

    @Test
    void haberlerKategoriyeGoreSuzulur() throws Exception {
        mvc.perform(get("/haberler").param("kategori", "SAGLIK_REHBERI"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kalp sağlığı")))
                .andExpect(content().string(not(containsString("Grip aşısı"))));
    }

    @Test
    void hastaVeDoktorIcerikYonetimineGiremez() throws Exception {
        mvc.perform(get("/icerik/yeni").with(user("62601815964").roles("HASTA"))).andExpect(status().isForbidden());
        mvc.perform(get("/icerik/yeni").with(user("ayse.demir@sakaryasaglik.com.tr").roles("DOKTOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/site/karsilama").with(user(HASTANE_YONETICI).roles("YONETICI")).with(csrf())
                        .param("kahramanBaslik", "Ele geçirildi").param("kahramanMetni", "x"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/haberler").with(user("ayse.demir@sakaryasaglik.com.tr").roles("DOKTOR")))
                .andExpect(content().string(not(containsString("/icerik/"))));
    }

    @Test
    void kurumYoneticisiYalnizcaKendiKurumununIceriginiGorurVeDuzenler() throws Exception {
        // Duyurular sayfası herkese aynıdır; düzenleme düğmeleri yalnızca kendi kurumunun içeriğinde çıkar
        Duyuru kendi = duyuru("Çocuklarda diş fırçalama alışkanlığı nasıl kazandırılır?");
        Duyuru baskaKurumun = duyuru("Grip aşısı randevuları başladı");
        mvc.perform(get("/haberler").with(user(KLINIK_YONETICI).roles("YONETICI")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grip aşısı")))
                .andExpect(content().string(containsString("/icerik/" + kendi.getId() + "/duzenle")))
                .andExpect(content().string(not(containsString("/icerik/" + baskaKurumun.getId() + "/duzenle"))));

        mvc.perform(post("/icerik/{id}/sil", baskaKurumun.getId()).with(user(KLINIK_YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attribute("hata", "Bu içeriği düzenleme yetkiniz yok."));
        assertTrue(duyuruRepository.findById(baskaKurumun.getId()).isPresent());
    }

    @Test
    void kurumYoneticisininIcerigiHerZamanKendiKurumunaAitOlur() throws Exception {
        Long baskaKurumId = duyuru("Grip aşısı randevuları başladı").getKurum().getId();
        mvc.perform(post("/icerik/yeni").with(user(KLINIK_YONETICI).roles("YONETICI")).with(csrf())
                        .param("baslik", "Klinik duyurusu").param("ozet", "Kısa özet").param("icerik", "Metin")
                        .param("kategori", "DUYURU").param("kurumId", baskaKurumId.toString()).param("yayinla", "true"))
                .andExpect(redirectedUrlPattern("/haberler/*"));
        Duyuru yeni = duyuru("Klinik duyurusu");
        assertEquals("Gülümse Diş Kliniği", yeni.getKurum().getAd(), "başka kurum adına yazamaz");
        assertTrue(yeni.isYayinda());
    }

    @Test
    void oneCikanIcerigiYalnizcaSistemYoneticisiSecerVeTekTaneOlur() throws Exception {
        Duyuru grip = duyuru("Grip aşısı randevuları başladı");
        mvc.perform(post("/icerik/{id}/one-cikar", grip.getId()).with(user(HASTANE_YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attributeExists("hata"));

        mvc.perform(post("/icerik/{id}/one-cikar", grip.getId()).with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf()))
                .andExpect(flash().attributeExists("basari"));
        assertTrue(duyuru("Grip aşısı randevuları başladı").isOnecikan());
        assertFalse(duyuru("Online randevu sistemimiz yenilendi").isOnecikan(), "eski öne çıkan kalkar");
    }

    @Test
    void sistemYoneticisiAnaSayfaMetniniDegistirir() throws Exception {
        mvc.perform(post("/admin/site/karsilama").with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                        .param("kahramanBaslik", "Sağlığınız için doğru saat").param("kahramanMetni", "Yeni açıklama"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(post("/admin/site/bilgi-bandi").with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                        .header("Referer", "http://localhost/haberler").param("bilgiBandi", "Bayramda acil servis açıktır."))
                .andExpect(redirectedUrl("/haberler"));
        mvc.perform(get("/haberler")).andExpect(content().string(containsString("Bayramda acil servis açıktır.")));
        mvc.perform(post("/admin/site/bilgi-bandi").with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                        .header("Referer", "http://localhost/haberler").param("bilgiBandi", ""))
                .andExpect(redirectedUrl("/haberler"))
                .andExpect(flash().attribute("basari", "Bilgi bandı kaldırıldı."));
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Sağlığınız için doğru saat")))
                .andExpect(content().string(not(containsString("Bayramda acil servis açıktır."))));
    }

    @Test
    void sistemYoneticisiDuzenlemeDugmeleriniHerkesinGorduguSayfadaGorur() throws Exception {
        mvc.perform(get("/").with(user(ADMIN).roles("SISTEM_YONETICISI")))
                .andExpect(content().string(containsString("Karşılama metnini düzenle")))
                .andExpect(content().string(containsString("Kurum ekle")))
                .andExpect(content().string(containsString("Kurum yöneticileri")))
                .andExpect(content().string(containsString("action=\"/admin/site/iletisim\"")));
        for (var kim : new org.springframework.test.web.servlet.request.RequestPostProcessor[]{
                user(HASTANE_YONETICI).roles("YONETICI"), user("62601815964").roles("HASTA")}) {
            mvc.perform(get("/").with(kim))
                    .andExpect(content().string(not(containsString("Karşılama metnini düzenle"))))
                    .andExpect(content().string(not(containsString("/admin/"))));
        }
        mvc.perform(get("/"))
                .andExpect(content().string(not(containsString("/admin/"))))
                .andExpect(content().string(containsString("data-tema-degistir")));
    }

    @Test
    void iletisimBilgisindeGecersizEpostaReddedilir() throws Exception {
        mvc.perform(post("/admin/site/iletisim").with(user(ADMIN).roles("SISTEM_YONETICISI")).with(csrf())
                        .header("Referer", "http://localhost/").param("telefon", "0850 000 00 00").param("eposta", "gecersiz"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("hata"));
        mvc.perform(get("/")).andExpect(content().string(containsString("destek@medora.com.tr")));
    }

    private Duyuru duyuru(String baslik) {
        return duyuruRepository.findAll().stream().filter(d -> d.getBaslik().equals(baslik)).findFirst().orElseThrow();
    }
}
