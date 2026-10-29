package com.klinik.randevu.servis;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.KayitBulunamadiHatasi;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kullanici.Yonetici;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.repository.KullaniciRepository;
import com.klinik.randevu.repository.KurumRepository;
import com.klinik.randevu.web.form.HastaKayitFormu;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hesap işlemleri: hasta kaydı, kurum yöneticisi atama ve hesap kapatma, iletişim bilgisi ve şifre değişikliği.
 * Şifreler yalnızca BCrypt özeti olarak saklanır.
 */
@Service
@Transactional
public class KullaniciServisi {

    public static final int ASGARI_SIFRE_UZUNLUGU = 8;

    private final KullaniciRepository kullaniciRepository;
    private final KurumRepository kurumRepository;
    private final PasswordEncoder sifreleyici;

    public KullaniciServisi(KullaniciRepository kullaniciRepository, KurumRepository kurumRepository,
                            PasswordEncoder sifreleyici) {
        this.kullaniciRepository = kullaniciRepository;
        this.kurumRepository = kurumRepository;
        this.sifreleyici = sifreleyici;
    }

    public Hasta hastaKaydet(HastaKayitFormu form) {
        if (kullaniciRepository.existsByGirisKimligi(Kullanici.kimligiNormallestir(form.getTcKimlikNo()))) {
            // Mesaj hesabın var olduğunu açıkça doğrulamaz; deneme sayısı da cihaz başına sınırlıdır
            throw new IsKuraliIhlali("Bu bilgilerle yeni hesap oluşturulamadı. Daha önce kaydolduysanız giriş yapın.");
        }
        sifreKontrolEt(form.getSifre());
        Hasta hasta = new Hasta(form.getTcKimlikNo(), sifreleyici.encode(form.getSifre()),
                form.getAdSoyad(), form.getDogumTarihi());
        hasta.epostaGuncelle(form.getEposta());
        hasta.telefonGuncelle(form.getTelefon());
        return kullaniciRepository.save(hasta);
    }

    public Yonetici yoneticiOlustur(Kullanici yapan, Long kurumId, String eposta, String sifre, String adSoyad) {
        if (!yapan.kurumTanimlayabilirMi()) {
            throw new YetkisizIslemHatasi("Kurum yöneticisini yalnızca sistem yöneticisi atayabilir.");
        }
        sifreKontrolEt(sifre);
        epostaKullanilabilirMi(eposta, null);
        Kurum kurum = kurumRepository.findById(kurumId)
                .orElseThrow(() -> new KayitBulunamadiHatasi("Kurum", kurumId));
        return kullaniciRepository.save(new Yonetici(eposta, sifreleyici.encode(sifre), adSoyad, kurum));
    }

    /** Kapatılan yönetici hesabı giriş yapamaz, açık oturumu da bir sonraki istekte sonlanır. */
    public Yonetici yoneticiHesabiDurumDegistir(Kullanici yapan, Long yoneticiId) {
        if (!yapan.kurumTanimlayabilirMi()) {
            throw new YetkisizIslemHatasi("Yönetici hesaplarını yalnızca sistem yöneticisi açıp kapatabilir.");
        }
        Yonetici yonetici = kullaniciRepository.findById(yoneticiId)
                .filter(Yonetici.class::isInstance).map(Yonetici.class::cast)
                .orElseThrow(() -> new KayitBulunamadiHatasi("Yönetici", yoneticiId));
        if (yonetici.isAktif()) {
            yonetici.pasiflestir();
        } else {
            yonetici.aktiflestir();
        }
        return yonetici;
    }

    /** Herkes kendi iletişim bilgisini değiştirebilir; personelde e-posta aynı zamanda giriş kimliğidir. */
    public void iletisimBilgisiGuncelle(Kullanici kullanici, String eposta, String telefon) {
        if (kullanici.girisEpostaIleMi()) {
            epostaKullanilabilirMi(eposta, kullanici);
        }
        kullanici.epostaGuncelle(eposta);
        kullanici.telefonGuncelle(telefon);
        kullaniciRepository.save(kullanici);
    }

    public void sifreDegistir(Kullanici kullanici, String mevcutSifre, String yeniSifre, String yeniSifreTekrar) {
        if (mevcutSifre == null || !sifreleyici.matches(mevcutSifre, kullanici.getSifre())) {
            throw new IsKuraliIhlali("Mevcut şifreniz hatalı.");
        }
        sifreKontrolEt(yeniSifre);
        if (!yeniSifre.equals(yeniSifreTekrar)) {
            throw new IsKuraliIhlali("Yeni şifreler eşleşmiyor.");
        }
        if (sifreleyici.matches(yeniSifre, kullanici.getSifre())) {
            throw new IsKuraliIhlali("Yeni şifre mevcut şifrenizle aynı olamaz.");
        }
        kullanici.sifreGuncelle(sifreleyici.encode(yeniSifre));
        kullaniciRepository.save(kullanici);
    }

    @Transactional(readOnly = true)
    public List<Yonetici> yoneticiler() {
        return kullaniciRepository.tumYoneticiler();
    }

    /** Kurumsal e-posta başka bir hesabın giriş kimliği olarak kullanılıyor mu? */
    void epostaKullanilabilirMi(String eposta, Kullanici sahibi) {
        if (eposta == null || eposta.isBlank()) {
            throw new IsKuraliIhlali("Kurumsal e-posta zorunludur.");
        }
        kullaniciRepository.findByGirisKimligi(Kullanici.kimligiNormallestir(eposta))
                .filter(mevcut -> !mevcut.equals(sahibi))
                .ifPresent(mevcut -> {
                    throw new IsKuraliIhlali("'" + eposta.trim() + "' adresi başka bir hesapta kayıtlı.");
                });
    }

    static void sifreKontrolEt(String sifre) {
        if (sifre == null || sifre.length() < ASGARI_SIFRE_UZUNLUGU) {
            throw new IsKuraliIhlali("Şifre en az " + ASGARI_SIFRE_UZUNLUGU + " karakter olmalıdır.");
        }
    }
}
