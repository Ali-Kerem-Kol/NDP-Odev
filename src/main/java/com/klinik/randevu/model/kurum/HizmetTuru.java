package com.klinik.randevu.model.kurum;

/**
 * Sağlık hizmetinin türü (genel muayene, radyoloji, laboratuvar...). Etiketi sabitin kendisi taşır.
 */
public enum HizmetTuru {
    GENEL_MUAYENE("Genel Muayene"),
    UZMAN_MUAYENESI("Uzman Muayenesi"),
    RADYOLOJI("Radyoloji"),
    LABORATUVAR("Laboratuvar"),
    FIZYOTERAPI("Fizyoterapi"),
    DIS_TEDAVISI("Diş Tedavisi"),
    DIGER("Diğer");

    private final String etiket;

    HizmetTuru(String etiket) {
        this.etiket = etiket;
    }

    public String getEtiket() {
        return etiket;
    }
}
