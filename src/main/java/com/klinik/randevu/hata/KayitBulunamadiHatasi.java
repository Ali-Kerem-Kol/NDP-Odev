package com.klinik.randevu.hata;

/**
 * İstenen kayıt yoksa fırlatılır. {@link IsKuraliIhlali}'nden türer; böylece tek bir yakalayıcı
 * tüm iş kuralı hatalarını aynı biçimde işler (istisna kalıtımı).
 */
public class KayitBulunamadiHatasi extends IsKuraliIhlali {

    public KayitBulunamadiHatasi(String kayitTuru, Object id) {
        super(kayitTuru + " bulunamadı (id: " + id + ")");
    }
}
