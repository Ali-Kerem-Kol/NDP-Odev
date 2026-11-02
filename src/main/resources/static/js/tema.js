/* Sayfa çizilmeden önce kayıtlı temayı uygular (beyaz yanıp sönmeyi önler). */
(function () {
  try {
    var kayitli = localStorage.getItem("medora-tema");
    if (kayitli === "acik" || kayitli === "koyu") {
      document.documentElement.setAttribute("data-tema", kayitli);
    }
  } catch (e) {
    /* depolama kapalıysa sistem teması kullanılır */
  }
})();
