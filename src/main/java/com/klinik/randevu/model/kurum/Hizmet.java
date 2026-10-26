package com.klinik.randevu.model.kurum;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.TemelVarlik;
import jakarta.persistence.*;

import java.math.BigDecimal;

/** Kurumun sunduğu sağlık hizmeti; süresi ve ücreti ile birlikte. */
@Entity
@Table(name = "hizmetler")
public class Hizmet extends TemelVarlik {

    public static final int AZAMI_SURE_DAKIKA = 480;

    @Column(nullable = false, length = 150)
    private String ad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HizmetTuru tur;

    @Column(nullable = false)
    private int sureDakika;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ucret;

    @Column(length = 500)
    private String aciklama;

    private boolean aktif = true;

    @ManyToOne(optional = false)
    private Kurum kurum;

    protected Hizmet() {
    }

    Hizmet(Kurum kurum, String ad, HizmetTuru tur, int sureDakika, BigDecimal ucret, String aciklama) {
        this.kurum = kurum;
        tanimiGuncelle(ad, tur, aciklama);
        setSureDakika(sureDakika);
        setUcret(ucret);
    }

    /** Hizmetin adı, türü ve açıklaması. Süre ve ücret ayrı doğrulanır; eski randevular etkilenmez. */
    public void tanimiGuncelle(String ad, HizmetTuru tur, String aciklama) {
        if (ad == null || ad.isBlank()) {
            throw new IsKuraliIhlali("Hizmet adı boş olamaz.");
        }
        this.ad = ad.trim();
        this.tur = tur == null ? HizmetTuru.DIGER : tur;
        this.aciklama = aciklama == null || aciklama.isBlank() ? null : aciklama.trim();
    }

    public void setSureDakika(int sureDakika) {
        if (sureDakika <= 0 || sureDakika > AZAMI_SURE_DAKIKA) {
            throw new IsKuraliIhlali("Hizmet süresi 1 ile " + AZAMI_SURE_DAKIKA + " dakika arasında olmalıdır.");
        }
        this.sureDakika = sureDakika;
    }

    public void setUcret(BigDecimal ucret) {
        if (ucret == null || ucret.signum() < 0) {
            throw new IsKuraliIhlali("Hizmet ücreti negatif olamaz.");
        }
        this.ucret = ucret;
    }

    public void aktiflikDegistir() {
        this.aktif = !this.aktif;
    }

    public String getAd() {
        return ad;
    }

    public HizmetTuru getTur() {
        return tur;
    }

    public int getSureDakika() {
        return sureDakika;
    }

    public BigDecimal getUcret() {
        return ucret;
    }

    public String getAciklama() {
        return aciklama;
    }

    public boolean isAktif() {
        return aktif;
    }

    public Kurum getKurum() {
        return kurum;
    }
}
