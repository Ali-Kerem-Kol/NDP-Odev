package com.klinik.randevu.servis;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Bir çalışanın tek bir gündeki boş başlangıç saatleri. */
public record GunlukMusaitlik(LocalDate tarih, List<LocalTime> saatler) {

    public boolean isBos() {
        return saatler.isEmpty();
    }
}
