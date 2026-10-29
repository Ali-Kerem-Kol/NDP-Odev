package com.klinik.randevu.servis;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.repository.HizmetRepository;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.RandevuRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Aynı anda gelen talepler çakışma kontrolünü atlatamamalı.
 * Gerçek eşzamanlılık için işlem (transaction) kullanmaz; oluşturduğu randevuları sonunda siler.
 */
@SpringBootTest
class EszamanliRandevuTest {

    private static final int TUR = 10;

    @Autowired
    private RandevuServisi randevuServisi;
    @Autowired
    private KullaniciRepository kullaniciRepository;
    @Autowired
    private RandevuRepository randevuRepository;
    @Autowired
    private HizmetRepository hizmetRepository;

    private final List<Long> olusanlar = Collections.synchronizedList(new ArrayList<>());
    private Hasta hasta1;
    private Hasta hasta2;
    private Calisan drAyse;
    private Calisan drMehmet;

    @BeforeEach
    void hazirla() {
        hasta1 = (Hasta) kullaniciRepository.findByGirisKimligi("62601815964").orElseThrow();
        hasta2 = (Hasta) kullaniciRepository.findByGirisKimligi("18301661332").orElseThrow();
        drAyse = (Calisan) kullaniciRepository.findByGirisKimligi("ayse.demir@sakaryasaglik.com.tr").orElseThrow();
        drMehmet = (Calisan) kullaniciRepository.findByGirisKimligi("mehmet.ozturk@sakaryasaglik.com.tr").orElseThrow();
    }

    @AfterEach
    void temizle() {
        randevuRepository.deleteAllById(olusanlar);
    }

    @Test
    void ayniDoktoraAyniSaateEszamanliIkiTaleptenYalnizBiriKabulEdilir() throws Exception {
        Long hizmetId = hizmetId(drAyse, "Genel Muayene");
        for (int tur = 0; tur < TUR; tur++) {
            LocalDateTime zaman = gun(tur).atTime(10, 0);
            int kabul = esZamanli(
                    () -> randevuServisi.randevuOlustur(hasta1, drAyse.getId(), hizmetId, zaman, null),
                    () -> randevuServisi.randevuOlustur(hasta2, drAyse.getId(), hizmetId, zaman, null));
            assertEquals(1, kabul, "tur " + tur + ": aynı doktor ve saat için tek randevu kabul edilmeli");
        }
    }

    @Test
    void ayniHastaAyniSaateIkiFarkliDoktordanYalnizBirineRandevuAlir() throws Exception {
        Long muayene = hizmetId(drAyse, "Genel Muayene");
        Long ekg = hizmetId(drMehmet, "EKG");
        for (int tur = 0; tur < TUR; tur++) {
            LocalDateTime zaman = gun(tur).atTime(14, 0);
            int kabul = esZamanli(
                    () -> randevuServisi.randevuOlustur(hasta1, drAyse.getId(), muayene, zaman, null),
                    () -> randevuServisi.randevuOlustur(hasta1, drMehmet.getId(), ekg, zaman, null));
            assertEquals(1, kabul, "tur " + tur + ": hasta aynı saatte iki randevu alamamalı");
        }
    }

    /** İki talebi bir bariyerle aynı anda başlatır; kabul edilenlerin sayısını döndürür. */
    private int esZamanli(Supplier<Randevu> birinci, Supplier<Randevu> ikinci) throws Exception {
        CyclicBarrier bariyer = new CyclicBarrier(2);
        ExecutorService havuz = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> sonuclar = new ArrayList<>();
            for (Supplier<Randevu> talep : List.of(birinci, ikinci)) {
                sonuclar.add(havuz.submit(() -> {
                    bariyer.await();
                    try {
                        olusanlar.add(talep.get().getId());
                        return true;
                    } catch (RuntimeException reddedildi) {
                        return false;
                    }
                }));
            }
            int kabul = 0;
            for (Future<Boolean> sonuc : sonuclar) {
                if (sonuc.get()) {
                    kabul++;
                }
            }
            return kabul;
        } finally {
            havuz.shutdown();
        }
    }

    /** Örnek veriyle karışmasın diye uzak gelecekte, her tur farklı bir Salı. */
    private static LocalDate gun(int tur) {
        return LocalDate.now().plusWeeks(20 + tur).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
    }

    private Long hizmetId(Calisan calisan, String ad) {
        return hizmetRepository.findByKurumIdOrderByAdAsc(calisan.getSube().getKurum().getId()).stream()
                .filter(h -> h.getAd().equals(ad)).findFirst().orElseThrow().getId();
    }
}
