package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.repository.CalisanRepository;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.KurumRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

/** Personel düzenleme/pasifleştirme, kurum oluşturma bütünlüğü, aydınlatma metni ve role göre bağlantılar. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonelVeKurumTest {

    private static final String YONETICI = "ahmet.yilmaz@sakaryasaglik.com.tr";
    private static final String KLINIK_YONETICI = "elif.kaya@gulumsedis.com.tr";
    private static final String DR_AYSE = "ayse.demir@sakaryasaglik.com.tr";
    private static final String DR_ZEYNEP = "zeynep.arslan@sakaryasaglik.com.tr";
    private static final String TEKNISYEN = "ali.celik@sakaryasaglik.com.tr";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private KullaniciRepository kullaniciRepository;
    @Autowired
    private CalisanRepository calisanRepository;
    @Autowired
    private KurumRepository kurumRepository;

    // ---- Personel düzenleme ----

    @Test
    void yoneticiDoktorunUzmanliginiVePersonelinGoreviniGunceller() throws Exception {
        Calisan zeynep = calisan(DR_ZEYNEP);
        guncelle(YONETICI, zeynep, "Zeynep  Arslan Kaya", "Aile Hekimliği", zeynep.getSube())
                .andExpect(flash().attribute("basari", "Personel bilgileri kaydedildi."));
        assertEquals("Zeynep Arslan Kaya", zeynep.getAdSoyad());
        assertEquals("Aile Hekimliği Uzmanı", zeynep.getUnvan(), "doktor unvanı uzmanlıktan üretilir");

        Calisan ali = calisan(TEKNISYEN);
        guncelle(YONETICI, ali, ali.getAdSoyad(), "Kıdemli Radyoloji Teknisyeni", ali.getSube())
                .andExpect(flash().attributeExists("basari"));
        assertEquals("Kıdemli Radyoloji Teknisyeni", ali.getUnvan(), "personel unvanı doğrudan görevdir");
    }

    @Test
    void ileriRandevusuOlanPersonelinSubesiDegistirilemez() throws Exception {
        Calisan ayse = calisan(DR_AYSE);
        Sube eskiSube = ayse.getSube();
        guncelle(YONETICI, ayse, ayse.getAdSoyad(), "İç Hastalıkları", digerSube(eskiSube))
                .andExpect(flash().attribute("hata", containsString("ileri tarihli randevusu var")));
        assertEquals(eskiSube.getId(), ayse.getSube().getId());
    }

    @Test
    void randevusuOlmayanPersonelinSubesiDegisir() throws Exception {
        Calisan zeynep = calisan(DR_ZEYNEP);
        Sube yeni = digerSube(zeynep.getSube());
        guncelle(YONETICI, zeynep, zeynep.getAdSoyad(), "İç Hastalıkları", yeni)
                .andExpect(flash().attributeExists("basari"));
        assertEquals(yeni.getId(), zeynep.getSube().getId());
    }

    @Test
    void baskaKurumunYoneticisiPersoneliDuzenleyemezVePasiflestiremez() throws Exception {
        Calisan zeynep = calisan(DR_ZEYNEP);
        guncelle(KLINIK_YONETICI, zeynep, "Başka Ad", "Başka", zeynep.getSube())
                .andExpect(flash().attribute("hata", "Bu personeli düzenleme yetkiniz yok."));
        mvc.perform(post("/yonetici/calisanlar/{id}/pasiflestir", zeynep.getId())
                        .with(user(KLINIK_YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attribute("hata", "Bu personeli düzenleme yetkiniz yok."));
        assertTrue(zeynep.isAktif());
        assertNotEquals("Başka Ad", zeynep.getAdSoyad());
    }

    // ---- Pasifleştirme ----

    @Test
    void ileriRandevusuOlanPersonelPasiflestirilemez() throws Exception {
        Calisan ayse = calisan(DR_AYSE);
        mvc.perform(post("/yonetici/calisanlar/{id}/pasiflestir", ayse.getId())
                        .with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attribute("hata", containsString("ileri tarihli randevusu var")));
        assertTrue(ayse.isAktif());
    }

    @Test
    void pasifPersonelGirisYapamazListelerdeGorunmezVeAcikOturumuKapanir() throws Exception {
        Calisan zeynep = calisan(DR_ZEYNEP);
        Long hizmetId = zeynep.getHizmetler().get(0).getId();
        assertFalse(calisanRepository.hizmetVerenler(zeynep.getSube().getId(), hizmetId).isEmpty());

        mvc.perform(post("/yonetici/calisanlar/{id}/pasiflestir", zeynep.getId())
                        .with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attributeExists("basari"));
        assertFalse(zeynep.isAktif());

        assertTrue(calisanRepository.hizmetVerenler(zeynep.getSube().getId(), hizmetId).isEmpty(),
                "pasif doktor randevu listesinde çıkmaz");
        mvc.perform(formLogin("/giris").userParameter("kimlik").passwordParam("sifre").user(DR_ZEYNEP).password("Medora2026"))
                .andExpect(unauthenticated());
        mvc.perform(get("/calisan").with(user(DR_ZEYNEP).roles("DOKTOR")))
                .andExpect(redirectedUrl("/giris?pasif"));

        mvc.perform(post("/yonetici/calisanlar/{id}/aktiflestir", zeynep.getId())
                        .with(user(YONETICI).roles("YONETICI")).with(csrf()))
                .andExpect(flash().attributeExists("basari"));
        assertTrue(zeynep.isAktif());
    }

    @Test
    void personelDetaySayfasiBilgiFormuVeHesapDurumunuGosterir() throws Exception {
        mvc.perform(get("/yonetici/calisanlar/{id}", calisan(DR_AYSE).getId()).with(user(YONETICI).roles("YONETICI")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Personel bilgileri")))
                .andExpect(content().string(containsString("Uzmanlık alanı")))
                .andExpect(content().string(containsString("şubesi değiştirilemez")));
        mvc.perform(get("/yonetici/calisanlar/{id}", calisan(TEKNISYEN).getId()).with(user(YONETICI).roles("YONETICI")))
                .andExpect(content().string(containsString(">Görev<")));
    }

    // ---- Kurum oluşturma bütünlüğü ----

    @Test
    void ilkSubeAdiGecersizseKurumDaOlusmaz() throws Exception {
        long once = kurumRepository.count();
        mvc.perform(post("/admin/kurum").with(user("admin@medora.com.tr").roles("SISTEM_YONETICISI")).with(csrf())
                        .param("ad", "Yarım Kalacak Klinik").param("tur", "KLINIK").param("ilkSubeAdi", " "))
                .andExpect(flash().attribute("hata", "Şube adı boş olamaz."));
        assertEquals(once, kurumRepository.count(), "şubesiz kurum kaydedilmemeli");
    }

    @Test
    void kurumIlkSubesiyleBirlikteOlusur() throws Exception {
        mvc.perform(post("/admin/kurum").with(user("admin@medora.com.tr").roles("SISTEM_YONETICISI")).with(csrf())
                        .param("ad", "Yeni Tıp Merkezi").param("tur", "TIP_MERKEZI").param("ilkSubeAdi", "Merkez"))
                .andExpect(flash().attributeExists("basari"));
        Kurum kurum = kurumRepository.findAll().stream().filter(k -> k.getAd().equals("Yeni Tıp Merkezi")).findFirst().orElseThrow();
        assertEquals(1, kurum.getSubeler().size());
        assertEquals("Merkez", kurum.getSubeler().get(0).getAd());
    }

    // ---- Aydınlatma metni ----

    @Test
    void aydinlatmaMetniHerkeseAcikVeKayittanBaglantili() throws Exception {
        mvc.perform(get("/aydinlatma-metni"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kanunun 11. maddesi")))
                .andExpect(content().string(containsString("destek@medora.com.tr")));
        mvc.perform(get("/kayit")).andExpect(content().string(containsString("href=\"/aydinlatma-metni\"")));
    }

    // ---- Role göre randevu bağlantıları ----

    @Test
    void personelVeYoneticiyeHastaRandevuBaglantisiGosterilmez() throws Exception {
        // Her rol kendi çalışma alanının adını görür; sistem yöneticisi sitenin kendisinde çalışır
        for (String[] kim : new String[][]{{DR_AYSE, "DOKTOR", "Randevularım"}, {YONETICI, "YONETICI", "Kurum yönetimi"},
                {"admin@medora.com.tr", "SISTEM_YONETICISI", "Kurum ekle"}}) {
            mvc.perform(get("/").with(user(kim[0]).roles(kim[1])))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("/hasta/randevu-al"))))
                    .andExpect(content().string(containsString(kim[2])));
            mvc.perform(get("/haberler/1").with(user(kim[0]).roles(kim[1])))
                    .andExpect(content().string(not(containsString("/hasta/randevu-al"))));
        }
        mvc.perform(get("/").with(user("62601815964").roles("HASTA")))
                .andExpect(content().string(containsString("/hasta/randevu-al")));
        mvc.perform(get("/")).andExpect(content().string(containsString("/hasta/randevu-al")));
    }

    private org.springframework.test.web.servlet.ResultActions guncelle(String yonetici, Calisan c, String ad,
                                                                         String unvan, Sube sube) throws Exception {
        return mvc.perform(post("/yonetici/calisanlar/{id}/bilgiler", c.getId())
                .with(user(yonetici).roles("YONETICI")).with(csrf())
                .param("adSoyad", ad).param("unvanBilgisi", unvan)
                .param("subeId", sube.getId().toString()).param("bolumId", "").param("telefon", ""));
    }

    private Calisan calisan(String kimlik) {
        return (Calisan) kullaniciRepository.findByGirisKimligi(kimlik).orElseThrow();
    }

    private Sube digerSube(Sube sube) {
        return sube.getKurum().getSubeler().stream().filter(s -> !s.getId().equals(sube.getId())).findFirst().orElseThrow();
    }
}
