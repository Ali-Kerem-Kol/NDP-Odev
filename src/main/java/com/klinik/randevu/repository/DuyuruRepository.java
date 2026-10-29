package com.klinik.randevu.repository;

import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.icerik.DuyuruKategorisi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Duyuru, haber ve sağlık rehberi içeriklerine erişim.
 */
public interface DuyuruRepository extends JpaRepository<Duyuru, Long> {

    Page<Duyuru> findByYayindaTrueOrderByYayinZamaniDescIdDesc(Pageable sayfa);

    Page<Duyuru> findByYayindaTrueAndKategoriOrderByYayinZamaniDescIdDesc(DuyuruKategorisi kategori, Pageable sayfa);

    Optional<Duyuru> findFirstByYayindaTrueAndOnecikanTrue();

    List<Duyuru> findAllByOrderByGuncellemeZamaniDesc();

    List<Duyuru> findByKurumIdOrderByGuncellemeZamaniDesc(Long kurumId);

    long countByYayindaTrue();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Duyuru d set d.onecikan = false where d.onecikan = true")
    void tumOnecikarmalariKaldir();
}
