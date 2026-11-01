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
            return uri.getRawQuery() == null ? yol : yol + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/panel";
        }
    }
}
