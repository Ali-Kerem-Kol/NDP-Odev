package com.klinik.randevu.model.icerik;

import com.klinik.randevu.hata.IsKuraliIhlali;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Ana sayfanın sistem yöneticisi tarafından düzenlenen metinleri ve iletişim bilgileri (tek kayıt). */
@Entity
@Table(name = "site_ayarlari")
public class SiteAyarlari {

    public static final long TEK_KAYIT = 1L;

    @Id
    private Long id = TEK_KAYIT;

    @Column(nullable = false, length = 120)
    private String kahramanBaslik;

    @Column(nullable = false, length = 400)
    private String kahramanMetni;

    /** Tüm sayfaların üstünde görünen kısa bilgi bandı; boşsa gösterilmez. */
    @Column(length = 250)
    private String bilgiBandi;

    @Column(length = 30)
    private String telefon;

    @Column(length = 120)
    private String eposta;

    @Column(length = 300)
    private String adres;

    private LocalDateTime guncellemeZamani;

    @Column(length = 120)
    private String guncelleyen;

    protected SiteAyarlari() {
    }

    public static SiteAyarlari varsayilan() {
        SiteAyarlari ayarlar = new SiteAyarlari();
        ayarlar.kahramanBaslik = "Doktorunuzdan randevu almak birkaç dakika sürer.";
        ayarlar.kahramanMetni = "Hastaneyi ve hizmeti seçin, doktorların boş saatlerini aynı ekranda görün, "
                + "size uyan saate tek dokunuşla randevu talebi bırakın.";
        return ayarlar;
    }

    /** Ana sayfadaki karşılama başlığı ve açıklaması. */
    public void karsilamaGuncelle(String baslik, String metin, String duzenleyen) {
        if (baslik == null || baslik.isBlank() || metin == null || metin.isBlank()) {
            throw new IsKuraliIhlali("Ana sayfa başlığı ve açıklaması boş olamaz.");
        }
        this.kahramanBaslik = baslik.trim();
        this.kahramanMetni = metin.trim();
        damgala(duzenleyen);
    }

    /** Sayfaların üstündeki duyuru bandı; boş bırakılırsa gizlenir. */
    public void bilgiBandiGuncelle(String bant, String duzenleyen) {
        this.bilgiBandi = bosIseNull(bant);
        damgala(duzenleyen);
    }

    /** Alt bilgideki iletişim bilgileri. */
    public void iletisimGuncelle(String yeniTelefon, String yeniEposta, String yeniAdres, String duzenleyen) {
        if (yeniEposta != null && !yeniEposta.isBlank() && !yeniEposta.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IsKuraliIhlali("Geçerli bir e-posta adresi giriniz.");
        }
        this.telefon = bosIseNull(yeniTelefon);
        this.eposta = bosIseNull(yeniEposta);
        this.adres = bosIseNull(yeniAdres);
        damgala(duzenleyen);
    }

    private void damgala(String duzenleyen) {
        this.guncellemeZamani = LocalDateTime.now();
        this.guncelleyen = duzenleyen;
    }

    private static String bosIseNull(String deger) {
        return deger == null || deger.isBlank() ? null : deger.trim();
    }

    public Long getId() {
        return id;
    }

    public String getKahramanBaslik() {
        return kahramanBaslik;
    }

    public String getKahramanMetni() {
        return kahramanMetni;
    }

    public String getBilgiBandi() {
        return bilgiBandi;
    }

    public String getTelefon() {
        return telefon;
    }

    public String getEposta() {
        return eposta;
    }

    public String getAdres() {
        return adres;
    }

    public LocalDateTime getGuncellemeZamani() {
        return guncellemeZamani;
    }

    public String getGuncelleyen() {
        return guncelleyen;
    }
}
