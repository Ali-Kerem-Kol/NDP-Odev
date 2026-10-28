package com.klinik.randevu.model.randevu;

import java.util.List;

/**
 * Randevunun yaşam döngüsü. İzin verilen geçişler:
 * <pre>
 * BEKLEMEDE ──► ONAYLANDI ──► TAMAMLANDI   (personel işaretler ya da süre dolunca sistem)
 *     │             ├──► GELMEDI           (randevu saati geçtikten sonra personel)
 *     │             └──► IPTAL
 *     ├──► REDDEDILDI
 *     ├──► IPTAL
 *     └──► ZAMAN_ASIMI                     (saat geldiğinde hâlâ onaylanmamışsa sistem)
 * </pre>
 */
public enum RandevuDurumu {
    BEKLEMEDE("Onay bekliyor", "uyari"),
    ONAYLANDI("Onaylandı", "basari"),
    REDDEDILDI("Reddedildi", "tehlike"),
    IPTAL("İptal edildi", "notr"),
    TAMAMLANDI("Tamamlandı", "bilgi"),
    GELMEDI("Gelmedi", "tehlike"),
    ZAMAN_ASIMI("Onaylanmadı", "notr");

    private final String etiket;
    private final String renk;

    RandevuDurumu(String etiket, String renk) {
        this.etiket = etiket;
        this.renk = renk;
    }

    public boolean gecebilir(RandevuDurumu hedef) {
        return switch (this) {
            case BEKLEMEDE -> hedef == ONAYLANDI || hedef == REDDEDILDI || hedef == IPTAL || hedef == ZAMAN_ASIMI;
            case ONAYLANDI -> hedef == TAMAMLANDI || hedef == GELMEDI || hedef == IPTAL;
            case REDDEDILDI, IPTAL, TAMAMLANDI, GELMEDI, ZAMAN_ASIMI -> false;
        };
    }

    /** Aktif randevular takvimde yer kaplar ve çakışma kontrolüne girer. */
    public boolean isAktif() {
        return this == BEKLEMEDE || this == ONAYLANDI;
    }

    public static List<RandevuDurumu> aktifDurumlar() {
        return List.of(BEKLEMEDE, ONAYLANDI);
    }

    public String getEtiket() {
        return etiket;
    }

    public String getRenk() {
        return renk;
    }
}
