package com.klinik.randevu.web.form;

import com.klinik.randevu.model.kullanici.Calisan;

/** Yöneticinin var olan bir personelin bilgilerini düzenlediği form. E-posta ve şifre bu formda değişmez. */
public class CalisanGuncellemeFormu {

    private String adSoyad;
    /** Doktor için uzmanlık alanı, sağlık personeli için görev tanımı. */
    private String unvanBilgisi;
    private Long subeId;
    private Long bolumId;
    private String telefon;

    public static CalisanGuncellemeFormu calisandan(Calisan c) {
        CalisanGuncellemeFormu f = new CalisanGuncellemeFormu();
        f.adSoyad = c.getAdSoyad();
        f.unvanBilgisi = c.getUnvanBilgisi();
        f.subeId = c.getSube().getId();
        f.bolumId = c.getBolum() == null ? null : c.getBolum().getId();
        f.telefon = c.getTelefon();
        return f;
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public void setAdSoyad(String adSoyad) {
        this.adSoyad = adSoyad;
    }

    public String getUnvanBilgisi() {
        return unvanBilgisi;
    }

    public void setUnvanBilgisi(String unvanBilgisi) {
        this.unvanBilgisi = unvanBilgisi;
    }

    public Long getSubeId() {
        return subeId;
    }

    public void setSubeId(Long subeId) {
        this.subeId = subeId;
    }

    public Long getBolumId() {
        return bolumId;
    }

    public void setBolumId(Long bolumId) {
        this.bolumId = bolumId;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }
}
