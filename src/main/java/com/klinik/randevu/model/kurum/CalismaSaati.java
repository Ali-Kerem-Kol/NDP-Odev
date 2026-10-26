package com.klinik.randevu.model.kurum;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.yardimci.TarihYardimcisi;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Bir şubenin belirli bir gündeki açılış-kapanış saatleri (değer nesnesi). */
@Embeddable
public class CalismaSaati {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek gun;

    @Column(nullable = false)
    private LocalTime acilis;

    @Column(nullable = false)
    private LocalTime kapanis;

    protected CalismaSaati() {
    }

    public CalismaSaati(DayOfWeek gun, LocalTime acilis, LocalTime kapanis) {
        if (gun == null || acilis == null || kapanis == null) {
            throw new IsKuraliIhlali("Gün, açılış ve kapanış saati zorunludur.");
        }
        if (!acilis.isBefore(kapanis)) {
            throw new IsKuraliIhlali("Açılış saati kapanış saatinden önce olmalıdır.");
        }
        this.gun = gun;
        this.acilis = acilis;
        this.kapanis = kapanis;
    }

    /** Verilen saat aralığı tamamen çalışma saatleri içinde mi? */
    public boolean kapsar(LocalTime baslangic, LocalTime bitis) {
        return !baslangic.isBefore(acilis) && !bitis.isAfter(kapanis);
    }

    public DayOfWeek getGun() {
        return gun;
    }

    public String getGunAdi() {
        return TarihYardimcisi.gunAdiniGetir(gun);
    }

    public LocalTime getAcilis() {
        return acilis;
    }

    public LocalTime getKapanis() {
        return kapanis;
    }
}
