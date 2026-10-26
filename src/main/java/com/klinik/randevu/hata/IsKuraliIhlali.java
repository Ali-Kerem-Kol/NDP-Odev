package com.klinik.randevu.hata;

/**
 * Bir iş kuralı çiğnendiğinde fırlatılır (ör. randevu çakışması, geçersiz süre).
 * Mesajı doğrudan kullanıcıya gösterilir.
 */
public class IsKuraliIhlali extends RuntimeException {

    public IsKuraliIhlali(String mesaj) {
        super(mesaj);
    }
}
