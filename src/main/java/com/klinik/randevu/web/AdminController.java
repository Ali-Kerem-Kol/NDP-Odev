package com.klinik.randevu.web;

import com.klinik.randevu.model.kurum.KurumTuru;
import com.klinik.randevu.servis.IcerikServisi;
import com.klinik.randevu.servis.KullaniciServisi;
import com.klinik.randevu.servis.KurumServisi;
import com.klinik.randevu.servis.OturumServisi;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Sistem yöneticisinin işlemleri. Ayrı bir panel sayfası yoktur: formlar ana sayfada,
 * yalnızca sistem yöneticisine görünen düzenleme düğmeleriyle açılır.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OturumServisi oturumServisi;
    private final KurumServisi kurumServisi;
    private final KullaniciServisi kullaniciServisi;
    private final IcerikServisi icerikServisi;

    public AdminController(OturumServisi oturumServisi, KurumServisi kurumServisi,
                           KullaniciServisi kullaniciServisi, IcerikServisi icerikServisi) {
        this.oturumServisi = oturumServisi;
        this.kurumServisi = kurumServisi;
        this.kullaniciServisi = kullaniciServisi;
        this.icerikServisi = icerikServisi;
    }

    /** Sistem yöneticisi ayrı bir panelde değil, sitenin kendisinde çalışır. */
    @GetMapping
    public String panel() {
        return "redirect:/#kurumlar";
    }

    @PostMapping("/kurum")
    public String kurumEkle(@RequestParam String ad, @RequestParam KurumTuru tur,
                            @RequestParam(required = false) String aciklama,
                            @RequestParam String ilkSubeAdi, RedirectAttributes yonlendirme) {
        var yapan = oturumServisi.aktifKullanici();
        var kurum = kurumServisi.kurumOlustur(yapan, ad, tur, aciklama, ilkSubeAdi);
        yonlendirme.addFlashAttribute("basari", "'" + kurum.getAd() + "' kurumu oluşturuldu.");
        return "redirect:/#kurumlar";
    }

    @PostMapping("/kurum/{kurumId}/duzenle")
    public String kurumDuzenle(@PathVariable Long kurumId, @RequestParam String ad, @RequestParam KurumTuru tur,
                               @RequestParam(required = false) String aciklama, RedirectAttributes yonlendirme) {
        kurumServisi.kurumGuncelle(oturumServisi.aktifKullanici(), kurumId, ad, tur, aciklama);
        yonlendirme.addFlashAttribute("basari", "Kurum bilgileri güncellendi.");
        return "redirect:/#kurumlar";
    }

    @PostMapping("/kurum/{kurumId}/yayin")
    public String kurumYayin(@PathVariable Long kurumId, RedirectAttributes yonlendirme) {
        var kurum = kurumServisi.kurumYayinDurumuDegistir(oturumServisi.aktifKullanici(), kurumId);
        yonlendirme.addFlashAttribute("basari", kurum.isAktif()
                ? "'" + kurum.getAd() + "' yeniden yayında; hastalar randevu alabilir."
                : "'" + kurum.getAd() + "' yayından kaldırıldı; yeni randevu alınamaz, mevcut randevular sürüyor.");
        return "redirect:/#kurumlar";
    }

    @PostMapping("/kurum/{kurumId}/sil")
    public String kurumSil(@PathVariable Long kurumId, RedirectAttributes yonlendirme) {
        String ad = kurumServisi.kurumSil(oturumServisi.aktifKullanici(), kurumId);
        yonlendirme.addFlashAttribute("basari", "'" + ad + "' kurumu silindi.");
        return "redirect:/#kurumlar";
    }

    @PostMapping("/yonetici/{yoneticiId}/hesap")
    public String yoneticiHesabi(@PathVariable Long yoneticiId, RedirectAttributes yonlendirme) {
        var yonetici = kullaniciServisi.yoneticiHesabiDurumDegistir(oturumServisi.aktifKullanici(), yoneticiId);
        yonlendirme.addFlashAttribute("basari", yonetici.getAdSoyad() + (yonetici.isAktif()
                ? " artık giriş yapabilir." : " hesabı kapatıldı; giriş yapamaz."));
        return "redirect:/#kurumlar";
    }

    @PostMapping("/yonetici")
    public String yoneticiEkle(@RequestParam Long kurumId, @RequestParam String adSoyad,
                               @RequestParam String eposta, @RequestParam String sifre,
                               RedirectAttributes yonlendirme) {
        kullaniciServisi.yoneticiOlustur(oturumServisi.aktifKullanici(), kurumId, eposta, sifre, adSoyad);
        yonlendirme.addFlashAttribute("basari", adSoyad + " kurum yöneticisi olarak eklendi.");
        return "redirect:/#kurumlar";
    }

    @PostMapping("/site/karsilama")
    public String karsilama(@RequestParam String kahramanBaslik, @RequestParam String kahramanMetni,
                            RedirectAttributes yonlendirme) {
        icerikServisi.karsilamaGuncelle(oturumServisi.aktifKullanici(), kahramanBaslik, kahramanMetni);
        yonlendirme.addFlashAttribute("basari", "Karşılama metni güncellendi.");
        return "redirect:/";
    }

    @PostMapping("/site/bilgi-bandi")
    public String bilgiBandi(@RequestParam(required = false) String bilgiBandi, HttpServletRequest istek,
                             RedirectAttributes yonlendirme) {
        icerikServisi.bilgiBandiGuncelle(oturumServisi.aktifKullanici(), bilgiBandi);
        yonlendirme.addFlashAttribute("basari", bilgiBandi == null || bilgiBandi.isBlank()
                ? "Bilgi bandı kaldırıldı." : "Bilgi bandı güncellendi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    @PostMapping("/site/iletisim")
    public String iletisim(@RequestParam(required = false) String telefon, @RequestParam(required = false) String eposta,
                           @RequestParam(required = false) String adres, HttpServletRequest istek,
                           RedirectAttributes yonlendirme) {
        icerikServisi.iletisimGuncelle(oturumServisi.aktifKullanici(), telefon, eposta, adres);
        yonlendirme.addFlashAttribute("basari", "İletişim bilgileri güncellendi.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }
}
