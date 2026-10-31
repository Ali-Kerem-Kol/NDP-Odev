package com.klinik.randevu.guvenlik;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bir anahtar (hesap kimliği, IP adresi...) için belirli bir pencerede yapılan başarısız
 * denemeleri sayar; sınır aşılınca anahtarı belirli bir süre kilitler.
 * <p>
 * Aynı sınıf giriş ve kayıt korumasında farklı ayarlarla kullanılır. Saat dışarıdan
 * verildiği için zamana bağlı davranış testte beklemeden doğrulanabilir.
 */
public class DenemeSinirlayici {

    /** Bellek şişmesin diye bu kadar anahtardan sonra süresi dolanlar temizlenir. */
    private static final int TEMIZLIK_ESIGI = 10_000;

    private final int azamiDeneme;
    private final Duration pencere;
    private final Duration kilitSuresi;
    private final Clock saat;
    private final Map<String, Durum> durumlar = new HashMap<>();

    private static final class Durum {
        private final Deque<Instant> denemeler = new ArrayDeque<>();
        private Instant kilitBitisi;
    }

    public DenemeSinirlayici(int azamiDeneme, Duration pencere, Duration kilitSuresi, Clock saat) {
        if (azamiDeneme < 1 || pencere.isNegative() || pencere.isZero() || kilitSuresi.isNegative() || kilitSuresi.isZero()) {
            throw new IllegalArgumentException("Deneme sınırı ve süreler pozitif olmalıdır.");
        }
        this.azamiDeneme = azamiDeneme;
        this.pencere = pencere;
        this.kilitSuresi = kilitSuresi;
        this.saat = saat;
    }

    /** Anahtar kilitliyse kilidin bitmesine kalan süre. */
    public synchronized Optional<Duration> kalanKilit(String anahtar) {
        Durum durum = durumlar.get(anahtar);
        if (durum == null || durum.kilitBitisi == null) {
            return Optional.empty();
        }
        Instant simdi = saat.instant();
        if (!simdi.isBefore(durum.kilitBitisi)) {
            durum.kilitBitisi = null;
            return Optional.empty();
        }
        return Optional.of(Duration.between(simdi, durum.kilitBitisi));
    }

    /**
     * Başarısız bir deneme kaydeder.
     * @return bu deneme ile anahtar kilitlendiyse true
     */
    public synchronized boolean basarisizKaydet(String anahtar) {
        if (durumlar.size() > TEMIZLIK_ESIGI) {
            suresiDolanlariTemizle();
        }
        Instant simdi = saat.instant();
        Durum durum = durumlar.computeIfAbsent(anahtar, k -> new Durum());
        while (!durum.denemeler.isEmpty() && !durum.denemeler.peekFirst().isAfter(simdi.minus(pencere))) {
            durum.denemeler.pollFirst();
        }
        durum.denemeler.addLast(simdi);
        if (durum.denemeler.size() >= azamiDeneme) {
            durum.kilitBitisi = simdi.plus(kilitSuresi);
            durum.denemeler.clear();
            return true;
        }
        return false;
    }

    /** Başarılı girişte hesabın sayacı sıfırlanır. */
    public synchronized void sifirla(String anahtar) {
        durumlar.remove(anahtar);
    }

    /** Testler ve yönetim amaçlı: tüm sayaçları siler. */
    public synchronized void tumunuSifirla() {
        durumlar.clear();
    }

    private void suresiDolanlariTemizle() {
        Instant esik = saat.instant().minus(pencere);
        durumlar.values().removeIf(d -> (d.kilitBitisi == null || !saat.instant().isBefore(d.kilitBitisi))
                && (d.denemeler.isEmpty() || !d.denemeler.peekLast().isAfter(esik)));
    }

    public Duration getKilitSuresi() {
        return kilitSuresi;
    }
}
