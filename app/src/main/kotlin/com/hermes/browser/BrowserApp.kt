package com.hermes.browser

import android.app.Application
import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class BrowserApp : Application() {

    // Shared GeckoRuntime — exactly one per process (GeckoRuntime.create throws if called twice),
    // shared by every BrowserTabActivity's GeckoSession. Lazy so it isn't started until the first
    // tab actually needs it (Phase 1+); the WebView path, still active until the engine swap, never
    // touches it. Configure tracking protection / remote debugging / etc. on the settings builder later.
    val geckoRuntime: GeckoRuntime by lazy {
        GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build())
    }

    companion object {
        fun runtime(context: Context): GeckoRuntime =
            (context.applicationContext as BrowserApp).geckoRuntime
    }
}
