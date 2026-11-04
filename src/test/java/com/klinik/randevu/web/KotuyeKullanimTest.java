package com.klinik.randevu.web;

import com.klinik.randevu.guvenlik.KotuyeKullanimKorumasi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Giriş ve kayıt ekranlarının kaba kuvvet ve hesap taramasına karşı korunması.
 * Diğer testleri etkilememek için her test kendi IP adresini kullanır ve sonunda sayaçları sıfırlar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KotuyeKullanimTest {

    private static final String TEKNISYEN = "ali.celik@sakaryasaglik.com.tr";
    private static final String LABORANT = "fatma.koc@sakaryasaglik.com.tr";
    private static final String SIFRE = "Medora2026";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private KotuyeKullanimKorumasi koruma;

    @AfterEach
    void temizle() {
        koruma.tumunuSifirla();
    }

    @Test
    void besHataliDenemedenSonraDogruSifreDeKabulEdilmez() throws Exception {
        for (int i = 0; i < 4; i++) {
            giris(TEKNISYEN, "yanlis", "10.0.0.1").andExpect(redirectedUrl("/giris?hata"));
        }
        giris(TEKNISYEN, "yanlis", "10.0.0.1").andExpect(redirectedUrl("/giris?kilitli=15"));
        giris(TEKNISYEN, SIFRE, "10.0.0.1")
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/giris?kilitli=15"));
        // Kilit hesaba bağlı: başka cihazdan da aynı hesap denenemez
        giris(TEKNISYEN, SIFRE, "10.0.0.2").andExpect(unauthenticated());
        // Başka hesap etkilenmez
        giris(LABORANT, SIFRE, "10.0.0.2").andExpect(authenticated());
    }

    @Test
    void kimligeKucukBuyukHarfOynayarakKilitAsilamaz() throws Exception {
        for (int i = 0; i < 5; i++) {
            giris(TEKNISYEN, "yanlis", "10.0.1.1");
        }
        giris(" ALI.CELIK@SakaryaSaglik.com.tr ", SIFRE, "10.0.1.1").andExpect(unauthenticated());
    }

    @Test
    void basariliGirisHesapSayaciniSifirlar() throws Exception {
        for (int i = 0; i < 4; i++) {
            giris(TEKNISYEN, "yanlis", "10.0.2.1");
        }
        giris(TEKNISYEN, SIFRE, "10.0.2.1").andExpect(authenticated());
        for (int i = 0; i < 4; i++) {
            giris(TEKNISYEN, "yanlis", "10.0.2.1").andExpect(redirectedUrl("/giris?hata"));
        }
    }

    @Test
    void olmayanHesapDaAyniSekildeKilitlenirHesapVarligiAnlasilmaz() throws Exception {
        for (int i = 0; i < 4; i++) {
            giris("olmayan.kisi@sakaryasaglik.com.tr", "yanlis", "10.0.3.1").andExpect(redirectedUrl("/giris?hata"));
        }
        giris("olmayan.kisi@sakaryasaglik.com.tr", "yanlis", "10.0.3.1").andExpect(redirectedUrl("/giris?kilitli=15"));
    }

    @Test
    void tekCihazdanCokSayidaHesapDenenirseCihazKilitlenir() throws Exception {
        for (int i = 0; i < 20; i++) {
            giris("tarama" + i + "@ornek.com", "yanlis", "10.0.4.1");
        }
        giris(LABORANT, SIFRE, "10.0.4.1")
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/giris?kilitli=15"));
        giris(LABORANT, SIFRE, "10.0.4.2").andExpect(authenticated());
    }

    @Test
    void kilitMesajiGirisSayfasindaGosterilir() throws Exception {
        mvc.perform(get("/giris").param("kilitli", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12 dakika sonra yeniden deneyin")));
    }

    @Test
    void kayitDenemeleriCihazBasinaSinirlanirVeMesajHesapVarliginiDogrulamaz() throws Exception {
        kayit("62601815964", "10.0.5.1")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bu bilgilerle yeni hesap oluşturulamadı")))
                .andExpect(content().string(not(containsString("kayıtlı bir hesap zaten var"))));
        for (int i = 0; i < 9; i++) {
            kayit("62601815964", "10.0.5.1");
        }
        kayit("28609139020", "10.0.5.1")
                .andExpect(content().string(containsString("çok fazla kayıt denemesi")));
        kayit("28609139020", "10.0.5.2").andExpect(redirectedUrl("/giris"));
    }

    private ResultActions giris(String kimlik, String sifre, String ip) throws Exception {
        return mvc.perform(post("/giris").param("kimlik", kimlik).param("sifre", sifre).with(csrf()).with(ip(ip)));
    }

    private ResultActions kayit(String tc, String ip) throws Exception {
        return mvc.perform(post("/kayit").with(csrf()).with(ip(ip))
                .param("tcKimlikNo", tc).param("adSoyad", "Deneme Kişi").param("dogumTarihi", "1990-01-01")
                .param("telefon", "0555 111 22 33").param("sifre", "GuvenliSifre1").param("sifreTekrar", "GuvenliSifre1")
                .param("kvkkOnay", "true"));
    }

    private static RequestPostProcessor ip(String adres) {
        return istek -> {
            istek.setRemoteAddr(adres);
            return istek;
        };
    }
}
