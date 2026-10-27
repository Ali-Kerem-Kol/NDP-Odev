package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.hata.IsKuraliIhlali;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Doktor dışındaki randevu verebilen personel: radyoloji teknisyeni, laborant, fizyoterapist... */
@Entity
@DiscriminatorValue("SAGLIK_PERSONELI")
public class SaglikPersoneli extends Calisan {

    @Column(length = 100)
    private String gorev;

    protected SaglikPersoneli() {
    }

    public SaglikPersoneli(String kurumsalEposta, String sifreOzeti, String adSoyad, String gorev) {
        super(kurumsalEposta, sifreOzeti, adSoyad);
        setGorev(gorev);
    }

    @Override
    public Rol getRol() {
        return Rol.SAGLIK_PERSONELI;
    }

    @Override
    public String getUnvan() {
        return gorev;
    }

    @Override
    public String getUnvanBilgisi() {
        return gorev;
    }

    @Override
    public void unvaniGuncelle(String yeniBilgi) {
        setGorev(yeniBilgi);
    }

    @Override
    public String getUnvanEtiketi() {
        return "Görev";
    }

    public void setGorev(String gorev) {
        if (gorev == null || gorev.isBlank()) {
            throw new IsKuraliIhlali("Sağlık personeli için görev tanımı zorunludur.");
        }
        this.gorev = gorev.trim();
    }

    public String getGorev() {
        return gorev;
    }
}
