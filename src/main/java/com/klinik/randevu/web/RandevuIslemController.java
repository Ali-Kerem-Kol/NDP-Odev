package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.servis.OturumServisi;
import com.klinik.randevu.servis.RandevuServisi;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tüm roller için ortak randevu işlemleri. Kimin neyi yapabileceğine
 * kullanıcı nesnesinin kendisi karar verir (bkz. Kullanici#randevuyuYonetebilirMi).
 */
@Controller
@RequestMapping("/randevu/{id}")
public class RandevuIslemController {

    private final RandevuServisi randevuServisi;
    private final OturumServisi oturumServisi;

    public RandevuIslemController(RandevuServisi randevuServisi, OturumServisi oturumServisi) {
        this.randevuServisi = randevuServisi;
        this.oturumServisi = oturumServisi;
    }

    /** Randevunun tüm detayı ve durum geçmişi; yalnızca ilgili kişiler görebilir. */
    @GetMapping
    public String detay(@PathVariable Long id, Model model) {
        Kullanici kullanici = oturumServisi.aktifKullanici();
        model.addAttribute("randevu", randevuServisi.gorulebilirRandevu(id, kullanici));
        model.addAttribute("menuAnahtari", switch (kullanici.getRol()) {
            case HASTA -> "randevularim";
            case SISTEM_YONETICISI -> "ana";
            default -> "randevular";
        });
        return "randevu-detay";
    }

    @PostMapping("/onayla")
    public String onayla(@PathVariable Long id, HttpServletRequest istek, RedirectAttributes yonlendirme) {
        randevuServisi.onayla(id, oturumServisi.aktifKullanici());
        yonlendirme.addFlashAttribute("basari", "Randevu onaylandı.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/reddet")
    public String reddet(@PathVariable Long id, @RequestParam(required = false) String neden,
                         HttpServletRequest istek, RedirectAttributes yonlendirme) {
        randevuServisi.reddet(id, oturumServisi.aktifKullanici(), neden);
        yonlendirme.addFlashAttribute("basari", "Randevu reddedildi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/tamamla")
    public String tamamla(@PathVariable Long id, HttpServletRequest istek, RedirectAttributes yonlendirme) {
        randevuServisi.tamamla(id, oturumServisi.aktifKullanici());
        yonlendirme.addFlashAttribute("basari", "Randevu tamamlandı olarak işaretlendi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/gelmedi")
    public String gelmedi(@PathVariable Long id, HttpServletRequest istek, RedirectAttributes yonlendirme) {
        randevuServisi.gelmedi(id, oturumServisi.aktifKullanici());
        yonlendirme.addFlashAttribute("basari", "Hasta gelmedi olarak işaretlendi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/iptal")
    public String iptal(@PathVariable Long id, @RequestParam(required = false) String neden,
                        HttpServletRequest istek, RedirectAttributes yonlendirme) {
        randevuServisi.iptalEt(id, oturumServisi.aktifKullanici(), neden);
        yonlendirme.addFlashAttribute("basari", "Randevu iptal edildi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }
}
