package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.servis.KullaniciServisi;
import com.klinik.randevu.servis.OturumServisi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Her rolün kendi hesap bilgilerini görüp düzenlediği sayfa. Yalnızca oturumdaki kullanıcıya erişir. */
@Controller
@RequestMapping("/profil")
public class ProfilController {

    private final OturumServisi oturumServisi;
    private final KullaniciServisi kullaniciServisi;
    private final SecurityContextRepository oturumDeposu = new HttpSessionSecurityContextRepository();

    public ProfilController(OturumServisi oturumServisi, KullaniciServisi kullaniciServisi) {
        this.oturumServisi = oturumServisi;
        this.kullaniciServisi = kullaniciServisi;
    }

    @GetMapping
    public String profil() {
        return "profil";
    }

    @PostMapping("/iletisim")
    public String iletisimGuncelle(@RequestParam(required = false) String eposta,
                                   @RequestParam(required = false) String telefon,
                                   HttpServletRequest istek, HttpServletResponse yanit,
                                   RedirectAttributes yonlendirme) {
        Kullanici kullanici = oturumServisi.aktifKullanici();
        String eskiKimlik = kullanici.getGirisKimligi();
        kullaniciServisi.iletisimBilgisiGuncelle(kullanici, eposta, telefon);
        if (!kullanici.getGirisKimligi().equals(eskiKimlik)) {
            oturumKimliginiYenile(kullanici, istek, yanit);
            yonlendirme.addFlashAttribute("basari",
                    "Bilgileriniz kaydedildi. Bundan sonra " + kullanici.getGirisKimligi() + " ile giriş yapacaksınız.");
        } else {
            yonlendirme.addFlashAttribute("basari", "Bilgileriniz kaydedildi.");
        }
        return "redirect:/profil";
    }

    @PostMapping("/sifre")
    public String sifreDegistir(@RequestParam String mevcutSifre, @RequestParam String yeniSifre,
                                @RequestParam String yeniSifreTekrar, RedirectAttributes yonlendirme) {
        kullaniciServisi.sifreDegistir(oturumServisi.aktifKullanici(), mevcutSifre, yeniSifre, yeniSifreTekrar);
        yonlendirme.addFlashAttribute("basari", "Şifreniz değiştirildi.");
        return "redirect:/profil";
    }

    /** Personelin e-postası giriş kimliğidir; değişince oturumdaki kimlik de güncellenir. */
    private void oturumKimliginiYenile(Kullanici kullanici, HttpServletRequest istek, HttpServletResponse yanit) {
        Authentication eski = SecurityContextHolder.getContext().getAuthentication();
        Authentication yeni = UsernamePasswordAuthenticationToken.authenticated(
                kullanici.getGirisKimligi(), null, eski.getAuthorities());
        SecurityContext baglam = SecurityContextHolder.createEmptyContext();
        baglam.setAuthentication(yeni);
        SecurityContextHolder.setContext(baglam);
        oturumDeposu.saveContext(baglam, istek, yanit);
    }
}
