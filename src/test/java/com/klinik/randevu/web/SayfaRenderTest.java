package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.KurumRepository;
import com.klinik.randevu.servis.GunlukMusaitlik;
import com.klinik.randevu.servis.RandevuServisi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Her rolün her sayfası şablon hatası olmadan çizilir mi? */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SayfaRenderTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private KullaniciRepository kullaniciRepository;
    @Autowired
    private KurumRepository kurumRepository;
    @Autowired
    private RandevuServisi randevuServisi;

    @Test
    void herkeseAcikSayfalar() throws Exception {
        for (String adres : new String[]{"/", "/haberler", "/haberler?kategori=HABER", "/haberler/1", "/giris", "/giris?hata", "/giris?cikis", "/kayit"}) {
            mvc.perform(get(adres)).andExpect(status().isOk()).andExpect(content().string(containsString("Medora")));
        }
    }

    @Test
    void kayitFormuHatalariAlanlardaGosterir() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/kayit")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("tcKimlikNo", "12345678901").param("adSoyad", "Ayla Su").param("dogumTarihi", "1990-05-05")
                        .param("telefon", "0532 000 00 00").param("sifre", "GuvenliSifre1").param("sifreTekrar", "GuvenliSifre1")
                        .param("kvkkOnay", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("geçerli bir T.C. Kimlik No değil")))
                .andExpect(content().string(containsString("value=\"Ayla Su\"")));
    }

    @Test
    void hastaSayfalari() throws Exception {
        RequestPostProcessor hasta = oturum("62601815964", "HASTA");
        Kurum kurum = hastane();
        Sube sube = kurum.getSubeler().get(0);
        Hizmet hizmet = kurum.getHizmetler().stream().filter(h -> h.getAd().equals("Genel Muayene")).findFirst().orElseThrow();

        ac("/hasta", hasta);
        ac("/hasta/randevu-al", hasta);
        ac("/hasta/randevu-al?kurumId=" + kurum.getId(), hasta);
        ac("/hasta/randevu-al?kurumId=" + kurum.getId() + "&subeId=" + sube.getId(), hasta);
        mvc.perform(get("/hasta/randevu-al").param("kurumId", kurum.getId().toString())
                        .param("subeId", sube.getId().toString()).param("hizmetId", hizmet.getId().toString()).with(hasta))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"saat\"")));

        Calisan doktor = (Calisan) kullaniciRepository.findByGirisKimligi("ayse.demir@sakaryasaglik.com.tr").orElseThrow();
        Hasta h = (Hasta) kullaniciRepository.findByGirisKimligi("62601815964").orElseThrow();
        GunlukMusaitlik ilkGun = randevuServisi.musaitlikTakvimi(h, doktor, hizmet, LocalDate.now(), 14).get(0);
        String zaman = ilkGun.tarih() + "T" + ilkGun.saatler().get(0);
        ac("/hasta/randevu-al/onay?calisanId=" + doktor.getId() + "&hizmetId=" + hizmet.getId() + "&zaman=" + zaman, hasta);
        ac("/profil", hasta);
        Long randevuId = randevuServisi.hastaRandevulari(h).get(0).getId();
        ac("/randevu/" + randevuId, hasta);
        ac("/randevu/" + randevuId, oturum("ahmet.yilmaz@sakaryasaglik.com.tr", "YONETICI"));
    }

    @Test
    void calisanSayfalari() throws Exception {
        RequestPostProcessor doktor = oturum("ayse.demir@sakaryasaglik.com.tr", "DOKTOR");
        ac("/calisan", doktor);
        for (String sekme : new String[]{"BEKLEYEN", "YAKLASAN", "GECMIS", "TUMU"}) {
            ac("/calisan?sekme=" + sekme, doktor);
        }
        ac("/calisan/uygunluk", doktor);
        ac("/profil", doktor);
        ac("/calisan", oturum("ali.celik@sakaryasaglik.com.tr", "SAGLIK_PERSONELI"));
    }

    @Test
    void yoneticiSayfalari() throws Exception {
        RequestPostProcessor yonetici = oturum("ahmet.yilmaz@sakaryasaglik.com.tr", "YONETICI");
        Kurum kurum = hastane();
        Calisan doktor = (Calisan) kullaniciRepository.findByGirisKimligi("ayse.demir@sakaryasaglik.com.tr").orElseThrow();
        ac("/yonetici", yonetici);
        ac("/yonetici/randevular", yonetici);
        ac("/yonetici/randevular?sekme=TUMU", yonetici);
        ac("/yonetici/calisanlar", yonetici);
        ac("/yonetici/calisanlar/" + doktor.getId(), yonetici);
        ac("/yonetici/hizmetler", yonetici);
        ac("/yonetici/subeler", yonetici);
        ac("/yonetici/subeler/" + kurum.getSubeler().get(0).getId(), yonetici);
        ac("/haberler", yonetici);
        ac("/icerik/yeni", yonetici);
        ac("/profil", yonetici);
    }

    @Test
    void sistemYoneticisiSayfalari() throws Exception {
        RequestPostProcessor admin = oturum("admin@medora.com.tr", "SISTEM_YONETICISI");
        ac("/", admin);
        ac("/haberler", admin);
        ac("/haberler/1", admin);
        ac("/aydinlatma-metni", admin);
        ac("/icerik/yeni", admin);
        ac("/icerik/1/duzenle", admin);
        ac("/profil", admin);
    }

    private void ac(String adres, RequestPostProcessor kim) throws Exception {
        mvc.perform(get(adres).with(kim))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Bir şeyler ters gitti"))));
    }

    private RequestPostProcessor oturum(String kimlik, String rol) {
        return SecurityMockMvcRequestPostProcessors.user(kimlik).roles(rol);
    }

    private Kurum hastane() {
        return kurumRepository.findAll().stream().filter(k -> k.getAd().equals("Sakarya Sağlık Grubu")).findFirst().orElseThrow();
    }
}
