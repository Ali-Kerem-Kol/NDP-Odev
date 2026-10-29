package com.klinik.randevu.servis.kural;

import java.time.LocalDateTime;

/**
 * Çakışma kurallarının "bu aralık dolu mu?" sorusunu sorduğu kaynak.
 * <p>
 * Tek bir randevu kaydedilirken her soru doğrudan veritabanına gider
 * ({@link VeritabaniDolulukBilgisi}). Haftalık müsaitlik takvimi çizilirken ise
 * yüzlerce aday saat denenir; bu durumda aralıktaki randevular bir kez yüklenip
 * bellekte sorgulanır ({@link TakvimDolulukBilgisi}). Kurallar hangisiyle
 * çalıştıklarını bilmez; ikisi de aynı arayüzü uygular.
 */
public interface DolulukBilgisi {

    boolean calisanDoluMu(Long calisanId, LocalDateTime baslangic, LocalDateTime bitis);

    boolean hastaDoluMu(Long hastaId, LocalDateTime baslangic, LocalDateTime bitis);
}
