package com.klinik.randevu.web;

import com.klinik.randevu.hata.KayitBulunamadiHatasi;
import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.icerik.DuyuruKategorisi;
import com.klinik.randevu.servis.IcerikServisi;
import com.klinik.randevu.servis.OturumServisi;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Herkese açık duyuru, haber ve sağlık rehberi sayfaları. */
@Controller
@RequestMapping("/haberler")
public class HaberController {

    private final IcerikServisi icerikServisi;
    private final OturumServisi oturumServisi;

    public HaberController(IcerikServisi icerikServisi, OturumServisi oturumServisi) {
        this.icerikServisi = icerikServisi;
        this.oturumServisi = oturumServisi;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) DuyuruKategorisi kategori,
                        @RequestParam(defaultValue = "0") int sayfa, Model model) {
        Page<Duyuru> sonuc = icerikServisi.yayindakiler(kategori, sayfa);
        model.addAttribute("duyurular", sonuc.getContent());
        model.addAttribute("sayfaBilgisi", sonuc);
        model.addAttribute("kategoriler", DuyuruKategorisi.values());
        model.addAttribute("seciliKategori", kategori);
        // İçerik yazabilenler taslaklarını aynı sayfada, listenin üstünde görür
        oturumServisi.aktifKullaniciBul()
                .filter(k -> k.icerikYazabilirMi())
                .ifPresent(k -> model.addAttribute("taslaklar", icerikServisi.yonetilebilenler(k).stream()
                        .filter(d -> !d.isYayinda()).toList()));
        return "haberler/liste";
    }

    @GetMapping("/{id}")
    public String detay(@PathVariable Long id, Model model) {
        Duyuru duyuru;
        try {
            duyuru = icerikServisi.goruntulenecekDuyuru(id, oturumServisi.aktifKullaniciBul().orElse(null));
        } catch (KayitBulunamadiHatasi yok) {
            // Herkese açık sayfa: taslak veya silinmiş içerik için standart 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("duyuru", duyuru);
        model.addAttribute("digerleri", icerikServisi.sonYayinlananlar(3, duyuru));
        return "haberler/detay";
    }
}
