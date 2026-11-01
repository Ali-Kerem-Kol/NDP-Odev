package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.servis.OturumServisi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Pasifleştirilen bir hesabın açık oturumu, bir sonraki istekte kapatılır.
 * (Yeni girişi zaten Spring Security engeller; bu sınıf hâlihazırda açık olan oturumlar içindir.)
 */
@Component
public class PasifHesapKontrolu implements HandlerInterceptor {

    private final OturumServisi oturumServisi;

    public PasifHesapKontrolu(OturumServisi oturumServisi) {
        this.oturumServisi = oturumServisi;
    }

    @Override
    public boolean preHandle(HttpServletRequest istek, HttpServletResponse yanit, Object isleyici) throws Exception {
        Optional<Kullanici> kullanici = oturumServisi.aktifKullaniciBul();
        boolean oturumVar = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if (oturumVar && (kullanici.isEmpty() || !kullanici.get().isAktif())) {
            SecurityContextHolder.clearContext();
            HttpSession oturum = istek.getSession(false);
            if (oturum != null) {
                oturum.invalidate();
            }
            yanit.sendRedirect(istek.getContextPath() + "/giris?pasif");
            return false;
        }
        return true;
    }
}
