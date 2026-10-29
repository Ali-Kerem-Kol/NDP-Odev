package com.klinik.randevu.servis;

import com.klinik.randevu.model.kullanici.Calisan;
import com.klinik.randevu.model.kullanici.Doktor;
import com.klinik.randevu.model.kullanici.SaglikPersoneli;

/**
 * Oluşturulabilecek çalışan türleri. Her sabit kendi nesnesini üretir
 * (Fabrika Metodu deseni); servis hangi alt sınıfın oluşturulduğunu bilmez.
 */
public enum CalisanTipi {

    DOKTOR("Doktor", "Uzmanlık Alanı") {
        @Override
        public Calisan olustur(String kurumsalEposta, String sifreOzeti, String adSoyad, String unvanBilgisi) {
            return new Doktor(kurumsalEposta, sifreOzeti, adSoyad, unvanBilgisi);
        }
    },
    SAGLIK_PERSONELI("Sağlık Personeli", "Görev") {
        @Override
        public Calisan olustur(String kurumsalEposta, String sifreOzeti, String adSoyad, String unvanBilgisi) {
            return new SaglikPersoneli(kurumsalEposta, sifreOzeti, adSoyad, unvanBilgisi);
        }
    };

    private final String etiket;
    private final String unvanEtiketi;

    CalisanTipi(String etiket, String unvanEtiketi) {
        this.etiket = etiket;
        this.unvanEtiketi = unvanEtiketi;
    }

    public abstract Calisan olustur(String kurumsalEposta, String sifreOzeti, String adSoyad, String unvanBilgisi);

    public String getEtiket() {
        return etiket;
    }

    public String getUnvanEtiketi() {
        return unvanEtiketi;
    }
}
