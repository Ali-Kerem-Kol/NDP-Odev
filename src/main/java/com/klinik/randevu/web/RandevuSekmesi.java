package com.klinik.randevu.web;

import com.klinik.randevu.model.randevu.Randevu;
import com.klinik.randevu.model.randevu.RandevuDurumu;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Personel ve yönetici randevu listelerindeki sekmeler; her sekme kendi süzgecini ve sırasını bilir. */
public enum RandevuSekmesi {

    BEKLEYEN("Onay bekleyen") {
        @Override
        boolean kapsar(Randevu r, LocalDateTime simdi) {
            return r.getDurum() == RandevuDurumu.BEKLEMEDE;
        }
    },
    YAKLASAN("Onaylı") {
        @Override
        boolean kapsar(Randevu r, LocalDateTime simdi) {
            return r.getDurum() == RandevuDurumu.ONAYLANDI && r.getBitis().isAfter(simdi);
        }
    },
    GECMIS("Geçmiş") {
        @Override
        boolean kapsar(Randevu r, LocalDateTime simdi) {
            return !BEKLEYEN.kapsar(r, simdi) && !YAKLASAN.kapsar(r, simdi);
        }

        @Override
        Comparator<Randevu> siralama() {
            return Comparator.comparing(Randevu::getBaslangic).reversed();
        }
    },
    TUMU("Tümü") {
        @Override
        boolean kapsar(Randevu r, LocalDateTime simdi) {
            return true;
        }

        @Override
        Comparator<Randevu> siralama() {
            return Comparator.comparing(Randevu::getBaslangic).reversed();
        }
    };

    private final String etiket;

    RandevuSekmesi(String etiket) {
        this.etiket = etiket;
    }

    abstract boolean kapsar(Randevu randevu, LocalDateTime simdi);

    Comparator<Randevu> siralama() {
        return Comparator.comparing(Randevu::getBaslangic);
    }

    public List<Randevu> suz(List<Randevu> randevular) {
        LocalDateTime simdi = LocalDateTime.now();
        return randevular.stream().filter(r -> kapsar(r, simdi)).sorted(siralama()).toList();
    }

    public long say(List<Randevu> randevular) {
        LocalDateTime simdi = LocalDateTime.now();
        return randevular.stream().filter(r -> kapsar(r, simdi)).count();
    }

    public String getEtiket() {
        return etiket;
    }
}
