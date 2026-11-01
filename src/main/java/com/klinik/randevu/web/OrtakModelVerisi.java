package com.klinik.randevu.web;

import com.klinik.randevu.model.icerik.SiteAyarlari;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.servis.IcerikServisi;
import com.klinik.randevu.servis.OturumServisi;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Tüm şablonlara oturumdaki kullanıcıyı ({@code aktifKullanici}) ve site ayarlarını ({@code site}) verir. */
@ControllerAdvice
public class OrtakModelVerisi {

    private final OturumServisi oturumServisi;
    private final IcerikServisi icerikServisi;

    public OrtakModelVerisi(OturumServisi oturumServisi, IcerikServisi icerikServisi) {
        this.oturumServisi = oturumServisi;
        this.icerikServisi = icerikServisi;
    }

    /** Bilgi bandı ve alt bilgideki iletişim bilgileri her sayfada gerekir. */
    @ModelAttribute("site")
    public SiteAyarlari site() {
        return icerikServisi.siteAyarlari();
    }

    @ModelAttribute("aktifKullanici")
    public Kullanici aktifKullanici() {
        return oturumServisi.aktifKullaniciBul().orElse(null);
    }
}
