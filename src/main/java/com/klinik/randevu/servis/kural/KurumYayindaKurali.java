package com.klinik.randevu.servis.kural;

import com.klinik.randevu.hata.IsKuraliIhlali;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Yayından kaldırılmış kuruma yeni randevu alınamaz; mevcut randevular etkilenmez. */
@Component
@Order(0)
public class KurumYayindaKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk) {
        if (!talep.hizmet().getKurum().isAktif()) {
            throw new IsKuraliIhlali(talep.hizmet().getKurum().getAd() + " şu an yeni randevu kabul etmiyor.");
        }
    }
}
