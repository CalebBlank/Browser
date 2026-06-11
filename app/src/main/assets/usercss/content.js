// Per-domain user CSS injector. Connects a native port to the host app, asks for this page's CSS,
// and injects it as a <style>. The app can push updated CSS over the same port for live edits.
// Also sends a compact element outline (tag#id.class tree) the app feeds to the AI so it can pick
// precise selectors (include/exclude real elements).
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

  // A compact tree of the page's elements (tags + ids + first few classes), capped in size.
  function outline() {
    var SKIP = { SCRIPT: 1, STYLE: 1, NOSCRIPT: 1, SVG: 1, PATH: 1, LINK: 1, META: 1, BR: 1, HR: 1, IMG: 1 };
    var lines = [];
    var count = 0;
    function walk(el, depth) {
      if (count > 400 || depth > 8 || !el.tagName || SKIP[el.tagName]) return;
      count++;
      var s = "";
      for (var i = 0; i < depth; i++) s += "  ";
      s += el.tagName.toLowerCase();
      if (el.id) s += "#" + el.id;
      if (el.classList && el.classList.length) {
        var cls = [];
        for (var j = 0; j < el.classList.length && j < 3; j++) cls.push(el.classList[j]);
        s += "." + cls.join(".");
      }
      lines.push(s);
      var kids = el.children || [];
      for (var k = 0; k < kids.length; k++) walk(kids[k], depth + 1);
    }
    if (document.body) walk(document.body, 0);
    return lines.join("\n").slice(0, 6000);
  }

  try {
    var port = browser.runtime.connectNative("browser");
    port.onMessage.addListener(function (msg) {
      if (msg && Object.prototype.hasOwnProperty.call(msg, "css")) apply(msg.css);
    });
    // Ask the app what CSS (if any) applies to this page.
    port.postMessage({ url: location.href });

    var sendContext = function () {
      try { port.postMessage({ context: outline() }); } catch (e) {}
    };
    if (document.readyState === "loading") {
      window.addEventListener("DOMContentLoaded", function () { setTimeout(sendContext, 300); });
    } else {
      setTimeout(sendContext, 300);
    }
    // Re-send once more after full load to catch late-rendered (SPA) content.
    window.addEventListener("load", function () { setTimeout(sendContext, 600); });
  } catch (e) {
    // No native port (extension not fully wired) — nothing to inject.
  }
})();
