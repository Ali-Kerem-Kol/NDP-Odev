package com.klinik.randevu.web;

import com.klinik.randevu.model.kurum.Bolum;
import com.klinik.randevu.model.kurum.HizmetTuru;
import com.klinik.randevu.model.kurum.Kurum;
import com.klinik.randevu.model.kurum.KurumTuru;
import com.klinik.randevu.model.kurum.Sube;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Şablonlardaki seçim kutularına ({@code fragments/sablon :: secim}) değer-etiket listeleri hazırlar. */
@Component("secenek")
public class SecenekYardimcisi {

    public record Secenek(String deger, String etiket) {
    }

    public List<Secenek> subeler(List<Sube> subeler) {
        return subeler.stream().map(s -> new Secenek(String.valueOf(s.getId()), s.getAd())).toList();
    }

    public List<Secenek> bolumler(List<Bolum> bolumler) {
        return bolumler.stream()
                .map(b -> new Secenek(String.valueOf(b.getId()), b.getSube().getAd() + " / " + b.getAd()))
                .toList();
    }

    public List<Secenek> kurumlar(List<Kurum> kurumlar) {
        return kurumlar.stream().map(k -> new Secenek(String.valueOf(k.getId()), k.getAd())).toList();
    }

    /** İçerik formu için: ilk seçenek genel (kurumsuz) duyurudur. */
    public List<Secenek> kurumlarVeGenel(List<Kurum> kurumlar) {
        List<Secenek> liste = new ArrayList<>();
        liste.add(new Secenek("", "Medora (genel duyuru)"));
        liste.addAll(kurumlar(kurumlar));
        return liste;
    }

    public List<Secenek> hizmetTurleri() {
        return Arrays.stream(HizmetTuru.values()).map(t -> new Secenek(t.name(), t.getEtiket())).toList();
    }

    public List<Secenek> kurumTurleri() {
        return Arrays.stream(KurumTuru.values()).map(t -> new Secenek(t.name(), t.getEtiket())).toList();
    }
}
