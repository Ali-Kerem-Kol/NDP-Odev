package com.klinik.randevu.web;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.icerik.DuyuruKategorisi;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.servis.IcerikServisi;
import com.klinik.randevu.servis.KurumServisi;
import com.klinik.randevu.servis.OturumServisi;
import com.klinik.randevu.web.form.DuyuruFormu;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Duyuru ve haber yönetimi. Sistem yöneticisi tüm içeriği, kurum yöneticisi yalnızca
 * kendi kurumunun içeriğini görür ve düzenler.
 */
@Controller
@RequestMapping("/icerik")
public class IcerikController {

    private final IcerikServisi icerikServisi;
    private final OturumServisi oturumServisi;
    private final KurumServisi kurumServisi;

    public IcerikController(IcerikServisi icerikServisi, OturumServisi oturumServisi, KurumServisi kurumServisi) {
        this.icerikServisi = icerikServisi;
        this.oturumServisi = oturumServisi;
        this.kurumServisi = kurumServisi;
    }

    /** İçerik ayrı bir listede değil, herkese açık Duyurular sayfasında yönetilir. */
    @GetMapping
    public String liste() {
        return "redirect:/haberler";
    }

    @GetMapping("/yeni")
    public String yeniFormu(Model model) {
        formModeli(model, new DuyuruFormu(), null);
        return "icerik/form";
    }

    @PostMapping("/yeni")
    public String olustur(@Valid @ModelAttribute("form") DuyuruFormu form, BindingResult sonuc, Model model,
                          RedirectAttributes yonlendirme) {
        if (sonuc.hasErrors()) {
            formModeli(model, form, null);
            return "icerik/form";
        }
        Duyuru duyuru;
        try {
            duyuru = icerikServisi.olustur(oturumServisi.aktifKullanici(), form);
        } catch (IsKuraliIhlali hata) {
            sonuc.reject("icerik", hata.getMessage());
            formModeli(model, form, null);
            return "icerik/form";
        }
        yonlendirme.addFlashAttribute("basari", duyuru.isYayinda()
                ? "İçerik yayımlandı ve ana sayfada görünüyor." : "İçerik taslak olarak kaydedildi. Yalnızca siz görebilirsiniz.");
        return "redirect:/haberler/" + duyuru.getId();
    }

    @GetMapping("/{id}/duzenle")
    public String duzenleFormu(@PathVariable Long id, Model model) {
        Duyuru duyuru = icerikServisi.yonetilenDuyuru(oturumServisi.aktifKullanici(), id);
        formModeli(model, DuyuruFormu.duyurudan(duyuru), duyuru);
        return "icerik/form";
    }

    @PostMapping("/{id}/duzenle")
    public String guncelle(@PathVariable Long id, @Valid @ModelAttribute("form") DuyuruFormu form, BindingResult sonuc,
                           Model model, RedirectAttributes yonlendirme) {
        Kullanici kullanici = oturumServisi.aktifKullanici();
        if (sonuc.hasErrors()) {
            formModeli(model, form, icerikServisi.yonetilenDuyuru(kullanici, id));
            return "icerik/form";
        }
        icerikServisi.guncelle(kullanici, id, form);
        yonlendirme.addFlashAttribute("basari", "Değişiklikler kaydedildi.");
        return "redirect:/haberler/" + id;
    }

    @PostMapping("/{id}/yayin")
    public String yayin(@PathVariable Long id, HttpServletRequest istek, RedirectAttributes yonlendirme) {
        boolean yayinda = icerikServisi.yayinDurumunuDegistir(oturumServisi.aktifKullanici(), id);
        yonlendirme.addFlashAttribute("basari", yayinda ? "İçerik yayımlandı." : "İçerik yayından kaldırıldı; artık yalnızca siz görebilirsiniz.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/{id}/one-cikar")
    public String oneCikar(@PathVariable Long id, HttpServletRequest istek, RedirectAttributes yonlendirme) {
        icerikServisi.oneCikar(oturumServisi.aktifKullanici(), id);
        yonlendirme.addFlashAttribute("basari", "Ana sayfadaki öne çıkan içerik güncellendi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/{id}/sil")
    public String sil(@PathVariable Long id, RedirectAttributes yonlendirme) {
        icerikServisi.sil(oturumServisi.aktifKullanici(), id);
        yonlendirme.addFlashAttribute("basari", "İçerik silindi.");
        return "redirect:/haberler";
    }

    private void formModeli(Model model, DuyuruFormu form, Duyuru duzenlenen) {
        model.addAttribute("form", form);
        model.addAttribute("duzenlenen", duzenlenen);
        model.addAttribute("kategoriler", DuyuruKategorisi.values());
        model.addAttribute("kurumlar", kurumServisi.tumKurumlar());
    }
}
