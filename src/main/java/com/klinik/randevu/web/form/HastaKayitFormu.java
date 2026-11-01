package com.klinik.randevu.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Hasta kayıt formunun verisi ve doğrulama kuralları (Bean Validation).
 */
public class HastaKayitFormu {

    @NotBlank(message = "T.C. Kimlik No zorunludur")
    @Pattern(regexp = "\\d{11}", message = "T.C. Kimlik No 11 haneden oluşmalıdır")
    private String tcKimlikNo;

    @NotBlank(message = "Ad soyad zorunludur")
    @Size(min = 3, max = 100, message = "Ad soyad 3-100 karakter olmalıdır")
    private String adSoyad;

    @NotNull(message = "Doğum tarihi zorunludur")
    @Past(message = "Doğum tarihi bugünden önce olmalıdır")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dogumTarihi;

    @NotBlank(message = "Cep telefonu zorunludur")
    @Pattern(regexp = "^[0-9 +()-]{10,20}$", message = "Geçerli bir telefon numarası giriniz")
    private String telefon;

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String eposta;

    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
    private String sifre;

    @NotBlank(message = "Şifreyi tekrar giriniz")
    private String sifreTekrar;

    @AssertTrue(message = "Devam etmek için aydınlatma metnini onaylamanız gerekir")
    private boolean kvkkOnay;

    @AssertTrue(message = "Şifreler eşleşmiyor")
    public boolean isSifrelerEslesiyor() {
        return sifre == null || sifre.equals(sifreTekrar);
    }

    public String getTcKimlikNo() {
        return tcKimlikNo;
    }

    public void setTcKimlikNo(String tcKimlikNo) {
        this.tcKimlikNo = tcKimlikNo == null ? null : tcKimlikNo.trim();
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public void setAdSoyad(String adSoyad) {
        this.adSoyad = adSoyad;
    }

    public LocalDate getDogumTarihi() {
        return dogumTarihi;
    }

    public void setDogumTarihi(LocalDate dogumTarihi) {
        this.dogumTarihi = dogumTarihi;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getEposta() {
        return eposta;
    }

    public void setEposta(String eposta) {
        this.eposta = eposta;
    }

    public String getSifre() {
        return sifre;
    }

    public void setSifre(String sifre) {
        this.sifre = sifre;
    }

    public String getSifreTekrar() {
        return sifreTekrar;
    }

    public void setSifreTekrar(String sifreTekrar) {
        this.sifreTekrar = sifreTekrar;
    }

    public boolean isKvkkOnay() {
        return kvkkOnay;
    }

    public void setKvkkOnay(boolean kvkkOnay) {
        this.kvkkOnay = kvkkOnay;
    }
}
