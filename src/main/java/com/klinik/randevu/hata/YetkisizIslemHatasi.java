package com.klinik.randevu.hata;

/** Kullanıcının yetkisi olmayan bir kayıt üzerinde işlem yapmaya çalışması. */
public class YetkisizIslemHatasi extends IsKuraliIhlali {

    public YetkisizIslemHatasi(String mesaj) {
        super(mesaj);
    }
}
