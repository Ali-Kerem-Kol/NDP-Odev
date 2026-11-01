package com.klinik.randevu.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC ayarları: pasif hesap denetimini tüm isteklere ekler.
 */
@Configuration
public class WebYapilandirmasi implements WebMvcConfigurer {

    private final PasifHesapKontrolu pasifHesapKontrolu;

    public WebYapilandirmasi(PasifHesapKontrolu pasifHesapKontrolu) {
        this.pasifHesapKontrolu = pasifHesapKontrolu;
    }

    @Override
    public void addInterceptors(InterceptorRegistry kayit) {
        kayit.addInterceptor(pasifHesapKontrolu)
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/giris", "/error");
    }
}
