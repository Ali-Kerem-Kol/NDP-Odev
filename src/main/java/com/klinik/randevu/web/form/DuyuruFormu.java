package com.klinik.randevu.web.form;

import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.icerik.DuyuruKategorisi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Duyuru yazma/düzenleme formunun verisi ve doğrulama kuralları.
 */
public class DuyuruFormu {

    @NotBlank(message = "Başlık zorunludur")
    @Size(max = Duyuru.AZAMI_BASLIK, message = "Başlık en fazla 150 karakter olabilir")
    private String baslik;

    @NotBlank(message = "Özet zorunludur")
    @Size(max = Duyuru.AZAMI_OZET, message = "Özet en fazla 300 karakter olabilir")
    private String ozet;

    @NotBlank(message = "İçerik zorunludur")
    @Size(max = Duyuru.AZAMI_ICERIK, message = "İçerik en fazla 10.000 karakter olabilir")
    private String icerik;

    @NotNull(message = "Kategori seçin")
    private DuyuruKategorisi kategori = DuyuruKategorisi.DUYURU;

    /** Yalnızca sistem yöneticisi için anlamlı; boşsa genel duyuru. */
    private Long kurumId;

    private boolean yayinla = true;

    public static DuyuruFormu duyurudan(Duyuru d) {
        DuyuruFormu f = new DuyuruFormu();
        f.baslik = d.getBaslik();
        f.ozet = d.getOzet();
        f.icerik = d.getIcerik();
        f.kategori = d.getKategori();
        f.kurumId = d.getKurum() == null ? null : d.getKurum().getId();
        f.yayinla = d.isYayinda();
        return f;
    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getOzet() {
        return ozet;
    }

    public void setOzet(String ozet) {
        this.ozet = ozet;
    }

    public String getIcerik() {
        return icerik;
    }

    public void setIcerik(String icerik) {
        this.icerik = icerik;
    }

    public DuyuruKategorisi getKategori() {
        return kategori;
    }

    public void setKategori(DuyuruKategorisi kategori) {
        this.kategori = kategori;
    }

    public Long getKurumId() {
        return kurumId;
    }

    public void setKurumId(Long kurumId) {
        this.kurumId = kurumId;
    }

    public boolean isYayinla() {
        return yayinla;
    }

    public void setYayinla(boolean yayinla) {
        this.yayinla = yayinla;
    }
}
