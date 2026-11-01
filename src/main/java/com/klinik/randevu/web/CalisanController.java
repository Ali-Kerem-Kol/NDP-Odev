package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.servis.OturumServisi;
import com.klinik.randevu.servis.PersonelServisi;
import com.klinik.randevu.servis.RandevuServisi;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Doktor ve sağlık personelinin ortak paneli. */
@Controller
@RequestMapping("/calisan")
public class CalisanController {

    private final OturumServisi oturumServisi;
    private final RandevuServisi randevuServisi;
    private final PersonelServisi personelServisi;

    public CalisanController(OturumServisi oturumServisi, RandevuServisi randevuServisi,
                             PersonelServisi personelServisi) {
        this.oturumServisi = oturumServisi;
        this.randevuServisi = randevuServisi;
        this.personelServisi = personelServisi;
    }

    @GetMapping
    public String panel(@RequestParam(required = false) RandevuSekmesi sekme, Model model) {
        Calisan calisan = oturumServisi.aktifKullanici(Calisan.class);
        List<Randevu> tumu = randevuServisi.calisanRandevulari(calisan, null);
        LocalDateTime simdi = LocalDateTime.now();
        List<Randevu> bugunku = tumu.stream()
                .filter(r -> r.getDurum().isAktif() && r.getBaslangic().toLocalDate().equals(LocalDate.now()))
                .toList();
        Randevu siradaki = tumu.stream()
                .filter(r -> r.getDurum().isAktif() && r.getBitis().isAfter(simdi))
                .findFirst().orElse(null);

        long bekleyen = RandevuSekmesi.BEKLEYEN.say(tumu);
        RandevuSekmesi secili = sekme != null ? sekme : (bekleyen > 0 ? RandevuSekmesi.BEKLEYEN : RandevuSekmesi.YAKLASAN);
        model.addAttribute("calisan", calisan);
        model.addAttribute("bugunku", bugunku);
        model.addAttribute("siradaki", siradaki);
        model.addAttribute("sekmeler", RandevuSekmesi.values());
        model.addAttribute("seciliSekme", secili);
        model.addAttribute("sekmeSayilari", sekmeSayilari(tumu));
        model.addAttribute("randevular", secili.suz(tumu));
        return "calisan/panel";
    }

    static Map<RandevuSekmesi, Long> sekmeSayilari(List<Randevu> randevular) {
        Map<RandevuSekmesi, Long> sayilar = new EnumMap<>(RandevuSekmesi.class);
        for (RandevuSekmesi s : RandevuSekmesi.values()) {
            sayilar.put(s, s.say(randevular));
        }
        return sayilar;
    }

    @GetMapping("/uygunluk")
    public String uygunluk(Model model) {
        model.addAttribute("calisan", oturumServisi.aktifKullanici(Calisan.class));
        return "calisan/uygunluk";
    }

    @PostMapping("/uygunluk")
    public String uygunlukEkle(@RequestParam DayOfWeek gun,
                               @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime baslangic,
                               @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime bitis,
                               RedirectAttributes yonlendirme) {
        Calisan calisan = oturumServisi.aktifKullanici(Calisan.class);
        personelServisi.uygunlukEkle(calisan, calisan.getId(), gun, baslangic, bitis);
        yonlendirme.addFlashAttribute("basari", "Çalışma saati eklendi.");
        return "redirect:/calisan/uygunluk";
    }

    @PostMapping("/uygunluk/{dilimId}/sil")
    public String uygunlukSil(@PathVariable Long dilimId, RedirectAttributes yonlendirme) {
        Calisan calisan = oturumServisi.aktifKullanici(Calisan.class);
        personelServisi.uygunlukKaldir(calisan, calisan.getId(), dilimId);
        yonlendirme.addFlashAttribute("basari", "Çalışma saati kaldırıldı.");
        return "redirect:/calisan/uygunluk";
    }
}
