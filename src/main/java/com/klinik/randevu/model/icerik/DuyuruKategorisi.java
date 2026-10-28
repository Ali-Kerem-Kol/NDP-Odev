package com.klinik.randevu.model.icerik;

/**
 * Ana sayfa içeriğinin kategorisi. Her sabit ekranda kullanılacak etiketi ve rozet rengini kendisi taşır.
 */
public enum DuyuruKategorisi {
    DUYURU("Duyuru", "bilgi"),
    HABER("Haber", "basari"),
    SAGLIK_REHBERI("Sağlık rehberi", "uyari");

    private final String etiket;
    private final String renk;

    DuyuruKategorisi(String etiket, String renk) {
        this.etiket = etiket;
        this.renk = renk;
    }

    public String getEtiket() {
        return etiket;
    }

    public String getRenk() {
        return renk;
    }
}
