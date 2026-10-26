package com.klinik.randevu.model.kurum;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.TemelVarlik;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Kurumun fiziksel şubesi. Çalışma saatleri ve bölümleri şube bazında tutulur.
 * Tek şubeli bir klinik için de bir adet şube kaydı bulunur.
 */
@Entity
@Table(name = "subeler")
public class Sube extends TemelVarlik {

    @Column(nullable = false, length = 150)
    private String ad;

    @Column(length = 300)
    private String adres;

    @Column(length = 30)
    private String telefon;

    @ManyToOne(optional = false)
    private Kurum kurum;

    @ElementCollection
    @CollectionTable(name = "sube_calisma_saatleri", joinColumns = @JoinColumn(name = "sube_id"))
    private List<CalismaSaati> calismaSaatleri = new ArrayList<>();

    @OneToMany(mappedBy = "sube", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bolum> bolumler = new ArrayList<>();

    protected Sube() {
    }

    Sube(Kurum kurum, String ad, String adres, String telefon) {
        this.kurum = kurum;
        bilgileriGuncelle(ad, adres, telefon);
    }

    public void bilgileriGuncelle(String ad, String adres, String telefon) {
        if (ad == null || ad.isBlank()) {
            throw new IsKuraliIhlali("Şube adı boş olamaz.");
        }
        this.ad = ad.trim();
        this.adres = bosIseNull(adres);
        this.telefon = bosIseNull(telefon);
    }

    /** Aynı gün için daha önce saat tanımlıysa yenisiyle değiştirilir. */
    public void calismaSaatiAyarla(DayOfWeek gun, LocalTime acilis, LocalTime kapanis) {
        CalismaSaati yeni = new CalismaSaati(gun, acilis, kapanis);
        calismaSaatleri.removeIf(c -> c.getGun() == gun);
        calismaSaatleri.add(yeni);
    }

    public void calismaSaatiKaldir(DayOfWeek gun) {
        calismaSaatleri.removeIf(c -> c.getGun() == gun);
    }

    public Optional<CalismaSaati> calismaSaati(DayOfWeek gun) {
        return calismaSaatleri.stream().filter(c -> c.getGun() == gun).findFirst();
    }

    /** Şube verilen zaman aralığının tamamında açık mı? Aralık gün aşamaz. */
    public boolean acikMi(LocalDateTime baslangic, LocalDateTime bitis) {
        if (!baslangic.toLocalDate().equals(bitis.toLocalDate())) {
            return false;
        }
        return calismaSaati(baslangic.getDayOfWeek())
                .map(c -> c.kapsar(baslangic.toLocalTime(), bitis.toLocalTime()))
                .orElse(false);
    }

    public Bolum bolumEkle(String bolumAdi, String aciklama) {
        bolumAdiKullanilabilirMi(bolumAdi, null);
        Bolum bolum = new Bolum(this, bolumAdi, aciklama);
        bolumler.add(bolum);
        return bolum;
    }

    /** Bölüm adı şube içinde benzersiz kalır; kontrolü bölümleri bilen şube yapar. */
    public void bolumGuncelle(Bolum bolum, String yeniAd, String aciklama) {
        bolumuDogrula(bolum);
        bolumAdiKullanilabilirMi(yeniAd, bolum);
        bolum.guncelle(yeniAd, aciklama);
    }

    public void bolumKaldir(Bolum bolum) {
        bolumuDogrula(bolum);
        bolumler.remove(bolum);
    }

    private void bolumuDogrula(Bolum bolum) {
        if (!bolumler.contains(bolum)) {
            throw new IsKuraliIhlali("Bölüm bu şubeye ait değil.");
        }
    }

    private void bolumAdiKullanilabilirMi(String bolumAdi, Bolum haric) {
        if (bolumAdi == null || bolumAdi.isBlank()) {
            throw new IsKuraliIhlali("Bölüm adı boş olamaz.");
        }
        boolean ayniAdVar = bolumler.stream()
                .filter(b -> b != haric)
                .anyMatch(b -> b.getAd().equalsIgnoreCase(bolumAdi.trim()));
        if (ayniAdVar) {
            throw new IsKuraliIhlali("Bu şubede '" + bolumAdi.trim() + "' adlı bir bölüm zaten var.");
        }
    }

    private static String bosIseNull(String deger) {
        return deger == null || deger.isBlank() ? null : deger.trim();
    }

    public String getTamAd() {
        return kurum.getAd() + " - " + ad;
    }

    public String getAd() {
        return ad;
    }

    public String getAdres() {
        return adres;
    }

    public String getTelefon() {
        return telefon;
    }

    public Kurum getKurum() {
        return kurum;
    }

    public List<CalismaSaati> getCalismaSaatleri() {
        return calismaSaatleri.stream().sorted(Comparator.comparing(CalismaSaati::getGun)).toList();
    }

    public List<Bolum> getBolumler() {
        return Collections.unmodifiableList(bolumler);
    }
}
