/* Medora arayüz davranışları: tema, üst menü, hesap menüsü, seçim kutusu, diyaloglar, form durumu. */
(function () {
  "use strict";

  var kok = document.documentElement;

  /* ---------- Tema ---------- */

  function etkinTema() {
    var secili = kok.getAttribute("data-tema");
    if (secili) {
      return secili;
    }
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "koyu" : "acik";
  }

  function temaDugmeleriniGuncelle() {
    var koyu = etkinTema() === "koyu";
    document.querySelectorAll("[data-tema-degistir]").forEach(function (dugme) {
      dugme.setAttribute("aria-pressed", String(koyu));
      var etiket = dugme.querySelector("[data-tema-etiketi]");
      if (etiket) {
        etiket.textContent = koyu ? "Açık tema" : "Koyu tema";
      }
      var ikon = dugme.querySelector("use");
      if (ikon) {
        ikon.setAttribute("href", ikon.getAttribute("href").replace(/#.*$/, koyu ? "#gunes" : "#ay"));
      }
    });
  }

  document.querySelectorAll("[data-tema-degistir]").forEach(function (dugme) {
    dugme.addEventListener("click", function () {
      var yeni = etkinTema() === "koyu" ? "acik" : "koyu";
      kok.setAttribute("data-tema", yeni);
      try {
        localStorage.setItem("medora-tema", yeni);
      } catch (e) {
        /* yok say */
      }
      temaDugmeleriniGuncelle();
    });
  });
  temaDugmeleriniGuncelle();

  /* ---------- Üst menü (dar ekranda açılır panel) ---------- */

  var siteUst = document.querySelector("[data-site-ust]");
  var menuAc = document.querySelector("[data-site-menu-ac]");

  function menuyuAyarla(acik) {
    if (!siteUst || !menuAc) {
      return;
    }
    if (acik) {
      siteUst.setAttribute("data-menu-acik", "");
    } else {
      siteUst.removeAttribute("data-menu-acik");
    }
    menuAc.setAttribute("aria-expanded", String(acik));
  }

  if (menuAc) {
    menuAc.addEventListener("click", function () {
      menuyuAyarla(!siteUst.hasAttribute("data-menu-acik"));
    });
    // Genişleyen ekranda panel kendiliğinden kapanır
    window.matchMedia("(min-width: 64rem)").addEventListener("change", function () {
      menuyuAyarla(false);
    });
  }

  /* ---------- Hesap menüsü ---------- */

  var hesap = document.querySelector("[data-hesap]");
  var hesapAc = document.querySelector("[data-hesap-ac]");

  function hesapMenusunuAyarla(acik, odakGeriDonsun) {
    if (!hesap) {
      return;
    }
    if (acik) {
      hesap.setAttribute("data-acik", "");
      var ilk = hesap.querySelector(".hesap-menusu-oge");
      if (ilk) {
        ilk.focus();
      }
    } else {
      hesap.removeAttribute("data-acik");
      if (odakGeriDonsun) {
        hesapAc.focus();
      }
    }
    hesapAc.setAttribute("aria-expanded", String(acik));
  }

  if (hesap && hesapAc) {
    hesapAc.addEventListener("click", function () {
      hesapMenusunuAyarla(!hesap.hasAttribute("data-acik"), false);
    });
    document.addEventListener("click", function (olay) {
      if (hesap.hasAttribute("data-acik") && !hesap.contains(olay.target)) {
        hesapMenusunuAyarla(false, false);
      }
    });
    hesap.addEventListener("focusout", function (olay) {
      if (olay.relatedTarget && !hesap.contains(olay.relatedTarget)) {
        hesapMenusunuAyarla(false, false);
      }
    });
  }

  document.addEventListener("keydown", function (olay) {
    if (olay.key !== "Escape") {
      return;
    }
    if (hesap && hesap.hasAttribute("data-acik")) {
      hesapMenusunuAyarla(false, true);
    } else if (siteUst && siteUst.hasAttribute("data-menu-acik")) {
      menuyuAyarla(false);
      menuAc.focus();
    }
  });

  /* ---------- Düzenleme diyalogları (yetkiliye özel formlar) ---------- */

  document.querySelectorAll("[data-diyalog-ac]").forEach(function (acici) {
    acici.addEventListener("click", function () {
      var diyalog = document.getElementById(acici.getAttribute("data-diyalog-ac"));
      if (!diyalog || typeof diyalog.showModal !== "function") {
        return;
      }
      // Aynı diyalog birden çok kayıt için açılabilir (ör. her kurum kartındaki "Yönetici ekle")
      var alan = acici.getAttribute("data-diyalog-alan");
      if (alan) {
        diyalog.querySelector("[name='" + alan + "']").value = acici.getAttribute("data-diyalog-deger");
      }
      var ek = diyalog.querySelector("[data-diyalog-ek]");
      if (ek && acici.hasAttribute("data-diyalog-ek")) {
        ek.textContent = acici.getAttribute("data-diyalog-ek");
      }
      menuyuAyarla(false);
      diyalog.showModal();
    });
  });

  document.querySelectorAll("dialog [data-diyalog-kapat]").forEach(function (dugme) {
    dugme.addEventListener("click", function () {
      dugme.closest("dialog").close();
    });
  });

  // Perdeye (diyaloğun dışına) tıklayınca kapanır
  document.querySelectorAll("dialog.diyalog").forEach(function (diyalog) {
    diyalog.addEventListener("click", function (olay) {
      if (olay.target === diyalog) {
        diyalog.close();
      }
    });
  });

  /* ---------- Bildirim kapatma ---------- */

  document.querySelectorAll("[data-bildirim-kapat]").forEach(function (dugme) {
    dugme.addEventListener("click", function () {
      var bildirim = dugme.closest(".bildirim");
      if (bildirim) {
        bildirim.remove();
      }
    });
  });

  /* ---------- Seçim kutusu (native select yerine, ARIA listbox) ---------- */

  function secimKur(kapsayici) {
    var girdi = kapsayici.querySelector("input[type=hidden]");
    var tetik = kapsayici.querySelector(".secim-tetik");
    var degerYazisi = tetik.querySelector(".secim-deger");
    var liste = kapsayici.querySelector(".secim-liste");
    var secenekler = Array.prototype.slice.call(liste.querySelectorAll("[role=option]"));
    var yerTutucu = degerYazisi.getAttribute("data-yer-tutucu") || degerYazisi.textContent;
    var aramaMetni = "";
    var aramaZamani = 0;

    function goster(secenek) {
      secenekler.forEach(function (s) {
        s.setAttribute("aria-selected", String(s === secenek));
      });
      if (secenek) {
        degerYazisi.textContent = secenek.getAttribute("data-etiket") || secenek.textContent.trim();
        degerYazisi.removeAttribute("data-bos");
      } else {
        degerYazisi.textContent = yerTutucu;
        degerYazisi.setAttribute("data-bos", "");
      }
    }

    function ac(acik) {
      tetik.setAttribute("aria-expanded", String(acik));
      liste.hidden = !acik;
      if (acik) {
        var secili = secenekler.filter(function (s) {
          return s.getAttribute("aria-selected") === "true";
        })[0] || secenekler[0];
        if (secili) {
          secili.focus();
        }
      }
    }

    function sec(secenek) {
      girdi.value = secenek.getAttribute("data-deger");
      goster(secenek);
      tetik.removeAttribute("aria-invalid");
      var hata = kapsayici.parentElement.querySelector("[data-secim-hata]");
      if (hata) {
        hata.hidden = true;
      }
      ac(false);
      tetik.focus();
      girdi.dispatchEvent(new Event("change", { bubbles: true }));
    }

    var baslangic = secenekler.filter(function (s) {
      return s.getAttribute("data-deger") === girdi.value && girdi.value !== "";
    })[0];
    goster(baslangic || null);

    tetik.addEventListener("click", function () {
      ac(tetik.getAttribute("aria-expanded") !== "true");
    });
    tetik.addEventListener("keydown", function (olay) {
      if (olay.key === "ArrowDown" || olay.key === "ArrowUp" || olay.key === "Enter" || olay.key === " ") {
        olay.preventDefault();
        ac(true);
      }
    });

    secenekler.forEach(function (secenek, i) {
      secenek.tabIndex = -1;
      secenek.addEventListener("click", function () {
        sec(secenek);
      });
      secenek.addEventListener("keydown", function (olay) {
        var hedef = null;
        if (olay.key === "ArrowDown") {
          hedef = secenekler[Math.min(i + 1, secenekler.length - 1)];
        } else if (olay.key === "ArrowUp") {
          hedef = secenekler[Math.max(i - 1, 0)];
        } else if (olay.key === "Home") {
          hedef = secenekler[0];
        } else if (olay.key === "End") {
          hedef = secenekler[secenekler.length - 1];
        } else if (olay.key === "Enter" || olay.key === " ") {
          olay.preventDefault();
          sec(secenek);
          return;
        } else if (olay.key === "Escape" || olay.key === "Tab") {
          if (olay.key === "Escape") {
            olay.preventDefault();
          }
          ac(false);
          tetik.focus();
          return;
        } else if (olay.key.length === 1) {
          var simdi = Date.now();
          aramaMetni = simdi - aramaZamani > 600 ? olay.key : aramaMetni + olay.key;
          aramaZamani = simdi;
          var aranan = aramaMetni.toLocaleLowerCase("tr");
          hedef = secenekler.filter(function (s) {
            return s.textContent.trim().toLocaleLowerCase("tr").indexOf(aranan) === 0;
          })[0] || null;
        }
        if (hedef) {
          olay.preventDefault();
          hedef.focus();
        }
      });
    });

    document.addEventListener("click", function (olay) {
      if (!kapsayici.contains(olay.target) && tetik.getAttribute("aria-expanded") === "true") {
        ac(false);
      }
    });
  }

  document.querySelectorAll("[data-secim]").forEach(secimKur);

  /* ---------- Onay diyaloğu ---------- */

  var diyalog = document.getElementById("onay-diyalogu");
  var bekleyenForm = null;

  function diyalogAc(form) {
    if (!diyalog || typeof diyalog.showModal !== "function") {
      return false;
    }
    bekleyenForm = form;
    diyalog.querySelector("[data-diyalog-baslik]").textContent = form.getAttribute("data-onay-baslik");
    diyalog.querySelector("[data-diyalog-metin]").textContent = form.getAttribute("data-onay-metni") || "";
    var onayDugmesi = diyalog.querySelector("[data-diyalog-onayla]");
    onayDugmesi.textContent = form.getAttribute("data-onay-dugmesi") || "Onayla";
    onayDugmesi.className = "dugme " + (form.hasAttribute("data-onay-tehlikeli") ? "dugme-tehlike" : "dugme-birincil");
    var nedenAlani = diyalog.querySelector("[data-diyalog-neden]");
    var nedenGirdisi = nedenAlani.querySelector("textarea");
    nedenAlani.hidden = !form.hasAttribute("data-onay-neden");
    nedenGirdisi.value = "";
    diyalog.showModal();
    (nedenAlani.hidden ? diyalog.querySelector("[data-diyalog-vazgec]") : nedenGirdisi).focus();
    return true;
  }

  if (diyalog) {
    diyalog.querySelector("[data-diyalog-vazgec]").addEventListener("click", function () {
      diyalog.close();
      bekleyenForm = null;
    });
    diyalog.querySelector("[data-diyalog-onayla]").addEventListener("click", function () {
      if (!bekleyenForm) {
        return;
      }
      var neden = bekleyenForm.querySelector("input[name=neden]");
      if (neden) {
        neden.value = diyalog.querySelector("[data-diyalog-neden] textarea").value;
      }
      var form = bekleyenForm;
      bekleyenForm = null;
      diyalog.close();
      mesgulYap(form);
      form.submit();
    });
  }

  /* ---------- Form gönderimi: zorunlu seçimler ve meşgul durumu ---------- */

  function mesgulYap(form) {
    var dugme = form.querySelector("button[type=submit], button:not([type])");
    if (dugme) {
      dugme.setAttribute("aria-busy", "true");
    }
    form.setAttribute("data-gonderiliyor", "");
  }

  document.querySelectorAll("form").forEach(function (form) {
    form.addEventListener("submit", function (olay) {
      if (form.hasAttribute("data-gonderiliyor")) {
        olay.preventDefault();
        return;
      }
      var eksik = null;
      form.querySelectorAll("[data-secim][data-zorunlu]").forEach(function (kapsayici) {
        var girdi = kapsayici.querySelector("input[type=hidden]");
        if (!girdi.value) {
          var tetik = kapsayici.querySelector(".secim-tetik");
          tetik.setAttribute("aria-invalid", "true");
          var hata = kapsayici.parentElement.querySelector("[data-secim-hata]");
          if (hata) {
            hata.hidden = false;
          }
          eksik = eksik || tetik;
        }
      });
      if (eksik) {
        olay.preventDefault();
        eksik.focus();
        return;
      }
      if (form.hasAttribute("data-onay-baslik")) {
        if (diyalogAc(form)) {
          olay.preventDefault();
        }
        return;
      }
      mesgulYap(form);
    });
  });

  /* Geri tuşuyla dönülen sayfada düğmeler meşgul kalmasın */
  window.addEventListener("pageshow", function () {
    document.querySelectorAll("form[data-gonderiliyor]").forEach(function (form) {
      form.removeAttribute("data-gonderiliyor");
      form.querySelectorAll("[aria-busy]").forEach(function (d) {
        d.removeAttribute("aria-busy");
      });
    });
  });
})();
