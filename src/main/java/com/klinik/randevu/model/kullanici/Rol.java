package com.klinik.randevu.model.kullanici;

/**
 * Kullanıcı rolleri. Spring Security yetkisi ve ekranda gösterilen etiket buradan gelir.
 * Yetki kararları rol karşılaştırmasıyla değil, {@link Kullanici} alt sınıflarının ezdiği metotlarla verilir.
 */
public enum Rol {
    HASTA("Hasta"),
    DOKTOR("Doktor"),
    SAGLIK_PERSONELI("Sağlık Personeli"),
    YONETICI("Kurum Yöneticisi"),
    SISTEM_YONETICISI("Sistem Yöneticisi");

    private final String etiket;

    Rol(String etiket) {
        this.etiket = etiket;
    }

    public String getEtiket() {
        return etiket;
    }

    /** Spring Security'nin beklediği yetki adı. */
    public String getYetkiAdi() {
        return "ROLE_" + name();
    }
}
