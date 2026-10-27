package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.TemelVarlik;
import com.klinik.randevu.yardimci.TarihYardimcisi;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Çalışanın haftalık olarak tekrar eden müsaitlik aralığı (ör. Pazartesi 09:00-12:00). */
@Entity
@Table(name = "uygunluk_dilimleri")
public class UygunlukDilimi extends TemelVarlik {

    @ManyToOne(optional = false)
    private Calisan calisan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek gun;

    @Column(nullable = false)
    private LocalTime baslangic;

    @Column(nullable = false)
    private LocalTime bitis;

    protected UygunlukDilimi() {
    }

    UygunlukDilimi(Calisan calisan, DayOfWeek gun, LocalTime baslangic, LocalTime bitis) {
        if (gun == null || baslangic == null || bitis == null) {
            throw new IsKuraliIhlali("Gün, başlangıç ve bitiş saati zorunludur.");
        }
        if (!baslangic.isBefore(bitis)) {
            throw new IsKuraliIhlali("Uygunluk başlangıcı bitişten önce olmalıdır.");
        }
        this.calisan = calisan;
        this.gun = gun;
        this.baslangic = baslangic;
        this.bitis = bitis;
    }

    /** Verilen zaman aralığı bu dilimin içinde kalıyor mu? */
    public boolean kapsar(LocalDateTime aralikBasi, LocalDateTime aralikSonu) {
        return aralikBasi.getDayOfWeek() == gun
                && aralikBasi.toLocalDate().equals(aralikSonu.toLocalDate())
                && !aralikBasi.toLocalTime().isBefore(baslangic)
                && !aralikSonu.toLocalTime().isAfter(bitis);
    }

    public boolean cakisir(UygunlukDilimi diger) {
        return gun == diger.gun && baslangic.isBefore(diger.bitis) && diger.baslangic.isBefore(bitis);
    }

    public String getAciklama() {
        return TarihYardimcisi.gunAdiniGetir(gun) + " " + baslangic + "-" + bitis;
    }

    public Calisan getCalisan() {
        return calisan;
    }

    public DayOfWeek getGun() {
        return gun;
    }

    public String getGunAdi() {
        return TarihYardimcisi.gunAdiniGetir(gun);
    }

    public LocalTime getBaslangic() {
        return baslangic;
    }

    public LocalTime getBitis() {
        return bitis;
    }
}
