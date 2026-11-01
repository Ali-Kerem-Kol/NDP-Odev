package com.klinik.randevu.web;

import com.klinik.randevu.guvenlik.KotuyeKullanimKorumasi;
import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kullanici.TcKimlikNo;
import com.klinik.randevu.servis.IcerikServisi;
import com.klinik.randevu.servis.KullaniciServisi;
import com.klinik.randevu.servis.KurumServisi;
import com.klinik.randevu.servis.OturumServisi;
import com.klinik.randevu.web.form.HastaKayitFormu;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Herkese açık sayfalar: ana sayfa, giriş, hasta kaydı ve aydınlatma metni.
 */
@Controller
public class AnaSayfaController {

    private final OturumServisi oturumServisi;
    private final KullaniciServisi kullaniciServisi;
    private final KurumServisi kurumServisi;
    private final IcerikServisi icerikServisi;
    private final KotuyeKullanimKorumasi koruma;

    public AnaSayfaController(OturumServisi oturumServisi, KullaniciServisi kullaniciServisi,
                              KurumServisi kurumServisi, IcerikServisi icerikServisi,
                              KotuyeKullanimKorumasi koruma) {
        this.koruma = koruma;
        this.oturumServisi = oturumServisi;
        this.kullaniciServisi = kullaniciServisi;
        this.kurumServisi = kurumServisi;
        this.icerikServisi = icerikServisi;
    }

    /** Herkese açık ana sayfa: öne çıkan içerik, son duyurular, kurumlar. Oturum açmış kullanıcı da görebilir. */
    @GetMapping("/")
    public String anaSayfa(Model model) {
        Duyuru onecikan = icerikServisi.onecikan().orElse(null);
        model.addAttribute("onecikan", onecikan);
        model.addAttribute("sonDuyurular", icerikServisi.sonYayinlananlar(4, onecikan));
        model.addAttribute("bugun", LocalDate.now().getDayOfWeek());
        // Sistem yöneticisi kurumları ana sayfadaki kartlardan yönetir; yayından kaldırılanları da görür
        boolean siteYoneticisi = oturumServisi.aktifKullaniciBul()
                .filter(Kullanici::siteAyarlariniDuzenleyebilirMi).isPresent();
        model.addAttribute("kurumlar", siteYoneticisi ? kurumServisi.tumKurumlar() : kurumServisi.yayindakiKurumlar());
        if (siteYoneticisi) {
            model.addAttribute("kurumYoneticileri", kullaniciServisi.yoneticiler().stream()
                    .collect(Collectors.groupingBy(y -> y.getKurum().getId())));
        }
        return "index";
    }

    /** Her kullanıcı türü kendi paneline yönlendirilir (getPanelYolu çok biçimli). */
    @GetMapping("/panel")
    public String panel() {
        return "redirect:" + oturumServisi.aktifKullanici().getPanelYolu();
    }

    @GetMapping("/giris")
    public String giris(@RequestParam(required = false) String hata, @RequestParam(required = false) String cikis,
                        @RequestParam(required = false) String pasif,
                        @RequestParam(required = false) Long kilitli, Model model) {
        model.addAttribute("hesapPasif", pasif != null);
        model.addAttribute("kilitDakika", kilitli);
        model.addAttribute("girisHatasi", hata != null);
        model.addAttribute("cikisYapildi", cikis != null);
        return "giris";
    }

    @GetMapping("/aydinlatma-metni")
    public String aydinlatmaMetni() {
        return "aydinlatma-metni";
    }

    @GetMapping("/kayit")
    public String kayitFormu(Model model) {
        model.addAttribute("form", new HastaKayitFormu());
        return "kayit";
    }

    @PostMapping("/kayit")
    public String kayitOl(@Valid @ModelAttribute("form") HastaKayitFormu form, BindingResult sonuc,
                          HttpServletRequest istek, RedirectAttributes yonlendirme) {
        String ip = istek.getRemoteAddr();
        var engel = koruma.kayitEngeli(ip);
        if (engel.isPresent()) {
            sonuc.reject("kayit", "Bu cihazdan çok fazla kayıt denemesi yapıldı. "
                    + KotuyeKullanimKorumasi.dakika(engel.get()) + " dakika sonra yeniden deneyin.");
            return "kayit";
        }
        koruma.kayitDenemesi(ip);
        if (!sonuc.hasFieldErrors("tcKimlikNo") && !TcKimlikNo.gecerliMi(form.getTcKimlikNo())) {
            sonuc.rejectValue("tcKimlikNo", "gecersiz", "Bu numara geçerli bir T.C. Kimlik No değil. Lütfen kontrol edin.");
        }
        if (sonuc.hasErrors()) {
            return "kayit";
        }
        try {
            kullaniciServisi.hastaKaydet(form);
        } catch (IsKuraliIhlali hata) {
            // Form içeriği kaybolmasın diye aynı sayfada gösterilir
            sonuc.reject("kayit", hata.getMessage());
            return "kayit";
        }
        yonlendirme.addFlashAttribute("basari", "Hesabınız oluşturuldu. T.C. Kimlik No ve şifrenizle giriş yapabilirsiniz.");
        return "redirect:/giris";
    }
}
