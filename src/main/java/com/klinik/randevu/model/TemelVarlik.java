package com.klinik.randevu.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;

/**
 * Veritabanında saklanan tüm varlıkların soyut üst sınıfı.
 * <p>
 * Kimlik alanını ({@code id}) ve Java'nın {@link Object} sözleşmesini ({@code equals}, {@code hashCode},
 * {@code toString}) tek yerde tanımlar; alt sınıflar bunları tekrar yazmaz (kalıtım ile kod tekrarının önlenmesi).
 * Kimlik dışarıdan atanamaz, yalnızca okunur (kapsülleme).
 * <p>
 * Eşitlik kuralı: iki varlık, aynı somut türden ve aynı kimliğe sahipse eşittir. Henüz kaydedilmemiş
 * (kimliği olmayan) nesne yalnızca kendisine eşittir. {@code hashCode} türe göre sabittir; böylece nesne
 * kaydedilip kimlik kazandığında {@code HashSet} içindeki yeri değişmez.
 */
@MappedSuperclass
public abstract class TemelVarlik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    /** Henüz veritabanına kaydedilmemiş mi? */
    public boolean yeniMi() {
        return id == null;
    }

    @Override
    public final boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        if (diger == null || Hibernate.getClass(this) != Hibernate.getClass(diger)) {
            return false;
        }
        return id != null && id.equals(((TemelVarlik) diger).getId());
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    /** Günlüklerde okunur bir kimlik: "Randevu#12". Alt sınıflar daha açıklayıcı bir metin verebilir. */
    @Override
    public String toString() {
        return Hibernate.getClass(this).getSimpleName() + "#" + id;
    }
}
