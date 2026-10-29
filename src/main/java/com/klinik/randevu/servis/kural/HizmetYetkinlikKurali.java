package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Hizmet randevuya açık olmalı ve seçilen çalışan bu hizmeti veriyor olmalıdır.
 */
@Component
@Order(2)
public class HizmetYetkinlikKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        if (!talep.hizmet().isAktif()) {
            throw new IsKuraliIhlali(talep.hizmet().getAd() + " hizmeti şu an randevuya kapalı.");
        }
        if (!talep.calisan().hizmetVerebilirMi(talep.hizmet())) {
            throw new IsKuraliIhlali(talep.calisan().getGorunenAd() + " bu hizmeti vermiyor.");
        }
    }
}
