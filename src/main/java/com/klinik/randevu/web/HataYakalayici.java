package com.klinik.randevu.web;

import com.klinik.randevu.hata.IsKuraliIhlali;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * İş kuralı ihlallerini yakalar, mesajı bir sonraki sayfada gösterilmek üzere
 * saklar ve kullanıcıyı geldiği sayfaya geri gönderir.
 */
@ControllerAdvice
public class HataYakalayici {

    @ExceptionHandler(IsKuraliIhlali.class)
    public String isKuraliIhlali(IsKuraliIhlali hata, HttpServletRequest istek) {
        RequestContextUtils.getOutputFlashMap(istek).put("hata", hata.getMessage());
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }

    /** Aynı kaydı başka biri bu sırada değiştirdiyse (ör. doktor onaylarken hasta iptal etti). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String eszamanliGuncelleme(ObjectOptimisticLockingFailureException hata, HttpServletRequest istek) {
        RequestContextUtils.getOutputFlashMap(istek).put("hata",
                "Bu kayıt siz işlem yaparken başka biri tarafından güncellendi. Güncel hâli gösteriliyor, lütfen tekrar deneyin.");
        return "redirect:" + GeriDonus.oncekiSayfa(istek);
    }
}
