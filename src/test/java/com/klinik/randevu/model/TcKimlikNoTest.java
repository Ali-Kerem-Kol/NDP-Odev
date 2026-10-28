package com.klinik.randevu.model;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.TcKimlikNo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TcKimlikNoTest {

    @Test
    void gecerliNumaralarKabulEdilir() {
        assertTrue(TcKimlikNo.gecerliMi("10000000146"));
        assertTrue(TcKimlikNo.gecerliMi("62601815964"));
    }

    @Test
    void hataliNumaralarReddedilir() {
        assertFalse(TcKimlikNo.gecerliMi(null));
        assertFalse(TcKimlikNo.gecerliMi("1234567890"), "10 hane");
        assertFalse(TcKimlikNo.gecerliMi("01234567890"), "0 ile başlayamaz");
        assertFalse(TcKimlikNo.gecerliMi("62601815965"), "son kontrol hanesi hatalı");
        assertFalse(TcKimlikNo.gecerliMi("6260181596a"));
    }

    @Test
    void maskeliGosterimIlkIkiVeSonIkiHaneyiBirakir() {
        assertEquals("62*******64", TcKimlikNo.maskele("62601815964"));
    }

    @Test
    void hastaGecersizTcIleOlusturulamaz() {
        assertThrows(IsKuraliIhlali.class, () -> new Hasta("12345678901", "x", "Ad Soyad", null));
    }
}
