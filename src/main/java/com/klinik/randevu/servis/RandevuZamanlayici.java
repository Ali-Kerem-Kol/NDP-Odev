package com.klinik.randevu.servis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/** Randevu durumlarını zamana göre güncel tutar; uygulama açılınca ve sonra her dakika çalışır. */
@Component
public class RandevuZamanlayici {

    private static final Logger LOG = LoggerFactory.getLogger(RandevuZamanlayici.class);

    private final RandevuServisi randevuServisi;
    private final Duration tamamlamaToleransi;

    public RandevuZamanlayici(RandevuServisi randevuServisi,
                              @Value("${randevu.otomatik-tamamlama-toleransi:PT2H}") Duration tamamlamaToleransi) {
        this.randevuServisi = randevuServisi;
        this.tamamlamaToleransi = tamamlamaToleransi;
    }

    @Scheduled(initialDelayString = "${randevu.zamanlayici-gecikmesi-ms:5000}",
            fixedDelayString = "${randevu.zamanlayici-araligi-ms:60000}")
    public void calistir() {
        RandevuServisi.ZamanIslemSonucu sonuc = randevuServisi.suresiDolanlariIsle(LocalDateTime.now(), tamamlamaToleransi);
        if (!sonuc.isBos()) {
            LOG.info("Randevu durumları güncellendi: {} talep zaman aşımına uğradı, {} randevu otomatik tamamlandı",
                    sonuc.zamanAsimi(), sonuc.otomatikTamamlanan());
        }
    }
}
