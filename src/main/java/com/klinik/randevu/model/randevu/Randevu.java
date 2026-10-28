package com.klinik.randevu.model.randevu;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.model.TemelVarlik;
import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Hasta;
import com.klinik.randevu.model.kullanici.Kullanici;
import com.klinik.randevu.model.kurum.Hizmet;
import com.klinik.randevu.model.kurum.Sube;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Randevu kaydı. Oluşturulduğu andaki hizmet süresi ve ücreti kopyalanır;
 * hizmetin fiyatı sonradan değişse bile randevunun detayı korunur.
 * Durum değişiklikleri yalnızca bu sınıfın metotlarıyla yapılabilir (kapsülleme)
 * ve her değişiklik kimin yaptığıyla birlikte geçmişe yazılır.
 */
@Entity
@Table(name = "randevular")
public class Randevu extends TemelVarlik {

    @ManyToOne(optional = false)
    private Hasta hasta;

    @ManyToOne(optional = false)
    private Calisan calisan;

    @ManyToOne(optional = false)
    private Hizmet hizmet;

    @ManyToOne(optional = false)
    private Sube sube;

    @Column(nullable = false)
    private LocalDateTime baslangic;

    @Column(nullable = false)
    private LocalDateTime bitis;

    @Column(nullable = false)
    private int sureDakika;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ucret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RandevuDurumu durum;

    @Column(length = 500)
    private String hastaNotu;

    @Column(length = 500)
    private String durumAciklamasi;

    @Column(nullable = false)
    private LocalDateTime olusturmaZamani;

    private LocalDateTime guncellemeZamani;

    /** İyimser kilit: randevuyu aynı anda iki kişi güncellerse ikincisi reddedilir. */
    @Version
    private Long surum;

    @OneToMany(mappedBy = "randevu", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("zaman ASC, id ASC")
    private List<RandevuHareketi> hareketler = new ArrayList<>();

    protected Randevu() {
    }

    private Randevu(Hasta hasta, Calisan calisan, Hizmet hizmet, LocalDateTime baslangic, String hastaNotu,
                    LocalDateTime talepZamani) {
        this.hasta = hasta;
        this.calisan = calisan;
        this.hizmet = hizmet;
        this.sube = calisan.getSube();
        this.baslangic = baslangic;
        this.sureDakika = hizmet.getSureDakika();
        this.bitis = baslangic.plusMinutes(sureDakika);
        this.ucret = hizmet.getUcret();
        this.hastaNotu = bosIseNull(hastaNotu);
        this.durum = RandevuDurumu.BEKLEMEDE;
        this.olusturmaZamani = talepZamani;
        hareketler.add(new RandevuHareketi(this, durum, olusturmaZamani, hasta.getAdSoyad(), "Randevu talebi oluşturuldu"));
    }

    /** Fabrika metodu: yeni randevu her zaman "onay bekliyor" durumunda başlar. */
    public static Randevu olustur(Hasta hasta, Calisan calisan, Hizmet hizmet, LocalDateTime baslangic, String hastaNotu) {
        if (hasta == null || calisan == null || hizmet == null || baslangic == null) {
            throw new IsKuraliIhlali("Randevu için hasta, çalışan, hizmet ve zaman zorunludur.");
        }
        return new Randevu(hasta, calisan, hizmet, baslangic, hastaNotu, LocalDateTime.now());
    }

    /**
     * Daha önce alınmış bir randevunun kaydını aktarır (eski sistemden veri taşıma, örnek veri).
     * Talep zamanı geçmişte olmalı ve randevudan sonra olamaz; kurallar uygulanmaz, çağıran sorumludur.
     */
    public static Randevu gecmisKaydiAktar(Hasta hasta, Calisan calisan, Hizmet hizmet, LocalDateTime baslangic,
                                           String hastaNotu, LocalDateTime talepZamani) {
        if (hasta == null || calisan == null || hizmet == null || baslangic == null || talepZamani == null) {
            throw new IsKuraliIhlali("Randevu için hasta, çalışan, hizmet, zaman ve talep zamanı zorunludur.");
        }
        if (talepZamani.isAfter(LocalDateTime.now()) || talepZamani.isAfter(baslangic)) {
            throw new IsKuraliIhlali("Talep zamanı geçmişte ve randevu saatinden önce olmalıdır.");
        }
        return new Randevu(hasta, calisan, hizmet, baslangic, hastaNotu, talepZamani);
    }

    // ---- Kullanıcı işlemleri ----

    public void onayla(Kullanici yapan) {
        onayla(yapan, LocalDateTime.now());
    }

    public void onayla(Kullanici yapan, LocalDateTime zaman) {
        durumDegistir(RandevuDurumu.ONAYLANDI, yapan, null, islemZamani(zaman));
    }

    public void reddet(Kullanici yapan, String neden) {
        durumDegistir(RandevuDurumu.REDDEDILDI, yapan, neden, LocalDateTime.now());
    }

    /** Başlamış ya da geçmiş randevu iptal edilmez; gelmedi veya tamamlandı olarak kapatılır. */
    public void iptalEt(Kullanici yapan, String neden) {
        LocalDateTime simdi = LocalDateTime.now();
        if (!baslangic.isAfter(simdi)) {
            throw new IsKuraliIhlali("Başlamış randevu iptal edilemez. Hasta gelmediyse \"Gelmedi\", geldiyse \"Tamamlandı\" olarak işaretleyin.");
        }
        durumDegistir(RandevuDurumu.IPTAL, yapan, neden, simdi);
    }

    public void tamamla(Kullanici yapan) {
        tamamla(yapan, LocalDateTime.now());
    }

    public void tamamla(Kullanici yapan, LocalDateTime zaman) {
        LocalDateTime islem = islemZamani(zaman);
        if (baslangic.isAfter(islem)) {
            throw new IsKuraliIhlali("Henüz başlamamış bir randevu tamamlandı olarak işaretlenemez.");
        }
        durumDegistir(RandevuDurumu.TAMAMLANDI, yapan, null, islem);
    }

    public void gelmediOlarakIsaretle(Kullanici yapan) {
        gelmediOlarakIsaretle(yapan, LocalDateTime.now());
    }

    public void gelmediOlarakIsaretle(Kullanici yapan, LocalDateTime zaman) {
        LocalDateTime islem = islemZamani(zaman);
        if (baslangic.isAfter(islem)) {
            throw new IsKuraliIhlali("Randevu saati gelmeden hasta gelmedi olarak işaretlenemez.");
        }
        durumDegistir(RandevuDurumu.GELMEDI, yapan, null, islem);
    }

    /** Geçmişe dönük kayıt aktarımında işlem zamanı gelecekte ya da talepten önce olamaz. */
    private LocalDateTime islemZamani(LocalDateTime zaman) {
        if (zaman.isAfter(LocalDateTime.now()) || zaman.isBefore(olusturmaZamani)) {
            throw new IsKuraliIhlali("İşlem zamanı gelecekte ya da randevu talebinden önce olamaz.");
        }
        return zaman;
    }

    // ---- Sistemin zamana bağlı işlemleri ----

    /**
     * Saat geldiği halde onaylanmamış talep düşer; takvimdeki yer serbest kalır.
     * @return durum değiştiyse true
     */
    public boolean zamanAsiminiUygula(LocalDateTime simdi) {
        if (durum != RandevuDurumu.BEKLEMEDE || baslangic.isAfter(simdi)) {
            return false;
        }
        durumDegistir(RandevuDurumu.ZAMAN_ASIMI, null, "Randevu saatine kadar onaylanmadı", simdi);
        return true;
    }

    /**
     * Onaylı randevu bitişinden belirli süre sonra hâlâ işaretlenmemişse tamamlanmış sayılır.
     * Tolerans, personelin "gelmedi" işaretleyebilmesi için bırakılan süredir.
     */
    public boolean otomatikTamamla(LocalDateTime simdi, Duration tolerans) {
        if (durum != RandevuDurumu.ONAYLANDI || bitis.plus(tolerans).isAfter(simdi)) {
            return false;
        }
        durumDegistir(RandevuDurumu.TAMAMLANDI, null, "Randevu süresi doldu, otomatik tamamlandı", simdi);
        return true;
    }

    private void durumDegistir(RandevuDurumu hedef, Kullanici yapan, String aciklama, LocalDateTime zaman) {
        if (!durum.gecebilir(hedef)) {
            throw new IsKuraliIhlali("'" + durum.getEtiket() + "' durumundaki randevu '"
                    + hedef.getEtiket() + "' durumuna geçirilemez.");
        }
        this.durum = hedef;
        this.durumAciklamasi = bosIseNull(aciklama);
        this.guncellemeZamani = zaman;
        String yapanAd = yapan == null ? RandevuHareketi.SISTEM : yapan.getGorunenAd();
        hareketler.add(new RandevuHareketi(this, hedef, zaman, yapanAd, durumAciklamasi));
    }

    private static String bosIseNull(String metin) {
        return metin == null || metin.isBlank() ? null : metin.trim();
    }

    // ---- Sorgular ----

    public boolean cakisir(LocalDateTime digerBaslangic, LocalDateTime digerBitis) {
        return durum.isAktif() && baslangic.isBefore(digerBitis) && digerBaslangic.isBefore(bitis);
    }

    public boolean isOnaylanabilir() {
        return durum.gecebilir(RandevuDurumu.ONAYLANDI);
    }

    public boolean isIptalEdilebilir() {
        return durum.gecebilir(RandevuDurumu.IPTAL) && baslangic.isAfter(LocalDateTime.now());
    }

    public boolean isTamamlanabilir() {
        return durum.gecebilir(RandevuDurumu.TAMAMLANDI) && !baslangic.isAfter(LocalDateTime.now());
    }

    public boolean isGelmediIsaretlenebilir() {
        return durum.gecebilir(RandevuDurumu.GELMEDI) && !baslangic.isAfter(LocalDateTime.now());
    }

    /** Şu anda devam ediyor mu (onaylı ve saat aralığının içinde)? */
    public boolean isSuAnda() {
        LocalDateTime simdi = LocalDateTime.now();
        return durum == RandevuDurumu.ONAYLANDI && !baslangic.isAfter(simdi) && bitis.isAfter(simdi);
    }

    public Hasta getHasta() {
        return hasta;
    }

    public Calisan getCalisan() {
        return calisan;
    }

    public Hizmet getHizmet() {
        return hizmet;
    }

    public Sube getSube() {
        return sube;
    }

    public LocalDateTime getBaslangic() {
        return baslangic;
    }

    public LocalDateTime getBitis() {
        return bitis;
    }

    public int getSureDakika() {
        return sureDakika;
    }

    public BigDecimal getUcret() {
        return ucret;
    }

    public RandevuDurumu getDurum() {
        return durum;
    }

    public String getHastaNotu() {
        return hastaNotu;
    }

    public String getDurumAciklamasi() {
        return durumAciklamasi;
    }

    public LocalDateTime getOlusturmaZamani() {
        return olusturmaZamani;
    }

    public LocalDateTime getGuncellemeZamani() {
        return guncellemeZamani;
    }

    public List<RandevuHareketi> getHareketler() {
        return Collections.unmodifiableList(hareketler);
    }

    /** Örn. "Randevu#12 [ONAYLANDI] 2026-10-24T10:00". İlişkili nesnelere dokunmaz, sorgu tetiklemez. */
    @Override
    public String toString() {
        return super.toString() + " [" + durum + "] " + baslangic;
    }
}
