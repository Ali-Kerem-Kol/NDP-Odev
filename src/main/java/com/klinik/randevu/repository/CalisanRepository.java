package com.klinik.randevu.repository;

import com.klinik.randevu.model.kullanici.Calisan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Doktor ve sağlık personeli kayıtlarına erişim. Randevu kaydında çakışmayı önlemek için
 * çalışan satırını kilitleyerek okuyan sorgu da buradadır.
 */
public interface CalisanRepository extends JpaRepository<Calisan, Long> {

    List<Calisan> findBySubeKurumIdOrderByAdSoyadAsc(Long kurumId);

    boolean existsBySubeKurumId(Long kurumId);

    boolean existsBySubeId(Long subeId);

    boolean existsByBolumId(Long bolumId);

    /**
     * Randevu kaydederken çalışanın satırını yazma kilidiyle okur (SELECT ... FOR UPDATE).
     * Aynı çalışana gelen eşzamanlı talepler böylece sırayla doğrulanır.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Calisan c where c.id = :id")
    Optional<Calisan> kilitleyerekBul(@Param("id") Long id);

    /** Belirli şubede, belirli hizmeti veren aktif çalışanlar. */
    @Query("""
            select distinct c from Calisan c join c.hizmetler h
            where c.sube.id = :subeId and h.id = :hizmetId and c.aktif = true
            order by c.adSoyad
            """)
    List<Calisan> hizmetVerenler(@Param("subeId") Long subeId, @Param("hizmetId") Long hizmetId);
}
