package com.klinik.randevu.guvenlik;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Rol bazlı erişim: her rol yalnızca kendi panelinin adreslerine girebilir. */
@Configuration
@EnableWebSecurity
public class GuvenlikYapilandirmasi {

    @Bean
    public SecurityFilterChain guvenlikFiltresi(HttpSecurity http, KotuyeKullanimKorumasi koruma) throws Exception {
        http
                .addFilterBefore(new GirisKilidiFiltresi(koruma), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(yetki -> yetki
                        .requestMatchers("/", "/haberler", "/haberler/**", "/aydinlatma-metni", "/giris", "/kayit", "/css/**", "/js/**", "/img/**", "/error").permitAll()
                        .requestMatchers("/hasta/**").hasRole("HASTA")
                        .requestMatchers("/calisan/**").hasAnyRole("DOKTOR", "SAGLIK_PERSONELI")
                        .requestMatchers("/yonetici/**").hasRole("YONETICI")
                        .requestMatchers("/admin/**").hasRole("SISTEM_YONETICISI")
                        .requestMatchers("/icerik/**").hasAnyRole("YONETICI", "SISTEM_YONETICISI")
                        .anyRequest().authenticated())
                .formLogin(giris -> giris
                        .loginPage("/giris")
                        .loginProcessingUrl("/giris")
                        .usernameParameter("kimlik")
                        .passwordParameter("sifre")
                        .successHandler(new GirisSonucIsleyicileri.Basari(koruma))
                        .failureHandler(new GirisSonucIsleyicileri.Hata(koruma))
                        .permitAll())
                .logout(cikis -> cikis
                        .logoutUrl("/cikis")
                        .logoutSuccessUrl("/giris?cikis")
                        .permitAll())
                .headers(basliklar -> basliklar
                        .frameOptions(cerceve -> cerceve.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; style-src 'self' https://fonts.googleapis.com; "
                                        + "font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; "
                                        + "script-src 'self'; form-action 'self'; frame-ancestors 'none'")));
        return http.build();
    }

    @Bean
    public PasswordEncoder sifreleyici() {
        return new BCryptPasswordEncoder();
    }
}
