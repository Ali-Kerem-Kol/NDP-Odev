package com.klinik.randevu.model.kullanici;

import com.klinik.randevu.hata.IsKuraliIhlali;
import com.klinik.randevu.hata.YetkisizIslemHatasi;
import com.klinik.randevu.model.TemelVarlik;
import com.klinik.randevu.model.icerik.Duyuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.randevu.Randevu;
import jakarta.persistence.*;

import java.util.List;
import java.util.Locale;

/**
 * Sistemdeki tüm kullanıcıların soyut üst sınıfı.
 * <p>
 * Tek tablo kalıtımı (SINGLE_TABLE) ile saklanır; {@code kullanici_tipi} sütunu
 * kaydın hangi alt sınıfa ait olduğunu belirtir. Yetki kararları ("bu randevuyu
 * onaylayabilir mi?" gibi) alt sınıflarda ezilen metotlarla verilir; böylece
 * servisler kullanıcının türünü sorgulamadan çok biçimli olarak çalışır.
 * <p>
 * Giriş kimliği türe göre değişir: hasta T.C. Kimlik No ile, personel kurumsal
 * e-posta adresiyle giriş yapar (bkz. {@link #girisEpostaIleMi()}).
 */
@Entity
@Table(name = "kullanicilar")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kullanici_tipi")
public abstract class Kullanici extends TemelVarlik {

    /** Hastada T.C. Kimlik No, personelde küçük harfe çevrilmiş e-posta. */
    @Column(nullable = false, unique = true, length = 120)
    private String girisKimligi;

    /** BCrypt ile özetlenmiş parola; düz metin asla saklanmaz. */
    @Column(nullable = false)
    private String sifre;

    @Column(nullable = false, length = 100)
    private String adSoyad;

    @Column(length = 120)
    private String eposta;

    @Column(length = 30)
    private String telefon;

    private boolean aktif = true;

    protected Kullanici() {
    }

    protected Kullanici(String girisKimligi, String sifreOzeti, String adSoyad) {
        if (girisKimligi == null || girisKimligi.isBlank()) {
            throw new IsKuraliIhlali("Giriş kimliği boş olamaz.");
        }
        if (adSoyad == null || adSoyad.isBlank()) {
            throw new IsKuraliIhlali("Ad soyad boş olamaz.");
        }
        this.girisKimligi = kimligiNormallestir(girisKimligi);
        this.sifre = sifreOzeti;
        this.adSoyad = adSoyad.trim().replaceAll("\\s+", " ");
    }

    /** Giriş formundan gelen değer ile kayıtlı kimlik aynı biçimde karşılaştırılır. */
    public static String kimligiNormallestir(String deger) {
        return deger == null ? null : deger.trim().toLowerCase(Locale.ROOT);
    }

    public abstract Rol getRol();

    /** Giriş sonrası kullanıcının yönlendirileceği panel adresi. */
    public abstract String getPanelYolu();

    /**
     * Üst menüde gösterilen çalışma alanı bağlantısının adı (ör. "Randevularım", "Kurum yönetimi").
     * Sistem yöneticisi sitenin kendisinde çalıştığı için ayrı bir bağlantısı yoktur.
     */
    public String getCalismaAlaniEtiketi() {
        return null;
    }

    /** Personel e-postayla giriş yapar; e-posta değişince giriş kimliği de değişir. Hasta bunu ezer. */
    public boolean girisEpostaIleMi() {
        return true;
    }

    /** Giriş ekranında ve profilde gösterilen kimlik etiketi. */
    public String getGirisKimligiEtiketi() {
        return girisEpostaIleMi() ? "Kurumsal e-posta" : "T.C. Kimlik No";
    }

    /** Ekranda gösterilecek ad; doktorlar "Dr." ön ekiyle görünür. */
    public String getGorunenAd() {
        return adSoyad;
    }

    /** Avatar için baş harfler: "Ayşe Demir" → "AD". */
    public String getBasHarfler() {
        String[] parcalar = adSoyad.split(" ");
        String ilk = parcalar[0].substring(0, 1);
        String son = parcalar.length > 1 ? parcalar[parcalar.length - 1].substring(0, 1) : "";
        return (ilk + son).toUpperCase(Locale.forLanguageTag("tr"));
    }

    /** Kendisi için randevu alabilir mi? Yalnızca hasta; personel ve yöneticiler hasta adına randevu almaz. */
    public boolean randevuAlabilirMi() {
        return false;
    }

    /** Randevuyu onaylama, reddetme ve tamamlama yetkisi. Varsayılan: yok. */
    public boolean randevuyuYonetebilirMi(Randevu randevu) {
        return false;
    }

    /** Randevu detayını ve geçmişini görme yetkisi. Varsayılan olarak yönetme yetkisiyle aynıdır. */
    public boolean randevuyuGorebilirMi(Randevu randevu) {
        return randevuyuYonetebilirMi(randevu);
    }

    /** Randevuyu iptal etme yetkisi. Varsayılan olarak yönetme yetkisiyle aynıdır. */
    public boolean randevuyuIptalEdebilirMi(Randevu randevu) {
        return randevuyuYonetebilirMi(randevu);
    }

    /** Kurumun şube, hizmet ve personel tanımlarını değiştirebilir mi? */
    public boolean kurumuYonetebilirMi(Kurum kurum) {
        return false;
    }

    /** Ana sayfaya duyuru/haber yazabilir mi? Varsayılan: hayır. */
    public boolean icerikYazabilirMi() {
        return false;
    }

    /** Bu duyuruyu düzenleyebilir, yayımlayabilir, silebilir mi? */
    public boolean icerigiYonetebilirMi(Duyuru duyuru) {
        return false;
    }

    /**
     * Yazılan içeriğin hangi kuruma ait olacağına karar verir. Kurum yöneticisi
     * yalnızca kendi kurumu adına yazar; sistem yöneticisi genel ya da istediği kurum adına.
     */
    public Kurum icerikKurumunuBelirle(Kurum istenen) {
        throw new YetkisizIslemHatasi("İçerik yazma yetkiniz yok.");
    }

    /** Yeni kurum tanımlayabilir, kurumu yayından kaldırıp silebilir, kuruma yönetici atayabilir mi? */
    public boolean kurumTanimlayabilirMi() {
        return false;
    }

    /**
     * Hesap menüsünde, "Hesabım" ve "Çıkış yap" bağlantılarının üstünde görünen kısayollar.
     * Her kullanıcı türü kendi kısayollarını döndürür; varsayılan olarak yoktur.
     */
    public List<MenuBaglantisi> getHesapMenusu() {
        return List.of();
    }

    /**
     * Üst menünün ikinci satırındaki çalışma alanı sekmeleri (kurum yönetimi, doktorun çalışma alanı).
     * Hasta ve sistem yöneticisinin ayrı bir çalışma alanı olmadığı için boş liste döner.
     */
    public List<MenuBaglantisi> getCalismaSekmeleri() {
        return List.of();
    }

    /** Çalışma alanı sekmelerinin solunda görünen başlık (kurum ya da şube adı). */
    public String getCalismaAlaniBasligi() {
        return null;
    }

    /** Verilen sayfa anahtarı bu kullanıcının çalışma alanı sekmelerinden biri mi? */
    public final boolean calismaSekmesiMi(String anahtar) {
        return getCalismaSekmeleri().stream().anyMatch(sekme -> sekme.anahtar().equals(anahtar));
    }

    /** Hesabım sayfasındaki kimlik kartına türe özgü satırlar (T.C. Kimlik No, görev yeri, kurum...). */
    public List<ProfilBilgisi> getProfilBilgileri() {
        return List.of();
    }

    /** Ana sayfa metinlerini ve iletişim bilgilerini düzenleyebilir mi? */
    public boolean siteAyarlariniDuzenleyebilirMi() {
        return false;
    }

    /** Çalışanın uygunluk takvimini düzenleyebilir mi? */
    public boolean calisaniYonetebilirMi(Calisan calisan) {
        return false;
    }

    public void epostaGuncelle(String yeniEposta) {
        String temiz = bosIseNull(yeniEposta);
        if (temiz != null && !temiz.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IsKuraliIhlali("Geçerli bir e-posta adresi giriniz.");
        }
        if (girisEpostaIleMi()) {
            if (temiz == null) {
                throw new IsKuraliIhlali("Kurumsal e-posta boş bırakılamaz.");
            }
            this.girisKimligi = kimligiNormallestir(temiz);
            temiz = this.girisKimligi;
        }
        this.eposta = temiz;
    }

    public void telefonGuncelle(String yeniTelefon) {
        this.telefon = bosIseNull(yeniTelefon);
    }

    public void sifreGuncelle(String yeniSifreOzeti) {
        this.sifre = yeniSifreOzeti;
    }

    /** Kurum kaydındaki yazım hatası gibi durumlarda yönetici adı düzeltebilir. */
    public void adSoyadDuzelt(String yeniAdSoyad) {
        if (yeniAdSoyad == null || yeniAdSoyad.isBlank()) {
            throw new IsKuraliIhlali("Ad soyad boş olamaz.");
        }
        this.adSoyad = yeniAdSoyad.trim().replaceAll("\\s+", " ");
    }

    /** Pasif hesap giriş yapamaz; personel ise randevu listelerinde görünmez. */
    public void pasiflestir() {
        this.aktif = false;
    }

    public void aktiflestir() {
        this.aktif = true;
    }

    protected boolean ayniKayitMi(Kullanici diger) {
        return equals(diger);
    }

    private static String bosIseNull(String deger) {
        return deger == null || deger.isBlank() ? null : deger.trim();
    }

    public String getGirisKimligi() {
        return girisKimligi;
    }

    public String getSifre() {
        return sifre;
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public String getEposta() {
        return eposta;
    }

    public String getTelefon() {
        return telefon;
    }

    public boolean isAktif() {
        return aktif;
    }
}
