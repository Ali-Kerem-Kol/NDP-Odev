package com.klinik.randevu.model.kullanici;

/**
 * Kullanıcıya özel menüdeki tek bağlantı. Menüyü şablon değil kullanıcı nesnesinin kendisi üretir
 * ({@link Kullanici#getHesapMenusu()}, {@link Kullanici#getCalismaSekmeleri()}); böylece arayüz
 * kullanıcının türünü sorgulamadan, çok biçimli olarak çizilir.
 *
 * @param anahtar sayfanın kendini tanıttığı ad; etkin sekmeyi işaretlemek için kullanılır
 * @param etiket  ekranda görünen ad
 * @param yol     bağlantının adresi
 * @param ikon    ikon setindeki simgenin adı
 */
public record MenuBaglantisi(String anahtar, String etiket, String yol, String ikon) {
}
