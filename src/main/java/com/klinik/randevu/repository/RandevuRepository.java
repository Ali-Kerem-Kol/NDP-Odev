package com.klinik.randevu.repository;

import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Randevu kayıtlarına erişim. Çakışma ve doluluk sorguları, zamanlayıcının kullandığı
 * durum sorguları burada tanımlıdır.
 */
public interface RandevuRepository extends JpaRepository<Randevu, Long> {

    List<Randevu> findByHastaIdOrderByBaslangicDesc(Long hastaId);

    List<Randevu> findByCalisanIdOrderByBaslangicAsc(Long calisanId);

    List<Randevu> findBySubeKurumIdOrderByBaslangicDesc(Long kurumId);

    long countBySubeKurumIdAndDurum(Long kurumId, RandevuDurumu durum);

    boolean existsBySubeKurumId(Long kurumId);

    boolean existsBySubeId(Long subeId);

    List<Randevu> findByDurumAndBaslangicLessThanEqual(RandevuDurumu durum, LocalDateTime zaman);

    long countByCalisanIdAndDurumInAndBaslangicAfter(Long calisanId, Collection<RandevuDurumu> durumlar,
                                                    LocalDateTime zaman);

    List<Randevu> findByDurumAndBitisBefore(RandevuDurumu durum, LocalDateTime zaman);

    /** Müsaitlik takvimi için: çalışanın veya hastanın aralıktaki aktif randevuları, tek sorguda. */
    @Query("""
            select r from Randevu r
            where (r.calisan.id = :calisanId or r.hasta.id = :hastaId) and r.durum in :durumlar
              and r.baslangic < :bitis and r.bitis > :baslangic
            """)
    List<Randevu> aktifRandevular(@Param("calisanId") Long calisanId,
                                  @Param("hastaId") Long hastaId,
                                  @Param("baslangic") LocalDateTime baslangic,
                                  @Param("bitis") LocalDateTime bitis,
                                  @Param("durumlar") Collection<RandevuDurumu> durumlar);

    /** İki aralık [a,b) ve [c,d) şu koşulda çakışır: a < d ve c < b. */
    @Query("""
            select count(r) > 0 from Randevu r
            where r.calisan.id = :calisanId and r.durum in :durumlar
              and r.baslangic < :bitis and r.bitis > :baslangic
            """)
    boolean calisanIcinCakismaVar(@Param("calisanId") Long calisanId,
                                  @Param("baslangic") LocalDateTime baslangic,
                                  @Param("bitis") LocalDateTime bitis,
                                  @Param("durumlar") Collection<RandevuDurumu> durumlar);

    @Query("""
            select count(r) > 0 from Randevu r
            where r.hasta.id = :hastaId and r.durum in :durumlar
              and r.baslangic < :bitis and r.bitis > :baslangic
            """)
    boolean hastaIcinCakismaVar(@Param("hastaId") Long hastaId,
                                @Param("baslangic") LocalDateTime baslangic,
                                @Param("bitis") LocalDateTime bitis,
                                @Param("durumlar") Collection<RandevuDurumu> durumlar);
}
