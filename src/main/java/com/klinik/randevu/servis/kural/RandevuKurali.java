package com.klinik.randevu.servis.kural;

/**
 * Randevu oluşturulmadan önce uygulanan tek bir iş kuralı (Strateji deseni).
 * <p>
 * Her kural ayrı bir sınıftır; {@code RandevuServisi} tüm uygulamaları bir liste
 * olarak alır ve sırayla çalıştırır. Yeni bir kural eklemek için mevcut koda
 * dokunmadan yeni bir sınıf yazmak yeterlidir (Açık/Kapalı prensibi).
 */
public interface RandevuKurali {

    /**
     * Kural ihlal edilirse {@link com.klinik.randevu.hata.IsKuraliIhlali} fırlatır.
     * Doluluğa bakması gereken kurallar ikinci parametreyi kullanır.
     */
    void dogrula(RandevuTalebi talep, DolulukBilgisi doluluk);
}
