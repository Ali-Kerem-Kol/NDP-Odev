package com.klinik.randevu.model.randevu;

import com.klinik.randevu.model.TemelVarlik;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Randevunun durum geçmişindeki tek bir adım: kim, ne zaman, hangi duruma geçirdi.
 * Değiştirilemez; yalnızca {@link Randevu} oluşturur.
 */
@Entity
@Table(name = "randevu_hareketleri")
public class RandevuHareketi extends TemelVarlik {

    /** Sistemin kendi yaptığı geçişlerde (zaman aşımı, otomatik tamamlama) görünen ad. */
    public static final String SISTEM = "Sistem";

    @ManyToOne(optional = false)
    private Randevu randevu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RandevuDurumu durum;

    @Column(nullable = false)
    private LocalDateTime zaman;

    @Column(nullable = false, length = 120)
    private String yapan;

    @Column(length = 500)
    private String aciklama;

    protected RandevuHareketi() {
    }

    RandevuHareketi(Randevu randevu, RandevuDurumu durum, LocalDateTime zaman, String yapan, String aciklama) {
        this.randevu = randevu;
        this.durum = durum;
        this.zaman = zaman;
        this.yapan = yapan;
        this.aciklama = aciklama;
    }

    public boolean isSistemTarafindan() {
        return SISTEM.equals(yapan);
    }

    public RandevuDurumu getDurum() {
        return durum;
    }

    public LocalDateTime getZaman() {
        return zaman;
    }

    public String getYapan() {
        return yapan;
    }

    public String getAciklama() {
        return aciklama;
    }
}
