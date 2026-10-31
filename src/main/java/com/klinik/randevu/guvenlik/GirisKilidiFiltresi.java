package com.klinik.randevu.guvenlik;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Kilitli hesap ya da cihazdan gelen giriş isteğini şifre kontrolüne hiç ulaştırmadan geri çevirir.
 * Böylece kilit süresince doğru şifre de denenemez.
 */
public class GirisKilidiFiltresi extends OncePerRequestFilter {

    private final KotuyeKullanimKorumasi koruma;

    public GirisKilidiFiltresi(KotuyeKullanimKorumasi koruma) {
        this.koruma = koruma;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest istek) {
        // servletPath sunucu yapılandırmasına göre boş gelebilir; yolu istek adresinden çıkarıyoruz
        String yol = istek.getRequestURI().substring(istek.getContextPath().length());
        return !("POST".equals(istek.getMethod()) && "/giris".equals(yol));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse yanit, FilterChain zincir)
            throws ServletException, IOException {
        Optional<Duration> engel = koruma.girisEngeli(istek.getParameter("kimlik"), istek.getRemoteAddr());
        if (engel.isPresent()) {
            yanit.sendRedirect(istek.getContextPath() + "/giris?kilitli=" + KotuyeKullanimKorumasi.dakika(engel.get()));
            return;
        }
        zincir.doFilter(istek, yanit);
    }
}
