package com.klinik.randevu.servis;

import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.repository.KullaniciRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Oturumu açık kullanıcının alan nesnesine (Hasta, Doktor, Yonetici...) erişim sağlar. */
@Service
public class OturumServisi {

    private final KullaniciRepository kullaniciRepository;

    public OturumServisi(KullaniciRepository kullaniciRepository) {
        this.kullaniciRepository = kullaniciRepository;
    }

    public Optional<Kullanici> aktifKullaniciBul() {
        Authentication kimlik = SecurityContextHolder.getContext().getAuthentication();
        if (kimlik == null || !kimlik.isAuthenticated() || kimlik instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return kullaniciRepository.findByGirisKimligi(kimlik.getName());
    }

    public Kullanici aktifKullanici() {
        return aktifKullaniciBul().orElseThrow(() -> new YetkisizIslemHatasi("Oturum açmanız gerekiyor."));
    }

    public <T extends Kullanici> T aktifKullanici(Class<T> beklenenTur) {
        Kullanici kullanici = aktifKullanici();
        if (!beklenenTur.isInstance(kullanici)) {
            throw new YetkisizIslemHatasi("Bu sayfa için yetkiniz yok.");
        }
        return beklenenTur.cast(kullanici);
    }
}
