package com.klinik.randevu.model.kullanici;

/**
 * Hesabım sayfasındaki kimlik kartının tek satırı. Her kullanıcı türü hangi bilgilerini
 * göstereceğine kendisi karar verir ({@link Kullanici#getProfilBilgileri()}).
 */
public record ProfilBilgisi(String etiket, String deger) {
}
