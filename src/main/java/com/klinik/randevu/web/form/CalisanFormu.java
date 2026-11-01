package com.klinik.randevu.web.form;

import com.klinik.randevu.servis.CalisanTipi;

/** Yöneticinin yeni doktor / sağlık personeli kaydı için doldurduğu form. */
public class CalisanFormu {

    private CalisanTipi tip = CalisanTipi.DOKTOR;
    private String adSoyad;
    /** Kurumsal e-posta; çalışan sisteme bu adresle giriş yapar. */
    private String eposta;
    private String sifre;
    private String telefon;
    private Long subeId;
    private Long bolumId;
    /** Doktor için uzmanlık alanı, sağlık personeli için görev tanımı. */
    private String unvanBilgisi;

    public CalisanTipi getTip() {
        return tip;
    }

    public void setTip(CalisanTipi tip) {
        this.tip = tip;
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public void setAdSoyad(String adSoyad) {
        this.adSoyad = adSoyad;
    }

    public String getEposta() {
        return eposta;
    }

    public void setEposta(String eposta) {
        this.eposta = eposta;
    }

    public String getSifre() {
        return sifre;
    }

    public void setSifre(String sifre) {
        this.sifre = sifre;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
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

    public String getUnvanBilgisi() {
        return unvanBilgisi;
    }

    public void setUnvanBilgisi(String unvanBilgisi) {
        this.unvanBilgisi = unvanBilgisi;
    }
}
