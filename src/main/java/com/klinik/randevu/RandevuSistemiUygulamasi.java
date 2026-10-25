package com.klinik.randevu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Uygulamanın giriş noktası. Zamanlanmış görevler (randevu yaşam döngüsü) burada etkinleştirilir.
 */
@SpringBootApplication
@EnableScheduling
public class RandevuSistemiUygulamasi {

    public static void main(String[] args) {
        SpringApplication.run(RandevuSistemiUygulamasi.class, args);
    }
}
