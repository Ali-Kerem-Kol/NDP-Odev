package com.klinik.randevu.servis.kural;

import com.klinik.randevu.model.randevu.Randevu;

import java.time.LocalDateTime;
import java.util.List;

/** Önceden yüklenmiş randevu listesi üzerinden, veritabanına gitmeden yanıt verir. */
public class TakvimDolulukBilgisi implements DolulukBilgisi {

    private final List<Randevu> randevular;

    public TakvimDolulukBilgisi(List<Randevu> randevular) {
        this.randevular = List.copyOf(randevular);
    }

    @Override
    public boolean calisanDoluMu(Long calisanId, LocalDateTime baslangic, LocalDateTime bitis) {
        return randevular.stream().anyMatch(r -> r.getCalisan().getId().equals(calisanId) && r.cakisir(baslangic, bitis));
    }

    @Override
    public boolean hastaDoluMu(Long hastaId, LocalDateTime baslangic, LocalDateTime bitis) {
        return randevular.stream().anyMatch(r -> r.getHasta().getId().equals(hastaId) && r.cakisir(baslangic, bitis));
    }
}
