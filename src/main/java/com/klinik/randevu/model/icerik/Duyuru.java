package com.klinik.randevu.model.icerik;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.TemelVarlik;
import com.klinik.randevu.model.kurum.Kurum;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Ana sayfada ve haberler bölümünde yayımlanan duyuru, haber veya sağlık rehberi yazısı.
 * Kurumu boş olan içerik tüm sistemin genel duyurusudur; kurum yöneticileri yalnızca
 * kendi kurumlarına ait içeriği yönetir.
 */
@Entity
@Table(name = "duyurular")
public class Duyuru extends TemelVarlik {

    public static final int AZAMI_BASLIK = 150;
    public static final int AZAMI_OZET = 300;
    public static final int AZAMI_ICERIK = 10_000;

    @Column(nullable = false, length = AZAMI_BASLIK)
    private String baslik;

    @Column(nullable = false, length = AZAMI_OZET)
    private String ozet;

    @Column(nullable = false, length = AZAMI_ICERIK)
    private String icerik;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DuyuruKategorisi kategori;

    @ManyToOne
    private Kurum kurum;

    private boolean yayinda;

    /** Ana sayfada en üstte büyük gösterilen içerik; aynı anda yalnızca bir tane olur. */
    private boolean onecikan;

    private LocalDateTime yayinZamani;

    @Column(nullable = false)
    private LocalDateTime olusturmaZamani;

    @Column(nullable = false)
    private LocalDateTime guncellemeZamani;

    @Column(nullable = false, length = 120)
    private String yazar;

    protected Duyuru() {
    }

    public Duyuru(String baslik, String ozet, String icerik, DuyuruKategorisi kategori, Kurum kurum, String yazar) {
        this.kurum = kurum;
        this.yazar = yazar;
        this.olusturmaZamani = LocalDateTime.now();
        guncelle(baslik, ozet, icerik, kategori, yazar);
    }

    public void guncelle(String yeniBaslik, String yeniOzet, String yeniIcerik, DuyuruKategorisi yeniKategori, String duzenleyen) {
        this.baslik = zorunlu(yeniBaslik, "Başlık", AZAMI_BASLIK);
        this.ozet = zorunlu(yeniOzet, "Özet", AZAMI_OZET);
        this.icerik = zorunlu(yeniIcerik, "İçerik", AZAMI_ICERIK);
        this.kategori = yeniKategori == null ? DuyuruKategorisi.DUYURU : yeniKategori;
        this.yazar = duzenleyen;
        this.guncellemeZamani = LocalDateTime.now();
    }

    public void yayinla() {
        if (!yayinda) {
            yayinda = true;
            yayinZamani = LocalDateTime.now();
        }
    }

    public void yayindanKaldir() {
        yayinda = false;
        onecikan = false;
    }

    public void oneCikar() {
        if (!yayinda) {
            throw new IsKuraliIhlali("Yalnızca yayındaki içerik öne çıkarılabilir.");
        }
        onecikan = true;
    }

    public void onecikarmayiKaldir() {
        onecikan = false;
    }

    /** İçerik düz metin olarak saklanır; boş satırlar paragraf ayırır (HTML enjeksiyonu olmaz). */
    public List<String> getParagraflar() {
        return Arrays.stream(icerik.split("\\R\\s*\\R"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();
    }

    /** Dakika cinsinden tahmini okuma süresi (dakikada ~200 kelime). */
    public int getOkumaSuresi() {
        int kelime = icerik.trim().split("\\s+").length;
        return Math.max(1, Math.round(kelime / 200f));
    }

    public boolean isGenel() {
        return kurum == null;
    }

    public String getKaynak() {
        return kurum == null ? "Medora" : kurum.getAd();
    }

    private static String zorunlu(String deger, String alan, int azami) {
        if (deger == null || deger.isBlank()) {
            throw new IsKuraliIhlali(alan + " boş olamaz.");
        }
        String temiz = deger.trim();
        if (temiz.length() > azami) {
            throw new IsKuraliIhlali(alan + " en fazla " + azami + " karakter olabilir.");
        }
        return temiz;
    }

    public String getBaslik() {
        return baslik;
    }

    public String getOzet() {
        return ozet;
    }

    public String getIcerik() {
        return icerik;
    }

    public DuyuruKategorisi getKategori() {
        return kategori;
    }

    public Kurum getKurum() {
        return kurum;
    }

    public boolean isYayinda() {
        return yayinda;
    }

    public boolean isOnecikan() {
        return onecikan;
    }

    public LocalDateTime getYayinZamani() {
        return yayinZamani;
    }

    public LocalDateTime getOlusturmaZamani() {
        return olusturmaZamani;
    }

    public LocalDateTime getGuncellemeZamani() {
        return guncellemeZamani;
    }

    public String getYazar() {
        return yazar;
    }
}
