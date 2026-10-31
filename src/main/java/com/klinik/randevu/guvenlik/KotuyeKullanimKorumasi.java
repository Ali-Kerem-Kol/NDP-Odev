package com.klinik.randevu.guvenlik;

import com.klinik.randevu.model.kullanici.Kullanici;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Giriş ve kayıt ekranlarını kaba kuvvet ve hesap taramasına karşı korur.
 * <ul>
 *   <li><b>Hesap bazında:</b> aynı kimliğe art arda yanlış şifre → o kimlik bir süre kilitlenir.
 *       Olmayan kimlikler de aynı şekilde kilitlenir; böylece kilit mesajı hesabın var olup
 *       olmadığını ele vermez.</li>
 *   <li><b>Cihaz (IP) bazında:</b> tek kaynaktan çok sayıda farklı hesabı denemek de durdurulur.</li>
 *   <li><b>Kayıt:</b> cihaz başına saatlik deneme sınırı; T.C. Kimlik No taraması pratikte imkânsızlaşır.</li>
 * </ul>
 * Sayaçlar bellektedir; uygulama yeniden başlayınca sıfırlanır.
 */
@Component
public class KotuyeKullanimKorumasi {

    private final DenemeSinirlayici hesapSiniri;
    private final DenemeSinirlayici cihazGirisSiniri;
    private final DenemeSinirlayici cihazKayitSiniri;

    public KotuyeKullanimKorumasi(
            @Value("${guvenlik.giris.hesap-azami-deneme:5}") int hesapAzami,
            @Value("${guvenlik.giris.cihaz-azami-deneme:20}") int cihazAzami,
            @Value("${guvenlik.giris.pencere:PT15M}") Duration girisPenceresi,
            @Value("${guvenlik.giris.kilit-suresi:PT15M}") Duration girisKilidi,
            @Value("${guvenlik.kayit.cihaz-azami-deneme:10}") int kayitAzami,
            @Value("${guvenlik.kayit.pencere:PT1H}") Duration kayitPenceresi) {
        Clock saat = Clock.systemUTC();
        this.hesapSiniri = new DenemeSinirlayici(hesapAzami, girisPenceresi, girisKilidi, saat);
        this.cihazGirisSiniri = new DenemeSinirlayici(cihazAzami, girisPenceresi, girisKilidi, saat);
        this.cihazKayitSiniri = new DenemeSinirlayici(kayitAzami, kayitPenceresi, kayitPenceresi, saat);
    }

    // ---- Giriş ----

    /** Hesap ya da cihaz kilitliyse kalan süre (ikisinden uzun olanı). */
    public Optional<Duration> girisEngeli(String kimlik, String ip) {
        return Stream.of(hesapSiniri.kalanKilit(hesapAnahtari(kimlik)), cihazGirisSiniri.kalanKilit(ip))
                .flatMap(Optional::stream)
                .max(Duration::compareTo);
    }

    /** @return bu denemeyle hesap ya da cihaz kilitlendiyse true */
    public boolean basarisizGiris(String kimlik, String ip) {
        boolean hesapKilitlendi = hesapSiniri.basarisizKaydet(hesapAnahtari(kimlik));
        boolean cihazKilitlendi = cihazGirisSiniri.basarisizKaydet(ip);
        return hesapKilitlendi || cihazKilitlendi;
    }

    /** Doğru şifreyle girişte hesabın sayacı sıfırlanır (cihaz sayacı sıfırlanmaz). */
    public void basariliGiris(String kimlik) {
        hesapSiniri.sifirla(hesapAnahtari(kimlik));
    }

    // ---- Kayıt ----

    public Optional<Duration> kayitEngeli(String ip) {
        return cihazKayitSiniri.kalanKilit(ip);
    }

    /** Her kayıt denemesi (başarılı ya da değil) sayılır. */
    public void kayitDenemesi(String ip) {
        cihazKayitSiniri.basarisizKaydet(ip);
    }

    public void tumunuSifirla() {
        hesapSiniri.tumunuSifirla();
        cihazGirisSiniri.tumunuSifirla();
        cihazKayitSiniri.tumunuSifirla();
    }

    /** Kalan süreyi kullanıcıya gösterilecek tam dakikaya yuvarlar (en az 1). */
    public static long dakika(Duration sure) {
        return Math.max(1, (sure.getSeconds() + 59) / 60);
    }

    private static String hesapAnahtari(String kimlik) {
        String temiz = Kullanici.kimligiNormallestir(kimlik);
        return temiz == null ? "" : temiz;
    }
}
