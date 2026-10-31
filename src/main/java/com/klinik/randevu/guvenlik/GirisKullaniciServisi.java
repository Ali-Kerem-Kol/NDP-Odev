package com.klinik.randevu.guvenlik;

import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.repository.KullaniciRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security girişini alan modelindeki {@link Kullanici} kayıtlarına bağlar.
 * Giriş alanına hasta T.C. Kimlik No'sunu, personel kurumsal e-postasını yazar;
 * ikisi de aynı benzersiz "giriş kimliği" sütununda tutulduğu için tek sorgu yeterlidir.
 */
@Service
public class GirisKullaniciServisi implements UserDetailsService {

    private final KullaniciRepository kullaniciRepository;

    public GirisKullaniciServisi(KullaniciRepository kullaniciRepository) {
        this.kullaniciRepository = kullaniciRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String girilenKimlik) {
        String kimlik = Kullanici.kimligiNormallestir(girilenKimlik);
        Kullanici kullanici = kullaniciRepository.findByGirisKimligi(kimlik)
                .orElseThrow(() -> new UsernameNotFoundException("Kayıt bulunamadı"));
        return User.withUsername(kullanici.getGirisKimligi())
                .password(kullanici.getSifre())
                .authorities(kullanici.getRol().getYetkiAdi())
                .disabled(!kullanici.isAktif())
                .build();
    }
}
