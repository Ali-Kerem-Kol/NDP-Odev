package com.klinik.randevu.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

/**
 * İşlemden sonra kullanıcıyı geldiği sayfaya döndürmek için güvenli adres üretir.
 */
final class GeriDonus {

    private GeriDonus() {
    }

    /**
     * İsteğin geldiği sayfanın yolunu döndürür. Yalnızca yol ve sorgu kısmı alınır;
     * böylece Referer başlığı başka bir siteye yönlendirme için kullanılamaz.
     */
    static String oncekiSayfa(HttpServletRequest istek) {
        String referer = istek.getHeader("Referer");
        if (referer == null) {
            return "/panel";
        }
        try {
            URI uri = URI.create(referer);
            String yol = uri.getRawPath();
            if (yol == null || yol.isBlank() || !yol.startsWith("/")) {
                return "/panel";
            }
            String hedef = uri.getRawQuery() == null ? yol : yol + "?" + uri.getRawQuery();
            // Hata veren sayfayı açmaya çalışırken (GET) aynı sayfaya dönmek sonsuz yönlendirme olurdu.
            // Form gönderimi (POST) ise çoğu zaman bulunduğu sayfanın adresine yapılır; ona dokunulmaz.
            String simdiki = istek.getQueryString() == null ? istek.getRequestURI()
                    : istek.getRequestURI() + "?" + istek.getQueryString();
            boolean kendineDonus = "GET".equalsIgnoreCase(istek.getMethod()) && hedef.equals(simdiki);
            return kendineDonus ? "/panel" : hedef;
        } catch (IllegalArgumentException e) {
            return "/panel";
        }
    }
}
