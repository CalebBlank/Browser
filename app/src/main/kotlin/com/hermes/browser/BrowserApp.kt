package com.hermes.browser

import android.app.Application
import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtension

class BrowserApp : Application() {

    // The bundled user-CSS WebExtension (assets/usercss/), resolved once ensureBuiltIn completes.
    // Sessions read this to set their per-session message delegate (CSS injection).
    @Volatile
    var userCssExtension: WebExtension? = null
        private set

    // Shared GeckoRuntime — exactly one per process (GeckoRuntime.create throws if called twice),
    // shared by every BrowserTabActivity's GeckoSession. Lazy so it isn't started until the first
    // tab actually needs it (Phase 1+); the WebView path, still active until the engine swap, never
    // touches it. Configure tracking protection / remote debugging / etc. on the settings builder later.
    val geckoRuntime: GeckoRuntime by lazy {
        GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build()).also { rt ->
            // Install the bundled user-CSS extension (content script injects per-domain CSS).
            rt.webExtensionController
                .ensureBuiltIn("resource://android/assets/usercss/", "usercss@hermes.browser")
                .accept(
                    { ext -> userCssExtension = ext },
                    { e -> android.util.Log.w("USERCSS", "ensureBuiltIn failed", e) }
                )
        }
    }

    companion object {
        fun runtime(context: Context): GeckoRuntime =
            (context.applicationContext as BrowserApp).geckoRuntime

        fun userCssExtension(context: Context): WebExtension? =
            (context.applicationContext as BrowserApp).userCssExtension
    }
}
