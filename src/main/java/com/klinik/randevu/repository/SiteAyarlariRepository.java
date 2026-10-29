package com.klinik.randevu.repository;

import com.klinik.randevu.model.icerik.SiteAyarlari;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ana sayfa ayarlarının tek kaydına erişim.
 */
public interface SiteAyarlariRepository extends JpaRepository<SiteAyarlari, Long> {
}
