package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Randevu, şubenin o günkü çalışma saatlerinin tamamen içinde olmalıdır.
 */
@Component
@Order(3)
public class CalismaSaatiKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        if (!talep.calisan().getSube().acikMi(talep.baslangic(), talep.bitis())) {
            throw new IsKuraliIhlali("Şube seçilen saatte kapalı.");
        }
    }
}
