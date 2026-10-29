package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Çalışanın aynı saatte başka bir etkin randevusu varsa talebi reddeder.
 */
@Component
@Order(5)
public class CalisanCakismaKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        boolean cakisma = doluluk.calisanDoluMu(talep.calisan().getId(), talep.baslangic(), talep.bitis());
        if (cakisma) {
            throw new IsKuraliIhlali("Seçtiğiniz saat dolu. Lütfen başka bir saat seçin.");
        }
    }
}
