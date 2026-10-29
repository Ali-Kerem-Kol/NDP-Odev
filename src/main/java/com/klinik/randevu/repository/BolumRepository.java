package com.klinik.randevu.repository;

import com.klinik.randevu.model.kurum.Bolum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Bölüm kayıtlarına erişim (Repository deseni; uygulamasını Spring Data üretir).
 */
public interface BolumRepository extends JpaRepository<Bolum, Long> {

    List<Bolum> findBySubeKurumIdOrderBySubeAdAscAdAsc(Long kurumId);
}
