package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Hasta yalnızca çalışanın tanımlı uygunluk zaman dilimlerine randevu alabilir. */
@Component
@Order(4)
public class UygunlukKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        if (!talep.calisan().musaitMi(talep.baslangic(), talep.bitis())) {
            throw new IsKuraliIhlali(talep.calisan().getGorunenAd() + " bu saatte hasta kabul etmiyor.");
        }
    }
}
