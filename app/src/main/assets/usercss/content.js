// Per-domain user CSS injector. Connects a native port to the host app, asks for this page's CSS,
// and injects it as a <style>. The app can push updated CSS over the same port for live edits.
(function () {
  function apply(css) {
    var el = document.getElementById("__hermes_usercss");
    if (css == null || css === "") {
      if (el) el.parentNode && el.parentNode.removeChild(el);
      return;
    }
    if (!el) {
      el = document.createElement("style");
      el.id = "__hermes_usercss";
      (document.head || document.documentElement).appendChild(el);
    }
    el.textContent = css;
  }

  try {
    var port = browser.runtime.connectNative("browser");
    port.onMessage.addListener(function (msg) {
      if (msg && Object.prototype.hasOwnProperty.call(msg, "css")) apply(msg.css);
    });
    // Ask the app what CSS (if any) applies to this page.
    port.postMessage({ url: location.href });
  } catch (e) {
    // No native port (extension not fully wired) — nothing to inject.
  }
})();
