package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Geçmiş bir saate randevu alınamaz.
 */
@Component
@Order(1)
public class GelecekZamanKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        if (!talep.baslangic().isAfter(LocalDateTime.now())) {
            throw new IsKuraliIhlali("Geçmiş bir saate randevu alınamaz.");
        }
    }
}
