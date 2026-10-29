package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Hasta aynı anda iki farklı randevuda bulunamaz (şube veya kurum farklı olsa bile). */
@Component
@Order(6)
public class HastaCakismaKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        boolean cakisma = doluluk.hastaDoluMu(talep.hasta().getId(), talep.baslangic(), talep.bitis());
        if (cakisma) {
            throw new IsKuraliIhlali("Bu saat aralığında zaten başka bir randevunuz var.");
        }
    }
}
