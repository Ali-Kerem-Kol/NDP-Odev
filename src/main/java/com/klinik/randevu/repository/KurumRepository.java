package com.klinik.randevu.repository;

import com.klinik.randevu.model.kurum.Kurum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Kurum kayıtlarına erişim.
 */
public interface KurumRepository extends JpaRepository<Kurum, Long> {

    List<Kurum> findAllByOrderByAdAsc();

    List<Kurum> findByAktifTrueOrderByAdAsc();
}
