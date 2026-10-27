package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.randevu.Randevu;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;

/** Kurumları tanımlayan ve kurum yöneticilerini atayan üst düzey yönetici. */
@Entity
@DiscriminatorValue("SISTEM_YONETICISI")
public class SistemYoneticisi extends Kullanici {

    protected SistemYoneticisi() {
    }

    public SistemYoneticisi(String kurumsalEposta, String sifreOzeti, String adSoyad) {
        super(kurumsalEposta, sifreOzeti, adSoyad);
        epostaGuncelle(kurumsalEposta);
    }

    @Override
    public Rol getRol() {
        return Rol.SISTEM_YONETICISI;
    }

    /** Sistem yöneticisi herkesin gördüğü sitede, kendisine görünen düzenleme düğmeleriyle çalışır. */
    @Override
    public String getPanelYolu() {
        return "/";
    }

    @Override
    public boolean kurumuYonetebilirMi(Kurum kurum) {
        return true;
    }

    @Override
    public boolean randevuyuYonetebilirMi(Randevu randevu) {
        return true;
    }

    @Override
    public boolean calisaniYonetebilirMi(Calisan calisan) {
        return true;
    }

    @Override
    public boolean icerikYazabilirMi() {
        return true;
    }

    @Override
    public boolean icerigiYonetebilirMi(Duyuru duyuru) {
        return true;
    }

    /** Boş bırakılırsa genel (tüm sistem) duyurusu olur. */
    @Override
    public Kurum icerikKurumunuBelirle(Kurum istenen) {
        return istenen;
    }

    @Override
    public boolean siteAyarlariniDuzenleyebilirMi() {
        return true;
    }

    @Override
    public boolean kurumTanimlayabilirMi() {
        return true;
    }

    @Override
    public List<MenuBaglantisi> getHesapMenusu() {
        return List.of(new MenuBaglantisi("kurumlar", "Kurumlar ve yöneticiler", "/#kurumlar", "bina"),
                new MenuBaglantisi("haberler", "Duyuruları yönet", "/haberler", "not"));
    }
}
