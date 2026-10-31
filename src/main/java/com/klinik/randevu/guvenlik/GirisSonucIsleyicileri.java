package com.klinik.randevu.guvenlik;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

/** Giriş sonucunu kötüye kullanım korumasına bildiren işleyiciler. */
final class GirisSonucIsleyicileri {

    private GirisSonucIsleyicileri() {
    }

    /** Başarılı girişte hesabın sayacını sıfırlar, sonra istenen sayfaya ya da panele yönlendirir. */
    static final class Basari extends SavedRequestAwareAuthenticationSuccessHandler {

        private final KotuyeKullanimKorumasi koruma;

        Basari(KotuyeKullanimKorumasi koruma) {
            this.koruma = koruma;
            setDefaultTargetUrl("/panel");
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest istek, HttpServletResponse yanit, Authentication kimlik)
                throws ServletException, IOException {
            koruma.basariliGiris(istek.getParameter("kimlik"));
            super.onAuthenticationSuccess(istek, yanit, kimlik);
        }
    }

    /**
     * Her başarısız denemeyi sayar. Hata mesajı hesap var mı, şifre mi yanlış, hesap pasif mi
     * ayrımını yapmaz; böylece giriş ekranından hesap taraması yapılamaz.
     */
    static final class Hata implements AuthenticationFailureHandler {

        private final KotuyeKullanimKorumasi koruma;

        Hata(KotuyeKullanimKorumasi koruma) {
            this.koruma = koruma;
        }

        @Override
        public void onAuthenticationFailure(HttpServletRequest istek, HttpServletResponse yanit,
                                            AuthenticationException hata) throws IOException {
            String kimlik = istek.getParameter("kimlik");
            String ip = istek.getRemoteAddr();
            if (koruma.basarisizGiris(kimlik, ip)) {
                long dakika = koruma.girisEngeli(kimlik, ip).map(KotuyeKullanimKorumasi::dakika).orElse(1L);
                yanit.sendRedirect(istek.getContextPath() + "/giris?kilitli=" + dakika);
                return;
            }
            yanit.sendRedirect(istek.getContextPath() + "/giris?hata");
        }
    }
}
