package com.klinik.randevu.model.kurum;

import com.klinik.randevu.model.TemelVarlik;
import jakarta.persistence.*;

/** Şubeye bağlı tıbbi bölüm (Dahiliye, Kardiyoloji, Radyoloji...). */
@Entity
@Table(name = "bolumler")
public class Bolum extends TemelVarlik {

    @Column(nullable = false, length = 100)
    private String ad;

    @Column(length = 300)
    private String aciklama;

    @ManyToOne(optional = false)
    private Sube sube;

    protected Bolum() {
    }

    Bolum(Sube sube, String ad, String aciklama) {
        this.sube = sube;
        guncelle(ad, aciklama);
    }

    /** Ad benzersizliğini şube denetler ({@link Sube#bolumGuncelle}); bu yüzden paket erişimlidir. */
    void guncelle(String ad, String aciklama) {
        this.ad = ad.trim();
        this.aciklama = aciklama == null || aciklama.isBlank() ? null : aciklama.trim();
    }

    public String getAd() {
        return ad;
    }

    public String getAciklama() {
        return aciklama;
    }

    public Sube getSube() {
        return sube;
    }
}
