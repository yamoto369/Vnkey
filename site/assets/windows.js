// Windows-only download actions.
(() => {
  const dl = (window.VK_DL || {}).windows || {};
  const icon = '<svg class="ic" aria-hidden="true"><use href="/assets/os.svg#windows"/></svg>';
  document.querySelectorAll("[data-cta]").forEach(a => {
    if (a.dataset.cta === "long") {
      a.innerHTML = icon + (dl.url ? "Download for Windows" : "Windows download");
      a.href = dl.url || "/download#windows";
      if (dl.url) a.setAttribute("download", "");
    } else {
      a.href = "/download#windows";
    }
  });
  const isWindows = /Windows/i.test(navigator.userAgent);
  if (isWindows) {
    document.querySelectorAll('.pcard[data-os="windows"]').forEach(c => c.classList.add("mine"));
  }
  if (isWindows && dl.arm && navigator.userAgentData?.getHighEntropyValues) {
    navigator.userAgentData.getHighEntropyValues(["architecture"]).then(v => {
      if (v.architecture === "arm") {
        document.querySelectorAll("[data-cta]").forEach(a => {
          if (a.href === new URL(dl.url, location.href).href) a.href = dl.arm;
        });
      }
    }).catch(() => {});
  }
})();
