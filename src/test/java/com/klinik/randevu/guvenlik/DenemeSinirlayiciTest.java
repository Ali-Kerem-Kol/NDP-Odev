package com.klinik.randevu.guvenlik;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Zamana bağlı davranış, elle ilerletilen bir saatle beklemeden doğrulanır. */
class DenemeSinirlayiciTest {

    /** Testte istenildiği kadar ileri sarılabilen saat. */
    private static final class ElleSaat extends Clock {
        private Instant simdi = Instant.parse("2026-10-21T10:00:00Z");

        void ilerlet(Duration sure) {
            simdi = simdi.plus(sure);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return simdi;
        }
    }

    private final ElleSaat saat = new ElleSaat();
    private final DenemeSinirlayici sinirlayici =
            new DenemeSinirlayici(5, Duration.ofMinutes(15), Duration.ofMinutes(15), saat);

    @Test
    void besinciHataliDenemedeKilitlenirVeSureBitinceAcilir() {
        for (int i = 1; i <= 4; i++) {
            assertFalse(sinirlayici.basarisizKaydet("hesap"), i + ". deneme kilitlememeli");
        }
        assertTrue(sinirlayici.basarisizKaydet("hesap"), "5. deneme kilitler");
        assertEquals(Duration.ofMinutes(15), sinirlayici.kalanKilit("hesap").orElseThrow());

        saat.ilerlet(Duration.ofMinutes(14));
        assertEquals(Duration.ofMinutes(1), sinirlayici.kalanKilit("hesap").orElseThrow());

        saat.ilerlet(Duration.ofMinutes(1));
        assertTrue(sinirlayici.kalanKilit("hesap").isEmpty(), "süre bitince kilit kalkar");
        assertFalse(sinirlayici.basarisizKaydet("hesap"), "kilit sonrası sayaç sıfırdan başlar");
    }

    @Test
    void pencereDisinaCikanEskiDenemelerSayilmaz() {
        for (int i = 0; i < 4; i++) {
            sinirlayici.basarisizKaydet("hesap");
        }
        saat.ilerlet(Duration.ofMinutes(16));
        assertFalse(sinirlayici.basarisizKaydet("hesap"), "15 dakikadan eski denemeler düşer");
        assertTrue(sinirlayici.kalanKilit("hesap").isEmpty());
    }

    @Test
    void sifirlamaSayaciTemizlerVeAnahtarlarBirbirindenBagimsizdir() {
        for (int i = 0; i < 4; i++) {
            sinirlayici.basarisizKaydet("hesap");
        }
        sinirlayici.sifirla("hesap");
        for (int i = 0; i < 4; i++) {
            assertFalse(sinirlayici.basarisizKaydet("hesap"));
        }
        for (int i = 0; i < 5; i++) {
            sinirlayici.basarisizKaydet("baska");
        }
        assertTrue(sinirlayici.kalanKilit("baska").isPresent());
        assertTrue(sinirlayici.kalanKilit("hesap").isEmpty(), "bir anahtarın kilidi diğerini etkilemez");
    }

    @Test
    void gecersizAyarReddedilir() {
        assertThrows(IllegalArgumentException.class,
                () -> new DenemeSinirlayici(0, Duration.ofMinutes(1), Duration.ofMinutes(1), saat));
    }

    @Test
    void kalanSureKullaniciyaYukariYuvarlanmisDakikaOlarakGosterilir() {
        assertEquals(1, KotuyeKullanimKorumasi.dakika(Duration.ofSeconds(5)));
        assertEquals(15, KotuyeKullanimKorumasi.dakika(Duration.ofMinutes(14).plusSeconds(1)));
    }
}
