package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.hata.IsKuraliIhlali;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Uzmanlık alanı olan çalışan. {@link Calisan}'dan türer; görünen adını "Dr." ön ekiyle,
 * unvanını uzmanlık alanından üreterek üst sınıfın metotlarını kendine göre ezer (çok biçimlilik).
 */
@Entity
@DiscriminatorValue("DOKTOR")
public class Doktor extends Calisan {

    @Column(length = 100)
    private String uzmanlikAlani;

    protected Doktor() {
    }

    public Doktor(String kurumsalEposta, String sifreOzeti, String adSoyad, String uzmanlikAlani) {
        super(kurumsalEposta, sifreOzeti, adSoyad);
        setUzmanlikAlani(uzmanlikAlani);
    }

    @Override
    public Rol getRol() {
        return Rol.DOKTOR;
    }

    @Override
    public String getGorunenAd() {
        return "Dr. " + getAdSoyad();
    }

    @Override
    public String getUnvan() {
        return uzmanlikAlani + " Uzmanı";
    }

    @Override
    public String getUnvanBilgisi() {
        return uzmanlikAlani;
    }

    @Override
    public void unvaniGuncelle(String yeniBilgi) {
        setUzmanlikAlani(yeniBilgi);
    }

    @Override
    public String getUnvanEtiketi() {
        return "Uzmanlık alanı";
    }

    public void setUzmanlikAlani(String uzmanlikAlani) {
        if (uzmanlikAlani == null || uzmanlikAlani.isBlank()) {
            throw new IsKuraliIhlali("Doktor için uzmanlık alanı zorunludur.");
        }
        this.uzmanlikAlani = uzmanlikAlani.trim();
    }

    public String getUzmanlikAlani() {
        return uzmanlikAlani;
    }
}
