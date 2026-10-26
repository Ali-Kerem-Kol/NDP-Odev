package com.klinik.randevu.model.kurum;

/**
 * Kurumun türü (hastane, klinik, poliklinik, tıp merkezi). Etiketi sabitin kendisi taşır.
 */
public enum KurumTuru {
    HASTANE("Hastane"),
    KLINIK("Klinik"),
    POLIKLINIK("Poliklinik"),
    TIP_MERKEZI("Tıp Merkezi");

    private final String etiket;

    KurumTuru(String etiket) {
        this.etiket = etiket;
    }

    public String getEtiket() {
        return etiket;
    }
}
