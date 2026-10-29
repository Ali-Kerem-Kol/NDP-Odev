package com.klinik.randevu.servis;

import com.klinik.randevu.hata.KayitBulunamadiHatasi;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.icerik.DuyuruKategorisi;
import com.klinik.randevu.model.icerik.SiteAyarlari;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.repository.DuyuruRepository;
import com.klinik.randevu.repository.KurumRepository;
import com.klinik.randevu.repository.SiteAyarlariRepository;
import com.klinik.randevu.web.form.DuyuruFormu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Ana sayfa içerikleri: duyurular, haberler, sağlık rehberi yazıları ve site ayarları.
 * Yetki kararlarını kullanıcı nesnesi verir ({@code icerigiYonetebilirMi}, {@code icerikKurumunuBelirle}).
 */
@Service
@Transactional
public class IcerikServisi {

    public static final int SAYFA_BOYUTU = 9;

    private final DuyuruRepository duyuruRepository;
    private final SiteAyarlariRepository siteAyarlariRepository;
    private final KurumRepository kurumRepository;

    public IcerikServisi(DuyuruRepository duyuruRepository, SiteAyarlariRepository siteAyarlariRepository,
                         KurumRepository kurumRepository) {
        this.duyuruRepository = duyuruRepository;
        this.siteAyarlariRepository = siteAyarlariRepository;
        this.kurumRepository = kurumRepository;
    }

    // ---- Herkese açık okuma ----

    @Transactional(readOnly = true)
    public Page<Duyuru> yayindakiler(DuyuruKategorisi kategori, int sayfa) {
        PageRequest istek = PageRequest.of(Math.max(sayfa, 0), SAYFA_BOYUTU);
        return kategori == null
                ? duyuruRepository.findByYayindaTrueOrderByYayinZamaniDescIdDesc(istek)
                : duyuruRepository.findByYayindaTrueAndKategoriOrderByYayinZamaniDescIdDesc(kategori, istek);
    }

    @Transactional(readOnly = true)
    public Optional<Duyuru> onecikan() {
        return duyuruRepository.findFirstByYayindaTrueAndOnecikanTrue();
    }

    /** Ana sayfa listesi: öne çıkan hariç en yeni yayındaki içerikler. */
    @Transactional(readOnly = true)
    public List<Duyuru> sonYayinlananlar(int adet, Duyuru haric) {
        return duyuruRepository.findByYayindaTrueOrderByYayinZamaniDescIdDesc(PageRequest.of(0, adet + 1)).stream()
                .filter(d -> !d.equals(haric))
                .limit(adet)
                .toList();
    }

    /** Yayındaki içeriği herkes, taslağı yalnızca yönetebilen kişi görür. */
    @Transactional(readOnly = true)
    public Duyuru goruntulenecekDuyuru(Long id, Kullanici okuyan) {
        Duyuru duyuru = bul(id);
        if (!duyuru.isYayinda() && (okuyan == null || !okuyan.icerigiYonetebilirMi(duyuru))) {
            throw new KayitBulunamadiHatasi("Duyuru", id);
        }
        return duyuru;
    }

    @Transactional(readOnly = true)
    public SiteAyarlari siteAyarlari() {
        return siteAyarlariRepository.findById(SiteAyarlari.TEK_KAYIT).orElseGet(SiteAyarlari::varsayilan);
    }

    // ---- Yönetim ----

    @Transactional(readOnly = true)
    public List<Duyuru> yonetilebilenler(Kullanici kullanici) {
        yazarMi(kullanici);
        return duyuruRepository.findAllByOrderByGuncellemeZamaniDesc().stream()
                .filter(kullanici::icerigiYonetebilirMi)
                .toList();
    }

    public Duyuru olustur(Kullanici yazar, DuyuruFormu form) {
        yazarMi(yazar);
        Kurum kurum = yazar.icerikKurumunuBelirle(kurumBul(form.getKurumId()));
        Duyuru duyuru = new Duyuru(form.getBaslik(), form.getOzet(), form.getIcerik(), form.getKategori(),
                kurum, yazar.getAdSoyad());
        if (form.isYayinla()) {
            duyuru.yayinla();
        }
        return duyuruRepository.save(duyuru);
    }

    public void guncelle(Kullanici yapan, Long id, DuyuruFormu form) {
        Duyuru duyuru = yonetilenDuyuru(yapan, id);
        duyuru.guncelle(form.getBaslik(), form.getOzet(), form.getIcerik(), form.getKategori(), yapan.getAdSoyad());
        if (form.isYayinla()) {
            duyuru.yayinla();
        } else {
            duyuru.yayindanKaldir();
        }
    }

    public boolean yayinDurumunuDegistir(Kullanici yapan, Long id) {
        Duyuru duyuru = yonetilenDuyuru(yapan, id);
        if (duyuru.isYayinda()) {
            duyuru.yayindanKaldir();
        } else {
            duyuru.yayinla();
        }
        return duyuru.isYayinda();
    }

    /** Ana sayfada aynı anda tek bir öne çıkan içerik bulunur; yenisi seçilince eskisi kalkar. */
    public void oneCikar(Kullanici yapan, Long id) {
        Duyuru duyuru = yonetilenDuyuru(yapan, id);
        if (duyuru.isOnecikan()) {
            duyuru.onecikarmayiKaldir();
            return;
        }
        if (!yapan.siteAyarlariniDuzenleyebilirMi()) {
            throw new YetkisizIslemHatasi("Ana sayfada öne çıkan içeriği yalnızca sistem yöneticisi seçebilir.");
        }
        duyuruRepository.tumOnecikarmalariKaldir();
        duyuruRepository.findById(id).orElseThrow().oneCikar();
    }

    public void sil(Kullanici yapan, Long id) {
        duyuruRepository.delete(yonetilenDuyuru(yapan, id));
    }

    public void karsilamaGuncelle(Kullanici yapan, String baslik, String metin) {
        SiteAyarlari ayarlar = duzenlenecekAyarlar(yapan);
        ayarlar.karsilamaGuncelle(baslik, metin, yapan.getAdSoyad());
        siteAyarlariRepository.save(ayarlar);
    }

    public void bilgiBandiGuncelle(Kullanici yapan, String bant) {
        SiteAyarlari ayarlar = duzenlenecekAyarlar(yapan);
        ayarlar.bilgiBandiGuncelle(bant, yapan.getAdSoyad());
        siteAyarlariRepository.save(ayarlar);
    }

    public void iletisimGuncelle(Kullanici yapan, String telefon, String eposta, String adres) {
        SiteAyarlari ayarlar = duzenlenecekAyarlar(yapan);
        ayarlar.iletisimGuncelle(telefon, eposta, adres, yapan.getAdSoyad());
        siteAyarlariRepository.save(ayarlar);
    }

    private SiteAyarlari duzenlenecekAyarlar(Kullanici yapan) {
        if (!yapan.siteAyarlariniDuzenleyebilirMi()) {
            throw new YetkisizIslemHatasi("Site metinlerini yalnızca sistem yöneticisi düzenleyebilir.");
        }
        return siteAyarlariRepository.findById(SiteAyarlari.TEK_KAYIT).orElseGet(SiteAyarlari::varsayilan);
    }

    @Transactional(readOnly = true)
    public Duyuru yonetilenDuyuru(Kullanici yapan, Long id) {
        Duyuru duyuru = bul(id);
        if (!yapan.icerigiYonetebilirMi(duyuru)) {
            throw new YetkisizIslemHatasi("Bu içeriği düzenleme yetkiniz yok.");
        }
        return duyuru;
    }

    private Duyuru bul(Long id) {
        return duyuruRepository.findById(id).orElseThrow(() -> new KayitBulunamadiHatasi("Duyuru", id));
    }

    private Kurum kurumBul(Long kurumId) {
        return kurumId == null ? null : kurumRepository.findById(kurumId)
                .orElseThrow(() -> new KayitBulunamadiHatasi("Kurum", kurumId));
    }

    private static void yazarMi(Kullanici kullanici) {
        if (!kullanici.icerikYazabilirMi()) {
            throw new YetkisizIslemHatasi("İçerik yönetimi yetkiniz yok.");
        }
    }
}
