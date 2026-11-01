package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Yonetici;
import com.klinik.randevu.model.kurum.HizmetTuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.KurumTuru;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;
import com.klinik.randevu.servis.*;
import com.klinik.randevu.web.form.CalisanFormu;
import com.klinik.randevu.web.form.CalisanGuncellemeFormu;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Hastane / klinik yöneticisinin paneli: şube, bölüm, hizmet, personel ve randevu yönetimi. */
@Controller
@RequestMapping("/yonetici")
public class YoneticiController {

    private final OturumServisi oturumServisi;
    private final KurumServisi kurumServisi;
    private final PersonelServisi personelServisi;
    private final RandevuServisi randevuServisi;

    public YoneticiController(OturumServisi oturumServisi, KurumServisi kurumServisi,
                              PersonelServisi personelServisi, RandevuServisi randevuServisi) {
        this.oturumServisi = oturumServisi;
        this.kurumServisi = kurumServisi;
        this.personelServisi = personelServisi;
        this.randevuServisi = randevuServisi;
    }

    private Yonetici yonetici() {
        return oturumServisi.aktifKullanici(Yonetici.class);
    }

    @GetMapping
    public String panel(Model model) {
        Kurum kurum = yonetici().getKurum();
        model.addAttribute("kurum", kurum);
        model.addAttribute("subeSayisi", kurumServisi.subeler(kurum.getId()).size());
        model.addAttribute("hizmetSayisi", kurumServisi.hizmetler(kurum.getId()).size());
        model.addAttribute("calisanSayisi", personelServisi.kurumCalisanlari(kurum.getId()).size());
        model.addAttribute("bekleyenSayisi", randevuServisi.bekleyenRandevuSayisi(kurum.getId()));
        model.addAttribute("randevular", RandevuSekmesi.BEKLEYEN.suz(
                randevuServisi.kurumRandevulari(kurum.getId(), RandevuDurumu.BEKLEMEDE)));
        model.addAttribute("subeler", kurumServisi.subeler(kurum.getId()));
        return "yonetici/panel";
    }

    /** Kurum yöneticisi kendi kurumunun adını, türünü ve tanıtım metnini düzenler. */
    @PostMapping("/kurum")
    public String kurumDuzenle(@RequestParam String ad, @RequestParam KurumTuru tur,
                               @RequestParam(required = false) String aciklama, RedirectAttributes yonlendirme) {
        Yonetici yonetici = yonetici();
        kurumServisi.kurumGuncelle(yonetici, yonetici.getKurum().getId(), ad, tur, aciklama);
        yonlendirme.addFlashAttribute("basari", "Kurum bilgileri güncellendi.");
        return "redirect:/yonetici";
    }

    // ---- Şubeler ----

    @GetMapping("/subeler")
    public String subeler(Model model) {
        Kurum kurum = yonetici().getKurum();
        model.addAttribute("kurum", kurum);
        model.addAttribute("subeler", kurumServisi.subeler(kurum.getId()));
        return "yonetici/subeler";
    }

    @PostMapping("/subeler")
    public String subeEkle(@RequestParam String ad, @RequestParam(required = false) String adres,
                           @RequestParam(required = false) String telefon, RedirectAttributes yonlendirme) {
        Yonetici yonetici = yonetici();
        kurumServisi.subeEkle(yonetici, yonetici.getKurum().getId(), ad, adres, telefon);
        yonlendirme.addFlashAttribute("basari", "Şube eklendi.");
        return "redirect:/yonetici/subeler";
    }

    @GetMapping("/subeler/{subeId}")
    public String subeDetay(@PathVariable Long subeId, Model model) {
        model.addAttribute("sube", kurumServisi.yonetilenSubeBul(yonetici(), subeId));
        return "yonetici/sube-detay";
    }

    @PostMapping("/subeler/{subeId}/calisma-saati")
    public String calismaSaatiAyarla(@PathVariable Long subeId, @RequestParam DayOfWeek gun,
                                     @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime acilis,
                                     @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime kapanis,
                                     RedirectAttributes yonlendirme) {
        kurumServisi.calismaSaatiAyarla(yonetici(), subeId, gun, acilis, kapanis);
        yonlendirme.addFlashAttribute("basari", "Çalışma saati kaydedildi.");
        return "redirect:/yonetici/subeler/" + subeId;
    }

    @PostMapping("/subeler/{subeId}/calisma-saati/{gun}/sil")
    public String calismaSaatiKaldir(@PathVariable Long subeId, @PathVariable DayOfWeek gun,
                                     RedirectAttributes yonlendirme) {
        kurumServisi.calismaSaatiKaldir(yonetici(), subeId, gun);
        yonlendirme.addFlashAttribute("basari", "Gün kapalı olarak ayarlandı.");
        return "redirect:/yonetici/subeler/" + subeId;
    }

    @PostMapping("/subeler/{subeId}/duzenle")
    public String subeDuzenle(@PathVariable Long subeId, @RequestParam String ad,
                              @RequestParam(required = false) String adres, @RequestParam(required = false) String telefon,
                              RedirectAttributes yonlendirme) {
        kurumServisi.subeGuncelle(yonetici(), subeId, ad, adres, telefon);
        yonlendirme.addFlashAttribute("basari", "Şube bilgileri güncellendi.");
        return "redirect:/yonetici/subeler/" + subeId;
    }

    @PostMapping("/subeler/{subeId}/sil")
    public String subeSil(@PathVariable Long subeId, RedirectAttributes yonlendirme) {
        String ad = kurumServisi.subeSil(yonetici(), subeId);
        yonlendirme.addFlashAttribute("basari", "'" + ad + "' şubesi kaldırıldı.");
        return "redirect:/yonetici/subeler";
    }

    @PostMapping("/bolumler/{bolumId}/duzenle")
    public String bolumDuzenle(@PathVariable Long bolumId, @RequestParam String ad,
                               @RequestParam(required = false) String aciklama, RedirectAttributes yonlendirme) {
        var sube = kurumServisi.bolumGuncelle(yonetici(), bolumId, ad, aciklama);
        yonlendirme.addFlashAttribute("basari", "Bölüm güncellendi.");
        return "redirect:/yonetici/subeler/" + sube.getId();
    }

    @PostMapping("/bolumler/{bolumId}/sil")
    public String bolumSil(@PathVariable Long bolumId, RedirectAttributes yonlendirme) {
        var sube = kurumServisi.bolumSil(yonetici(), bolumId);
        yonlendirme.addFlashAttribute("basari", "Bölüm kaldırıldı.");
        return "redirect:/yonetici/subeler/" + sube.getId();
    }

    @PostMapping("/subeler/{subeId}/bolum")
    public String bolumEkle(@PathVariable Long subeId, @RequestParam String ad,
                            @RequestParam(required = false) String aciklama, RedirectAttributes yonlendirme) {
        kurumServisi.bolumEkle(yonetici(), subeId, ad, aciklama);
        yonlendirme.addFlashAttribute("basari", "Bölüm eklendi.");
        return "redirect:/yonetici/subeler/" + subeId;
    }

    // ---- Hizmetler ----

    @GetMapping("/hizmetler")
    public String hizmetler(Model model) {
        Kurum kurum = yonetici().getKurum();
        model.addAttribute("kurum", kurum);
        model.addAttribute("hizmetler", kurumServisi.hizmetler(kurum.getId()));
        model.addAttribute("hizmetTurleri", HizmetTuru.values());
        return "yonetici/hizmetler";
    }

    @PostMapping("/hizmetler")
    public String hizmetEkle(@RequestParam String ad, @RequestParam HizmetTuru tur, @RequestParam int sureDakika,
                             @RequestParam BigDecimal ucret, @RequestParam(required = false) String aciklama,
                             RedirectAttributes yonlendirme) {
        Yonetici yonetici = yonetici();
        kurumServisi.hizmetEkle(yonetici, yonetici.getKurum().getId(), ad, tur, sureDakika, ucret, aciklama);
        yonlendirme.addFlashAttribute("basari", "Hizmet eklendi.");
        return "redirect:/yonetici/hizmetler";
    }

    @PostMapping("/hizmetler/{hizmetId}/guncelle")
    public String hizmetGuncelle(@PathVariable Long hizmetId, @RequestParam(required = false) String ad,
                                 @RequestParam(required = false) HizmetTuru tur, @RequestParam int sureDakika,
                                 @RequestParam BigDecimal ucret, @RequestParam(required = false) String aciklama,
                                 RedirectAttributes yonlendirme) {
        if (ad == null) {
            kurumServisi.hizmetGuncelle(yonetici(), hizmetId, sureDakika, ucret);
        } else {
            kurumServisi.hizmetGuncelle(yonetici(), hizmetId, ad, tur, sureDakika, ucret, aciklama);
        }
        yonlendirme.addFlashAttribute("basari", "Hizmet güncellendi. Mevcut randevuların ücreti değişmez.");
        return "redirect:/yonetici/hizmetler";
    }

    @PostMapping("/hizmetler/{hizmetId}/aktiflik")
    public String hizmetAktiflik(@PathVariable Long hizmetId, RedirectAttributes yonlendirme) {
        kurumServisi.hizmetAktiflikDegistir(yonetici(), hizmetId);
        yonlendirme.addFlashAttribute("basari", "Hizmet durumu değiştirildi.");
        return "redirect:/yonetici/hizmetler";
    }

    // ---- Çalışanlar ----

    @GetMapping("/calisanlar")
    public String calisanlar(Model model) {
        Kurum kurum = yonetici().getKurum();
        model.addAttribute("kurum", kurum);
        model.addAttribute("calisanlar", personelServisi.kurumCalisanlari(kurum.getId()));
        model.addAttribute("subeler", kurumServisi.subeler(kurum.getId()));
        model.addAttribute("bolumler", kurumServisi.bolumler(kurum.getId()));
        model.addAttribute("tipler", CalisanTipi.values());
        model.addAttribute("form", new CalisanFormu());
        return "yonetici/calisanlar";
    }

    @PostMapping("/calisanlar")
    public String calisanEkle(@ModelAttribute CalisanFormu form, RedirectAttributes yonlendirme) {
        Calisan calisan = personelServisi.calisanEkle(yonetici(), form);
        yonlendirme.addFlashAttribute("basari",
                calisan.getGorunenAd() + " kaydedildi. Şimdi hizmetlerini ve uygunluk saatlerini tanımlayın.");
        return "redirect:/yonetici/calisanlar/" + calisan.getId();
    }

    @GetMapping("/calisanlar/{calisanId}")
    public String calisanDetay(@PathVariable Long calisanId, Model model) {
        Calisan calisan = personelServisi.yonetilenCalisanBul(yonetici(), calisanId);
        model.addAttribute("calisan", calisan);
        model.addAttribute("hizmetler", kurumServisi.hizmetler(calisan.getKurum().getId()));
        model.addAttribute("subeler", kurumServisi.subeler(calisan.getKurum().getId()));
        model.addAttribute("bolumler", kurumServisi.bolumler(calisan.getKurum().getId()));
        model.addAttribute("bilgiFormu", CalisanGuncellemeFormu.calisandan(calisan));
        model.addAttribute("ileriRandevuSayisi", personelServisi.ileriTarihliAktifRandevuSayisi(calisan));
        return "yonetici/calisan-detay";
    }

    @PostMapping("/calisanlar/{calisanId}/bilgiler")
    public String calisanGuncelle(@PathVariable Long calisanId, @ModelAttribute CalisanGuncellemeFormu form,
                                  RedirectAttributes yonlendirme) {
        personelServisi.calisanGuncelle(yonetici(), calisanId, form);
        yonlendirme.addFlashAttribute("basari", "Personel bilgileri kaydedildi.");
        return "redirect:/yonetici/calisanlar/" + calisanId;
    }

    @PostMapping("/calisanlar/{calisanId}/pasiflestir")
    public String pasiflestir(@PathVariable Long calisanId, RedirectAttributes yonlendirme) {
        personelServisi.pasiflestir(yonetici(), calisanId);
        yonlendirme.addFlashAttribute("basari",
                "Personel pasifleştirildi. Artık giriş yapamaz ve hastalar ondan randevu alamaz.");
        return "redirect:/yonetici/calisanlar/" + calisanId;
    }

    @PostMapping("/calisanlar/{calisanId}/aktiflestir")
    public String aktiflestir(@PathVariable Long calisanId, RedirectAttributes yonlendirme) {
        personelServisi.aktiflestir(yonetici(), calisanId);
        yonlendirme.addFlashAttribute("basari", "Personel yeniden aktif.");
        return "redirect:/yonetici/calisanlar/" + calisanId;
    }

    @PostMapping("/calisanlar/{calisanId}/hizmetler")
    public String hizmetleriAta(@PathVariable Long calisanId,
                                @RequestParam(name = "hizmetIdleri", required = false) List<Long> hizmetIdleri,
                                RedirectAttributes yonlendirme) {
        personelServisi.hizmetleriAta(yonetici(), calisanId, hizmetIdleri);
        yonlendirme.addFlashAttribute("basari", "Çalışanın hizmetleri güncellendi.");
        return "redirect:/yonetici/calisanlar/" + calisanId;
    }

    @PostMapping("/calisanlar/{calisanId}/uygunluk")
    public String uygunlukEkle(@PathVariable Long calisanId, @RequestParam DayOfWeek gun,
                               @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime baslangic,
                               @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime bitis,
                               RedirectAttributes yonlendirme) {
        personelServisi.uygunlukEkle(yonetici(), calisanId, gun, baslangic, bitis);
        yonlendirme.addFlashAttribute("basari", "Uygunluk dilimi eklendi.");
        return "redirect:/yonetici/calisanlar/" + calisanId;
    }

    @PostMapping("/calisanlar/{calisanId}/uygunluk/{dilimId}/sil")
    public String uygunlukSil(@PathVariable Long calisanId, @PathVariable Long dilimId,
                              RedirectAttributes yonlendirme) {
        personelServisi.uygunlukKaldir(yonetici(), calisanId, dilimId);
        yonlendirme.addFlashAttribute("basari", "Uygunluk dilimi kaldırıldı.");
        return "redirect:/yonetici/calisanlar/" + calisanId;
    }

    // ---- Randevular ----

    @GetMapping("/randevular")
    public String randevular(@RequestParam(required = false) RandevuSekmesi sekme, Model model) {
        Kurum kurum = yonetici().getKurum();
        List<Randevu> tumu = randevuServisi.kurumRandevulari(kurum.getId(), null);
        RandevuSekmesi secili = sekme == null ? RandevuSekmesi.BEKLEYEN : sekme;
        model.addAttribute("kurum", kurum);
        model.addAttribute("randevular", secili.suz(tumu));
        model.addAttribute("sekmeler", RandevuSekmesi.values());
        model.addAttribute("seciliSekme", secili);
        model.addAttribute("sekmeSayilari", CalisanController.sekmeSayilari(tumu));
        return "yonetici/randevular";
    }
}
