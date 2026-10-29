package com.klinik.randevu.servis.kural;

import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.repository.RandevuRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Her soruyu güncel veritabanı durumuna göre yanıtlar; randevu kaydederken kullanılır. */
@Component
public class VeritabaniDolulukBilgisi implements DolulukBilgisi {

    private final RandevuRepository randevuRepository;

    public VeritabaniDolulukBilgisi(RandevuRepository randevuRepository) {
        this.randevuRepository = randevuRepository;
    }

    @Override
    public boolean calisanDoluMu(Long calisanId, LocalDateTime baslangic, LocalDateTime bitis) {
        return randevuRepository.calisanIcinCakismaVar(calisanId, baslangic, bitis, RandevuDurumu.aktifDurumlar());
    }

    @Override
    public boolean hastaDoluMu(Long hastaId, LocalDateTime baslangic, LocalDateTime bitis) {
        return randevuRepository.hastaIcinCakismaVar(hastaId, baslangic, bitis, RandevuDurumu.aktifDurumlar());
    }
}
