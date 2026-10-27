package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.randevu.Randevu;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.List;

/** Bir hastane/kliniğin yöneticisi; yalnızca kendi kurumunun tüm şubelerinde yetkilidir. */
@Entity
@DiscriminatorValue("YONETICI")
public class Yonetici extends Kullanici {

    @ManyToOne
    private Kurum kurum;

    protected Yonetici() {
    }

    public Yonetici(String kurumsalEposta, String sifreOzeti, String adSoyad, Kurum kurum) {
        super(kurumsalEposta, sifreOzeti, adSoyad);
        epostaGuncelle(kurumsalEposta);
        this.kurum = kurum;
    }

    @Override
    public Rol getRol() {
        return Rol.YONETICI;
    }

    @Override
    public String getPanelYolu() {
        return "/yonetici";
    }

    @Override
    public String getCalismaAlaniEtiketi() {
        return "Kurum yönetimi";
    }

    @Override
    public boolean kurumuYonetebilirMi(Kurum hedef) {
        return kurum.equals(hedef);
    }

    @Override
    public boolean randevuyuYonetebilirMi(Randevu randevu) {
        return kurumuYonetebilirMi(randevu.getSube().getKurum());
    }

    @Override
    public boolean calisaniYonetebilirMi(Calisan calisan) {
        return kurumuYonetebilirMi(calisan.getKurum());
    }

    @Override
    public boolean icerikYazabilirMi() {
        return true;
    }

    @Override
    public boolean icerigiYonetebilirMi(Duyuru duyuru) {
        return !duyuru.isGenel() && kurumuYonetebilirMi(duyuru.getKurum());
    }

    /** Kurum yöneticisinin yazdığı her içerik kendi kurumuna aittir. */
    @Override
    public Kurum icerikKurumunuBelirle(Kurum istenen) {
        return kurum;
    }

    @Override
    public List<MenuBaglantisi> getHesapMenusu() {
        return List.of(new MenuBaglantisi("genel", "Kurum yönetimi", "/yonetici", "pano"),
                new MenuBaglantisi("haberler", "Kurum duyuruları", "/haberler", "not"));
    }

    @Override
    public List<MenuBaglantisi> getCalismaSekmeleri() {
        return List.of(new MenuBaglantisi("genel", "Genel bakış", "/yonetici", "pano"),
                new MenuBaglantisi("randevular", "Randevular", "/yonetici/randevular", "takvim"),
                new MenuBaglantisi("calisanlar", "Personel", "/yonetici/calisanlar", "kullanicilar"),
                new MenuBaglantisi("hizmetler", "Hizmetler", "/yonetici/hizmetler", "hizmet"),
                new MenuBaglantisi("subeler", "Şubeler", "/yonetici/subeler", "konum"));
    }

    @Override
    public String getCalismaAlaniBasligi() {
        return kurum.getAd();
    }

    @Override
    public List<ProfilBilgisi> getProfilBilgileri() {
        return List.of(new ProfilBilgisi("Kurum", kurum.getAd()));
    }

    public Kurum getKurum() {
        return kurum;
    }
}
