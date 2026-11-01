package com.klinik.randevu.web;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.servis.GunlukMusaitlik;
import com.klinik.randevu.servis.KurumServisi;
import com.klinik.randevu.servis.OturumServisi;
import com.klinik.randevu.servis.PersonelServisi;
import com.klinik.randevu.servis.RandevuServisi;
import com.klinik.randevu.servis.kural.RandevuTalebi;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Hastanın sayfaları: randevularım ve adım adım randevu alma (kurum, şube, hizmet, doktor, saat).
 */
@Controller
@RequestMapping("/hasta")
public class HastaController {

    /** Hastaya kaç günlük boş saat gösterileceği. */
    static final int TAKVIM_GUN_SAYISI = 14;

    private final OturumServisi oturumServisi;
    private final RandevuServisi randevuServisi;
    private final KurumServisi kurumServisi;
    private final PersonelServisi personelServisi;

    public HastaController(OturumServisi oturumServisi, RandevuServisi randevuServisi,
                           KurumServisi kurumServisi, PersonelServisi personelServisi) {
        this.oturumServisi = oturumServisi;
        this.randevuServisi = randevuServisi;
        this.kurumServisi = kurumServisi;
        this.personelServisi = personelServisi;
    }

    /** Bir doktorun önümüzdeki günlerdeki boş saatleri. */
    public record CalisanTakvimi(Calisan calisan, List<GunlukMusaitlik> gunler) {
        public boolean isBos() {
            return gunler.isEmpty();
        }
    }

    @GetMapping
    public String panel(Model model) {
        Hasta hasta = oturumServisi.aktifKullanici(Hasta.class);
        LocalDateTime simdi = LocalDateTime.now();
        List<Randevu> tumu = randevuServisi.hastaRandevulari(hasta);
        List<Randevu> yaklasanlar = tumu.stream()
                .filter(r -> r.getDurum().isAktif() && r.getBaslangic().isAfter(simdi))
                .sorted(Comparator.comparing(Randevu::getBaslangic))
                .toList();
        List<Randevu> gecmis = tumu.stream().filter(r -> !yaklasanlar.contains(r)).toList();
        model.addAttribute("siradaki", yaklasanlar.isEmpty() ? null : yaklasanlar.get(0));
        model.addAttribute("digerYaklasanlar", yaklasanlar.stream().skip(1).toList());
        model.addAttribute("gecmis", gecmis);
        return "hasta/panel";
    }

    /**
     * Randevu alma: kurum → şube → hizmet seçilir; ardından o hizmeti veren
     * tüm doktorların önümüzdeki iki haftalık boş saatleri kutucuk olarak listelenir.
     * Hasta isterse listeyi tek bir doktora daraltır (çalışan seçimi).
     */
    @GetMapping("/randevu-al")
    public String randevuAl(@RequestParam(required = false) Long kurumId,
                            @RequestParam(required = false) Long subeId,
                            @RequestParam(required = false) Long hizmetId,
                            @RequestParam(required = false) Long calisanId,
                            Model model) {
        Hasta hasta = oturumServisi.aktifKullanici(Hasta.class);
        List<Kurum> kurumlar = kurumServisi.yayindakiKurumlar();
        model.addAttribute("kurumlar", kurumlar);

        Kurum kurum = kurumlar.size() == 1 ? kurumlar.get(0) : sec(kurumlar, kurumId, Kurum::getId).orElse(null);
        model.addAttribute("kurum", kurum);
        if (kurum == null) {
            return "hasta/randevu-al";
        }
        List<Sube> subeler = kurumServisi.subeler(kurum.getId());
        model.addAttribute("subeler", subeler);
        Sube sube = subeler.size() == 1 ? subeler.get(0) : sec(subeler, subeId, Sube::getId).orElse(null);
        model.addAttribute("sube", sube);
        if (sube == null) {
            return "hasta/randevu-al";
        }
        List<Hizmet> hizmetler = kurumServisi.aktifHizmetler(kurum.getId());
        model.addAttribute("hizmetler", hizmetler);
        Hizmet hizmet = sec(hizmetler, hizmetId, Hizmet::getId).orElse(null);
        model.addAttribute("hizmet", hizmet);
        if (hizmet == null) {
            return "hasta/randevu-al";
        }

        LocalDate bugun = LocalDate.now();
        List<Calisan> doktorlar = personelServisi.hizmetVerenler(sube.getId(), hizmet.getId());
        Calisan doktor = sec(doktorlar, calisanId, Calisan::getId).orElse(null);
        model.addAttribute("doktorlar", doktorlar);
        model.addAttribute("doktor", doktor);
        List<CalisanTakvimi> takvimler = (doktor == null ? doktorlar : List.of(doktor)).stream()
                .map(c -> new CalisanTakvimi(c,
                        randevuServisi.musaitlikTakvimi(hasta, c, hizmet, bugun, TAKVIM_GUN_SAYISI)))
                .toList();
        model.addAttribute("takvimler", takvimler);
        model.addAttribute("takvimGunSayisi", TAKVIM_GUN_SAYISI);
        return "hasta/randevu-al";
    }

    /** Seçilen saatin özeti; hasta not ekleyip onaylar. Saat bu arada dolduysa geri döner. */
    @GetMapping("/randevu-al/onay")
    public String onayEkrani(@RequestParam Long calisanId, @RequestParam Long hizmetId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime zaman,
                             Model model) {
        Hasta hasta = oturumServisi.aktifKullanici(Hasta.class);
        RandevuTalebi talep = randevuServisi.talebiDogrula(hasta, calisanId, hizmetId, zaman);
        model.addAttribute("talep", talep);
        return "hasta/randevu-onay";
    }

    @PostMapping("/randevu-al")
    public String randevuOlustur(@RequestParam Long calisanId,
                                 @RequestParam Long hizmetId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime zaman,
                                 @RequestParam(required = false) String not,
                                 RedirectAttributes yonlendirme) {
        Hasta hasta = oturumServisi.aktifKullanici(Hasta.class);
        randevuServisi.randevuOlustur(hasta, calisanId, hizmetId, zaman, not);
        yonlendirme.addFlashAttribute("basari", "Randevu talebiniz alındı. Onaylandığında burada görünecek.");
        return "redirect:/hasta";
    }

    private static <T> Optional<T> sec(List<T> liste, Long id, Function<T, Long> idAl) {
        if (id == null) {
            return Optional.empty();
        }
        return liste.stream().filter(e -> id.equals(idAl.apply(e))).findFirst();
    }
}
