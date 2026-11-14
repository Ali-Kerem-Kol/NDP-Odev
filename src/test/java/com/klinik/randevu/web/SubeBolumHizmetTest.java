package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kurum.Bolum;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.kurum.HizmetTuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.KurumRepository;
import com.klinik.randevu.repository.SubeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kurum yöneticisinin şube, bölüm ve hizmet düzenleme/kaldırma işlemleri ve hastanın doktor seçimi. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubeBolumHizmetTest {

    private static final String YONETICI = "ahmet.yilmaz@sakaryasaglik.com.tr";
    private static final String KLINIK_YONETICI = "elif.kaya@gulumsedis.com.tr";
    private static final String HASTA = "62601815964";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private KurumRepository kurumRepository;
    @Autowired
    private SubeRepository subeRepository;
    @Autowired
    private KullaniciRepository kullaniciRepository;

    // ---- Şube ----

    @Test
    void subeBilgileriDuzenlenir() throws Exception {
        Sube serdivan = sube("Serdivan Şubesi");
        mvc.perform(post("/yonetici/subeler/{id}/duzenle", serdivan.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "Serdivan Kampüsü").param("adres", "Yeni adres").param("telefon", " "))
                .andExpect(redirectedUrl("/yonetici/subeler/" + serdivan.getId()));
        assertEquals("Serdivan Kampüsü", serdivan.getAd());
        assertEquals("Yeni adres", serdivan.getAdres());
        assertNull(serdivan.getTelefon(), "boş telefon kaydedilmez");
    }

    @Test
    void personeliOlanSubeKaldirilamazBosSubeKaldirilir() throws Exception {
        Sube serdivan = sube("Serdivan Şubesi");
        mvc.perform(post("/yonetici/subeler/{id}/sil", serdivan.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attribute("hata", containsString("kayıtlı personel var")));

        mvc.perform(post("/yonetici/subeler").with(user(YONETICI).roles("YONETICI")).with(csrf()).param("ad", "Geçici Şube"));
        Sube gecici = sube("Geçici Şube");
        mvc.perform(post("/yonetici/subeler/{id}/sil", gecici.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(redirectedUrl("/yonetici/subeler"))
                .andExpect(flash().attribute("basari", "'Geçici Şube' şubesi kaldırıldı."));
        assertFalse(subeRepository.existsById(gecici.getId()));
    }

    @Test
    void kurumunTekSubesiKaldirilamaz() throws Exception {
        Kurum klinik = kurum("Gülümse Diş Kliniği");
        // Personeli başka yere taşımadan önce bile tek şube kuralı devreye girer mi diye boş bir kurum üzerinde denenir
        Sube tek = klinik.getSubeler().get(0);
        mvc.perform(post("/yonetici/subeler/{id}/sil", tek.getId()).with(user(KLINIK_YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attributeExists("hata"));
        assertTrue(subeRepository.existsById(tek.getId()));
    }

    @Test
    void baskaKurumunSubesiDuzenlenemez() throws Exception {
        Sube serdivan = sube("Serdivan Şubesi");
        mvc.perform(post("/yonetici/subeler/{id}/duzenle", serdivan.getId()).with(user(KLINIK_YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "Ele geçirildi"))
                .andExpect(flash().attribute("hata", containsString("yetkiniz yok")));
        assertEquals("Serdivan Şubesi", serdivan.getAd());
    }

    // ---- Bölüm ----

    @Test
    void bolumDuzenlenirAyniAdVerilemezPersoneliOlanBolumKaldirilamaz() throws Exception {
        Sube serdivan = sube("Serdivan Şubesi");
        Bolum dahiliye = bolum(serdivan, "Dahiliye");
        Bolum kardiyoloji = bolum(serdivan, "Kardiyoloji");

        mvc.perform(post("/yonetici/bolumler/{id}/duzenle", kardiyoloji.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "dahiliye"))
                .andExpect(flash().attribute("hata", containsString("zaten var")));
        mvc.perform(post("/yonetici/bolumler/{id}/duzenle", kardiyoloji.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "Kardiyoloji Polikliniği").param("aciklama", "Kalp ve damar"))
                .andExpect(flash().attribute("basari", "Bölüm güncellendi."));
        assertEquals("Kardiyoloji Polikliniği", kardiyoloji.getAd());

        mvc.perform(post("/yonetici/bolumler/{id}/sil", dahiliye.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attribute("hata", containsString("personel var")));

        mvc.perform(post("/yonetici/subeler/{id}/bolum", serdivan.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf())
                .param("ad", "Göz"));
        Bolum goz = bolum(serdivan, "Göz");
        mvc.perform(post("/yonetici/bolumler/{id}/sil", goz.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(redirectedUrl("/yonetici/subeler/" + serdivan.getId()));
        assertTrue(serdivan.getBolumler().stream().noneMatch(b -> b.getAd().equals("Göz")));
    }

    // ---- Hizmet ----

    @Test
    void hizmetinTumTanimiDuzenlenir() throws Exception {
        Hizmet ekg = hizmet("EKG");
        mvc.perform(post("/yonetici/hizmetler/{id}/guncelle", ekg.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", "Efor Testi").param("tur", "UZMAN_MUAYENESI").param("sureDakika", "30")
                        .param("ucret", "950").param("aciklama", "Kardiyoloji"))
                .andExpect(flash().attributeExists("basari"));
        assertEquals("Efor Testi", ekg.getAd());
        assertEquals(HizmetTuru.UZMAN_MUAYENESI, ekg.getTur());
        assertEquals(30, ekg.getSureDakika());
        assertEquals(0, new BigDecimal("950").compareTo(ekg.getUcret()));
        assertEquals("Kardiyoloji", ekg.getAciklama());
    }

    @Test
    void hizmetAdiBosBirakilamaz() throws Exception {
        Hizmet ekg = hizmet("EKG");
        mvc.perform(post("/yonetici/hizmetler/{id}/guncelle", ekg.getId()).with(user(YONETICI).roles("YONETICI")).with(csrf())
                        .param("ad", " ").param("tur", "UZMAN_MUAYENESI").param("sureDakika", "15").param("ucret", "400"))
                .andExpect(flash().attribute("hata", "Hizmet adı boş olamaz."));
        assertEquals("EKG", ekg.getAd());
    }

    // ---- Hastanın doktor seçimi ----

    @Test
    void hastaAyniHizmetiVerenDoktorlarArasindanSecimYapar() throws Exception {
        Sube serdivan = sube("Serdivan Şubesi");
        Hizmet genel = hizmet("Genel Muayene");
        Calisan ayse = calisan("ayse.demir@sakaryasaglik.com.tr");
        Calisan selin = calisan("selin.kurt@sakaryasaglik.com.tr");
        String adres = "/hasta/randevu-al?kurumId=" + serdivan.getKurum().getId() + "&subeId=" + serdivan.getId()
                + "&hizmetId=" + genel.getId();

        mvc.perform(get(adres).with(user(HASTA).roles("HASTA")))
                .andExpect(content().string(containsString("Tüm doktorlar")))
                .andExpect(content().string(containsString("id=\"doktor-" + ayse.getId() + "\"")))
                .andExpect(content().string(containsString("id=\"doktor-" + selin.getId() + "\"")));

        mvc.perform(get(adres + "&calisanId=" + selin.getId()).with(user(HASTA).roles("HASTA")))
                .andExpect(content().string(containsString("id=\"doktor-" + selin.getId() + "\"")))
                .andExpect(content().string(not(containsString("id=\"doktor-" + ayse.getId() + "\""))))
                .andExpect(content().string(containsString("<dt>Doktor</dt>")));
    }

    @Test
    void secilenSaatDolduysaHastaDoktorunTakvimineDoner() throws Exception {
        Sube serdivan = sube("Serdivan Şubesi");
        Hizmet genel = hizmet("Genel Muayene");
        Calisan ayse = calisan("ayse.demir@sakaryasaglik.com.tr");
        String onay = "/hasta/randevu-al/onay?calisanId=" + ayse.getId() + "&hizmetId=" + genel.getId()
                + "&zaman=" + java.time.LocalDate.now().plusDays(30).with(java.time.DayOfWeek.MONDAY) + "T09:20";
        String zaman = onay.substring(onay.indexOf("&zaman=") + 7);

        // Saat başka bir sekmede alınır
        mvc.perform(post("/hasta/randevu-al").with(user("18301661332").roles("HASTA")).with(csrf())
                        .param("calisanId", ayse.getId().toString()).param("hizmetId", genel.getId().toString())
                        .param("zaman", zaman))
                .andExpect(redirectedUrl("/hasta"));

        String takvim = "/hasta/randevu-al?kurumId=" + serdivan.getKurum().getId() + "&subeId=" + serdivan.getId()
                + "&hizmetId=" + genel.getId() + "&calisanId=" + ayse.getId();
        // Özet sayfası artık açılmaz; döngüye girmeden doktorun takvimine döner
        mvc.perform(get(onay).with(user(HASTA).roles("HASTA")).header("Referer", "http://localhost" + onay))
                .andExpect(redirectedUrl(takvim))
                .andExpect(flash().attribute("hata", containsString("dolu")));
        // Özet sayfasından yapılan gönderim de reddedilir ve aynı yere döner
        mvc.perform(post("/hasta/randevu-al").with(user(HASTA).roles("HASTA")).with(csrf())
                        .header("Referer", "http://localhost" + onay)
                        .param("calisanId", ayse.getId().toString()).param("hizmetId", genel.getId().toString())
                        .param("zaman", zaman))
                .andExpect(redirectedUrl(onay));
    }

    @Test
    void ayniAdreseGonderilenFormdakiHataAyniSayfadaGosterilir() throws Exception {
        // Hizmetler sayfasındaki form yine /yonetici/hizmetler adresine gönderilir; hata panele atmamalı
        mvc.perform(post("/yonetici/hizmetler").with(user(YONETICI).roles("YONETICI")).with(csrf())
                        .header("Referer", "http://localhost/yonetici/hizmetler")
                        .param("ad", "Hatalı").param("tur", "GENEL_MUAYENE").param("sureDakika", "30").param("ucret", "-5"))
                .andExpect(redirectedUrl("/yonetici/hizmetler"))
                .andExpect(flash().attribute("hata", "Hizmet ücreti negatif olamaz."));
    }

    @Test
    void panelYonlendirmesiHataMesajiniKaybetmez() throws Exception {
        mvc.perform(get("/panel").with(user(YONETICI).roles("YONETICI")).flashAttr("hata", "Örnek hata"))
                .andExpect(redirectedUrl("/yonetici"))
                .andExpect(flash().attribute("hata", "Örnek hata"));
    }

    @Test
    void doktorBulunamazsaRandevuAlmaninIlkAdiminaDoner() throws Exception {
        mvc.perform(get("/hasta/randevu-al/onay?calisanId=999999&hizmetId=1&zaman=2030-01-07T09:00")
                        .with(user(HASTA).roles("HASTA"))
                        .header("Referer", "http://localhost/hasta/randevu-al/onay?calisanId=999999&hizmetId=1&zaman=2030-01-07T09:00"))
                .andExpect(redirectedUrl("/hasta/randevu-al"));
    }

    private Sube sube(String ad) {
        return subeRepository.findAll().stream().filter(s -> s.getAd().equals(ad)).findFirst().orElseThrow();
    }

    private Kurum kurum(String ad) {
        return kurumRepository.findAll().stream().filter(k -> k.getAd().equals(ad)).findFirst().orElseThrow();
    }

    private Bolum bolum(Sube sube, String ad) {
        return sube.getBolumler().stream().filter(b -> b.getAd().equals(ad)).findFirst().orElseThrow();
    }

    private Hizmet hizmet(String ad) {
        return kurum("Sakarya Sağlık Grubu").getHizmetler().stream().filter(h -> h.getAd().equals(ad)).findFirst().orElseThrow();
    }

    private Calisan calisan(String eposta) {
        return (Calisan) kullaniciRepository.findByGirisKimligi(eposta).orElseThrow();
    }
}
