package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.randevu.Randevu;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Randevu alan kullanıcı. T.C. Kimlik No ile giriş yapar ({@link TcKimlikNo} ile doğrulanır).
 * Yalnızca kendi randevusunu görür ve henüz başlamamışsa iptal edebilir; bu kararları
 * {@link Kullanici} üst sınıfındaki metotları ezerek kendisi verir.
 */
@Entity
@DiscriminatorValue("HASTA")
public class Hasta extends Kullanici {

    private LocalDate dogumTarihi;

    protected Hasta() {
    }

    public Hasta(String tcKimlikNo, String sifreOzeti, String adSoyad, LocalDate dogumTarihi) {
        super(tcKimlikNoDogrula(tcKimlikNo), sifreOzeti, adSoyad);
        this.dogumTarihi = dogumTarihi;
    }

    private static String tcKimlikNoDogrula(String tcKimlikNo) {
        String temiz = tcKimlikNo == null ? null : tcKimlikNo.trim();
        if (!TcKimlikNo.gecerliMi(temiz)) {
            throw new IsKuraliIhlali("Geçerli bir T.C. Kimlik No giriniz.");
        }
        return temiz;
    }

    @Override
    public Rol getRol() {
        return Rol.HASTA;
    }

    @Override
    public String getPanelYolu() {
        return "/hasta";
    }

    @Override
    public String getCalismaAlaniEtiketi() {
        return "Randevularım";
    }

    /** Hasta T.C. Kimlik No ile giriş yapar; e-postası yalnızca iletişim içindir. */
    @Override
    public boolean girisEpostaIleMi() {
        return false;
    }

    @Override
    public boolean randevuAlabilirMi() {
        return true;
    }

    @Override
    public boolean randevuyuGorebilirMi(Randevu randevu) {
        return ayniKayitMi(randevu.getHasta());
    }

    /** Hasta yalnızca kendi randevusunu ve henüz başlamamışsa iptal edebilir. */
    @Override
    public boolean randevuyuIptalEdebilirMi(Randevu randevu) {
        return ayniKayitMi(randevu.getHasta()) && randevu.getBaslangic().isAfter(LocalDateTime.now());
    }

    @Override
    public List<MenuBaglantisi> getHesapMenusu() {
        return List.of(new MenuBaglantisi("randevularim", "Randevularım", "/hasta", "takvim"),
                new MenuBaglantisi("randevu-al", "Randevu al", "/hasta/randevu-al", "takvim-arti"));
    }

    /** Hasta kendi T.C. Kimlik No'sunu da maskeli görür; doğum tarihi varsa yaşıyla birlikte gösterilir. */
    @Override
    public List<ProfilBilgisi> getProfilBilgileri() {
        List<ProfilBilgisi> bilgiler = new ArrayList<>();
        bilgiler.add(new ProfilBilgisi("T.C. Kimlik No", getMaskeliTcKimlikNo()));
        if (dogumTarihi != null) {
            bilgiler.add(new ProfilBilgisi("Doğum tarihi",
                    dogumTarihi.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr")))
                            + " (" + getYas() + " yaş)"));
        }
        return bilgiler;
    }

    public String getTcKimlikNo() {
        return getGirisKimligi();
    }

    public String getMaskeliTcKimlikNo() {
        return TcKimlikNo.maskele(getGirisKimligi());
    }

    public Integer getYas() {
        return dogumTarihi == null ? null : Period.between(dogumTarihi, LocalDate.now()).getYears();
    }

    public LocalDate getDogumTarihi() {
        return dogumTarihi;
    }
}
