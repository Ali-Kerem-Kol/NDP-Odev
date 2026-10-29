package com.klinik.randevu.repository;

import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kullanici.Yonetici;
import com.klinik.randevu.model.kullanici.Hasta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Tüm kullanıcı türlerine tek depodan erişim. Tek tablo kalıtımı sayesinde sorgu sonucu
 * doğrudan doğru alt sınıfın nesnesi olarak gelir (Hasta, Doktor, Yonetici...).
 */
public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {

    Optional<Kullanici> findByGirisKimligi(String girisKimligi);

    boolean existsByGirisKimligi(String girisKimligi);

    /** Aynı hastanın farklı doktorlara eşzamanlı talepleri de sırayla doğrulansın diye. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hasta h where h.id = :id")
    Optional<Hasta> hastayiKilitle(@Param("id") Long id);

    @Query("select y from Yonetici y order by y.kurum.ad, y.adSoyad")
    List<Yonetici> tumYoneticiler();

    @Query("select y from Yonetici y where y.kurum.id = :kurumId")
    List<Yonetici> kurumYoneticileri(@Param("kurumId") Long kurumId);
}
