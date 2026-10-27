package com.klinik.randevu.model.kullanici;

/**
 * T.C. Kimlik Numarası doğrulaması.
 * <p>
 * 11 hane, ilk hane 0 olamaz; 10. hane tek ve çift sıradaki hanelerden,
 * 11. hane ilk 10 hanenin toplamından hesaplanan kontrol haneleridir.
 */
public final class TcKimlikNo {

    private TcKimlikNo() {
    }

    public static boolean gecerliMi(String deger) {
        if (deger == null || !deger.matches("[1-9]\\d{10}")) {
            return false;
        }
        int[] h = deger.chars().map(c -> c - '0').toArray();
        int tekler = h[0] + h[2] + h[4] + h[6] + h[8];
        int ciftler = h[1] + h[3] + h[5] + h[7];
        int onuncu = Math.floorMod(tekler * 7 - ciftler, 10);
        int ilkOnToplami = tekler + ciftler + h[9];
        return h[9] == onuncu && h[10] == ilkOnToplami % 10;
    }

    /** Ekranda gösterim için: 12*******46 */
    public static String maskele(String deger) {
        if (deger == null || deger.length() != 11) {
            return deger;
        }
        return deger.substring(0, 2) + "*******" + deger.substring(9);
    }
}
