package com.klinik.randevu.model.kurum;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.TemelVarlik;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hastane veya klinik. Bir kurum tek şubeli de olabilir, çok şubeli de.
 * Sunulan sağlık hizmetleri kurum düzeyinde tanımlanır, tüm şubelerde geçerlidir.
 * Yayından kaldırılan kurum herkese açık sayfalarda görünmez ve yeni randevu almaz;
 * mevcut randevuları ve geçmişi korunur.
 */
@Entity
@Table(name = "kurumlar")
public class Kurum extends TemelVarlik {

    @Column(nullable = false, length = 150)
    private String ad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KurumTuru tur;

    @Column(length = 500)
    private String aciklama;

    @Column(nullable = false)
    private boolean aktif = true;

    @OneToMany(mappedBy = "kurum", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sube> subeler = new ArrayList<>();

    @OneToMany(mappedBy = "kurum", cascade = CascadeType.ALL)
    private List<Hizmet> hizmetler = new ArrayList<>();

    protected Kurum() {
    }

    public Kurum(String ad, KurumTuru tur, String aciklama) {
        setAd(ad);
        this.tur = tur == null ? KurumTuru.KLINIK : tur;
        this.aciklama = aciklama;
    }

    public Sube subeEkle(String subeAdi, String adres, String telefon) {
        Sube sube = new Sube(this, subeAdi, adres, telefon);
        subeler.add(sube);
        return sube;
    }

    /** Şubesiz kurumda randevu alınamayacağı için son şube kaldırılamaz. */
    public void subeKaldir(Sube sube) {
        if (subeler.size() <= 1) {
            throw new IsKuraliIhlali("Kurumun tek şubesi kaldırılamaz. Kurumu kapatmak için sistem yöneticisiyle görüşün.");
        }
        if (!subeler.remove(sube)) {
            throw new IsKuraliIhlali("Şube bu kuruma ait değil.");
        }
    }

    public Hizmet hizmetEkle(String hizmetAdi, HizmetTuru hizmetTuru, int sureDakika, BigDecimal ucret, String aciklama) {
        Hizmet hizmet = new Hizmet(this, hizmetAdi, hizmetTuru, sureDakika, ucret, aciklama);
        hizmetler.add(hizmet);
        return hizmet;
    }

    public void bilgileriGuncelle(String ad, KurumTuru tur, String aciklama) {
        setAd(ad);
        if (tur == null) {
            throw new IsKuraliIhlali("Kurum türü seçilmelidir.");
        }
        this.tur = tur;
        this.aciklama = aciklama == null || aciklama.isBlank() ? null : aciklama.trim();
    }

    public void yayindanKaldir() {
        this.aktif = false;
    }

    public void yayinaAl() {
        this.aktif = true;
    }

    public boolean isCokSubeli() {
        return subeler.size() > 1;
    }

    public void setAd(String ad) {
        if (ad == null || ad.isBlank()) {
            throw new IsKuraliIhlali("Kurum adı boş olamaz.");
        }
        this.ad = ad.trim();
    }

    public String getAd() {
        return ad;
    }

    public KurumTuru getTur() {
        return tur;
    }

    public String getAciklama() {
        return aciklama;
    }

    public boolean isAktif() {
        return aktif;
    }

    public List<Sube> getSubeler() {
        return Collections.unmodifiableList(subeler);
    }

    public List<Hizmet> getHizmetler() {
        return Collections.unmodifiableList(hizmetler);
    }
}
