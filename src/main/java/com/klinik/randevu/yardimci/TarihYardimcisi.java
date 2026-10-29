package com.klinik.randevu.yardimci;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Şablonlarda {@code @tarih} adıyla kullanılan Türkçe tarih ve para biçimleri. */
@Component("tarih")
public class TarihYardimcisi {

    private static final Locale TURKCE = Locale.forLanguageTag("tr-TR");
    private static final DateTimeFormatter UZUN_TARIH = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TURKCE);
    private static final DateTimeFormatter GUN_AY = DateTimeFormatter.ofPattern("d MMMM", TURKCE);

    public static String gunAdiniGetir(DayOfWeek gun) {
        return gun == null ? "" : gun.getDisplayName(TextStyle.FULL, TURKCE);
    }

    public String gunAdi(DayOfWeek gun) {
        return gunAdiniGetir(gun);
    }

    public String kisaGunAdi(DayOfWeek gun) {
        return gun.getDisplayName(TextStyle.SHORT, TURKCE);
    }

    public DayOfWeek[] gunler() {
        return DayOfWeek.values();
    }

    /** "21 Ekim 2026, Çarşamba" */
    public String uzunTarih(LocalDate tarih) {
        return tarih.format(UZUN_TARIH);
    }

    /** "21 Ekim" */
    public String gunAy(LocalDate tarih) {
        return tarih.format(GUN_AY);
    }

    /** Bugün / Yarın / gün adı. */
    public String goreceliGun(LocalDate tarih) {
        LocalDate bugun = LocalDate.now();
        if (tarih.equals(bugun)) {
            return "Bugün";
        }
        if (tarih.equals(bugun.plusDays(1))) {
            return "Yarın";
        }
        return gunAdiniGetir(tarih.getDayOfWeek());
    }

    /**
     * Liste satırları için: Bugün / Yarın / Dün; önümüzdeki 6 gün içinde gün adı;
     * daha uzak ya da daha eski tarihlerde "16 Ekim" gibi tam tarih (hangi Cuma olduğu belirsiz kalmasın).
     */
    public String tarihEtiketi(LocalDate tarih) {
        LocalDate bugun = LocalDate.now();
        if (tarih.equals(bugun)) {
            return "Bugün";
        }
        if (tarih.equals(bugun.plusDays(1))) {
            return "Yarın";
        }
        if (tarih.equals(bugun.minusDays(1))) {
            return "Dün";
        }
        if (tarih.isAfter(bugun) && tarih.isBefore(bugun.plusDays(7))) {
            return gunAdiniGetir(tarih.getDayOfWeek());
        }
        return gunAy(tarih);
    }

    /** "1.200,00 ₺" */
    public String para(BigDecimal tutar) {
        DecimalFormat bicim = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(TURKCE));
        return bicim.format(tutar) + " ₺";
    }
}
