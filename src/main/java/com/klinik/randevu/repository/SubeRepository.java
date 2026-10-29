package com.klinik.randevu.repository;

import com.klinik.randevu.model.kurum.Sube;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Şube kayıtlarına erişim.
 */
public interface SubeRepository extends JpaRepository<Sube, Long> {

    List<Sube> findByKurumIdOrderByAdAsc(Long kurumId);
}
