package com.klinik.randevu.model;

import com.klinik.randevu.model.randevu.RandevuDurumu;
import org.junit.jupiter.api.Test;

import static com.klinik.randevu.model.randevu.RandevuDurumu.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandevuDurumuTest {

    @Test
    void bekleyenRandevuOnaylanabilirReddedilebilirIptalEdilebilir() {
        assertTrue(BEKLEMEDE.gecebilir(ONAYLANDI));
        assertTrue(BEKLEMEDE.gecebilir(REDDEDILDI));
        assertTrue(BEKLEMEDE.gecebilir(IPTAL));
        assertFalse(BEKLEMEDE.gecebilir(TAMAMLANDI));
    }

    @Test
    void onaylanmisRandevuTamamlanabilirAmaReddedilemez() {
        assertTrue(ONAYLANDI.gecebilir(TAMAMLANDI));
        assertTrue(ONAYLANDI.gecebilir(IPTAL));
        assertFalse(ONAYLANDI.gecebilir(REDDEDILDI));
        assertTrue(ONAYLANDI.gecebilir(GELMEDI));
        assertFalse(ONAYLANDI.gecebilir(ZAMAN_ASIMI), "onaylanmış randevu zaman aşımına uğramaz");
        assertTrue(BEKLEMEDE.gecebilir(ZAMAN_ASIMI));
        assertFalse(BEKLEMEDE.gecebilir(GELMEDI), "onaylanmamış randevuya gelmedi denemez");
    }

    @Test
    void sonDurumlardanCikisYoktur() {
        for (RandevuDurumu son : new RandevuDurumu[]{REDDEDILDI, IPTAL, TAMAMLANDI, GELMEDI, ZAMAN_ASIMI}) {
            for (RandevuDurumu hedef : RandevuDurumu.values()) {
                assertFalse(son.gecebilir(hedef), son + " -> " + hedef);
            }
        }
    }
}
