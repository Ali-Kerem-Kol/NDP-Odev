package com.klinik.randevu.repository;

import com.klinik.randevu.model.kurum.Hizmet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Kurumların sağlık hizmetlerine erişim.
 */
public interface HizmetRepository extends JpaRepository<Hizmet, Long> {

    List<Hizmet> findByKurumIdOrderByAdAsc(Long kurumId);

    List<Hizmet> findByKurumIdAndAktifTrueOrderByAdAsc(Long kurumId);
}
