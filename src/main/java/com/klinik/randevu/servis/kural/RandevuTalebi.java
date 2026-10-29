package com.klinik.randevu.servis.kural;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kurum.Hizmet;

import java.time.LocalDateTime;

/** Doğrulanacak randevu isteği; bitiş zamanı hizmet süresinden hesaplanır. */
public record RandevuTalebi(Hasta hasta, Calisan calisan, Hizmet hizmet,
                            LocalDateTime baslangic, LocalDateTime bitis) {

    public static RandevuTalebi of(Hasta hasta, Calisan calisan, Hizmet hizmet, LocalDateTime baslangic) {
        return new RandevuTalebi(hasta, calisan, hizmet, baslangic, baslangic.plusMinutes(hizmet.getSureDakika()));
    }
}
