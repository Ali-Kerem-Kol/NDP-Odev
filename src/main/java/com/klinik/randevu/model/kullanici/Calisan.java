package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.kurum.Bolum;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.Sube;
import com.klinik.randevu.model.randevu.Randevu;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Randevu verebilen sağlık çalışanlarının soyut üst sınıfı.
 * Doktor ve diğer sağlık personeli (teknisyen, hemşire...) bu sınıftan türer.
 * Çalışanın sunduğu hizmetler ve haftalık uygunluk dilimleri burada yönetilir.
 */
@Entity
public abstract class Calisan extends Kullanici {

    @ManyToOne
    private Sube sube;

    @ManyToOne
    private Bolum bolum;

    @ManyToMany
    @JoinTable(name = "calisan_hizmetleri",
            joinColumns = @JoinColumn(name = "calisan_id"),
            inverseJoinColumns = @JoinColumn(name = "hizmet_id"))
    private Set<Hizmet> hizmetler = new HashSet<>();

    @OneToMany(mappedBy = "calisan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UygunlukDilimi> uygunluklar = new ArrayList<>();

    protected Calisan() {
    }

    protected Calisan(String kurumsalEposta, String sifreOzeti, String adSoyad) {
        super(kurumsalEposta, sifreOzeti, adSoyad);
        epostaGuncelle(kurumsalEposta);
    }

    /** Ekranda gösterilen unvan; her alt sınıf kendine göre üretir. */
    public abstract String getUnvan();

    /** Formda düzenlenen ham bilgi: doktorda uzmanlık alanı, sağlık personelinde görev. */
    public abstract String getUnvanBilgisi();

    /** Unvan bilgisini günceller; her alt sınıf kendi alanını değiştirir. */
    public abstract void unvaniGuncelle(String yeniBilgi);

    /** Formdaki unvan alanının etiketi. */
    public abstract String getUnvanEtiketi();

    @Override
    public String getPanelYolu() {
        return "/calisan";
    }

    @Override
    public String getCalismaAlaniEtiketi() {
        return "Randevularım";
    }

    @Override
    public List<MenuBaglantisi> getHesapMenusu() {
        return List.of(new MenuBaglantisi("randevular", "Randevularım", "/calisan", "gelen"),
                new MenuBaglantisi("calisma-saatleri", "Çalışma saatlerim", "/calisan/uygunluk", "saat"));
    }

    @Override
    public List<MenuBaglantisi> getCalismaSekmeleri() {
        return List.of(new MenuBaglantisi("randevular", "Randevular", "/calisan", "gelen"),
                new MenuBaglantisi("calisma-saatleri", "Çalışma saatlerim", "/calisan/uygunluk", "saat"));
    }

    @Override
    public String getCalismaAlaniBasligi() {
        return sube == null ? null : sube.getTamAd();
    }

    @Override
    public List<ProfilBilgisi> getProfilBilgileri() {
        return List.of(new ProfilBilgisi("Görev yeri", getUnvan() + " · " + sube.getTamAd()));
    }

    /** Çalışan yalnızca kendisine atanmış randevuları yönetir. */
    @Override
    public boolean randevuyuYonetebilirMi(Randevu randevu) {
        return ayniKayitMi(randevu.getCalisan());
    }

    @Override
    public boolean calisaniYonetebilirMi(Calisan calisan) {
        return ayniKayitMi(calisan);
    }

    public void gorevlendir(Sube sube, Bolum bolum) {
        if (sube == null) {
            throw new IsKuraliIhlali("Çalışan bir şubeye bağlı olmalıdır.");
        }
        if (bolum != null && !bolum.getSube().equals(sube)) {
            throw new IsKuraliIhlali("Seçilen bölüm bu şubeye ait değil.");
        }
        this.sube = sube;
        this.bolum = bolum;
    }

    public void hizmetleriAyarla(Collection<Hizmet> yeniHizmetler) {
        for (Hizmet hizmet : yeniHizmetler) {
            if (!hizmet.getKurum().equals(getKurum())) {
                throw new IsKuraliIhlali("'" + hizmet.getAd() + "' bu çalışanın kurumuna ait bir hizmet değil.");
            }
        }
        hizmetler.clear();
        hizmetler.addAll(yeniHizmetler);
    }

    public boolean hizmetVerebilirMi(Hizmet hizmet) {
        return hizmetler.contains(hizmet);
    }

    public UygunlukDilimi uygunlukEkle(DayOfWeek gun, LocalTime baslangic, LocalTime bitis) {
        UygunlukDilimi yeni = new UygunlukDilimi(this, gun, baslangic, bitis);
        uygunluklar.stream()
                .filter(mevcut -> mevcut.cakisir(yeni))
                .findAny()
                .ifPresent(mevcut -> {
                    throw new IsKuraliIhlali("Yeni dilim mevcut bir dilimle çakışıyor: " + mevcut.getAciklama());
                });
        uygunluklar.add(yeni);
        return yeni;
    }

    public void uygunlukKaldir(Long dilimId) {
        boolean silindi = uygunluklar.removeIf(d -> d.getId() != null && d.getId().equals(dilimId));
        if (!silindi) {
            throw new IsKuraliIhlali("Uygunluk dilimi bulunamadı.");
        }
    }

    /** Verilen zaman aralığı çalışanın uygunluk dilimlerinden birinin içinde mi? */
    public boolean musaitMi(LocalDateTime baslangic, LocalDateTime bitis) {
        return uygunluklar.stream().anyMatch(d -> d.kapsar(baslangic, bitis));
    }

    public List<UygunlukDilimi> gunlukUygunluklar(DayOfWeek gun) {
        return getUygunluklar().stream().filter(d -> d.getGun() == gun).toList();
    }

    public Kurum getKurum() {
        return sube == null ? null : sube.getKurum();
    }

    public Sube getSube() {
        return sube;
    }

    public Bolum getBolum() {
        return bolum;
    }

    public List<Hizmet> getHizmetler() {
        return hizmetler.stream().sorted(Comparator.comparing(Hizmet::getAd)).toList();
    }

    public List<UygunlukDilimi> getUygunluklar() {
        return uygunluklar.stream()
                .sorted(Comparator.comparing(UygunlukDilimi::getGun).thenComparing(UygunlukDilimi::getBaslangic))
                .toList();
    }
}
