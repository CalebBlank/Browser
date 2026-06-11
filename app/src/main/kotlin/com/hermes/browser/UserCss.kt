package com.hermes.browser

import android.content.Context

/**
 * Per-domain user CSS storage. Keyed by domain (host minus a leading www., via [Favicons.keyOf]) so
 * it matches the favicon/accent keying and the user's "this website" mental model. The bundled
 * usercss WebExtension's content script requests CSS for each page; the host app answers from here.
 */
object UserCss {
    private const val PREFS = "usercss"

    fun getCss(context: Context, domainKey: String?): String? {
        if (domainKey.isNullOrBlank()) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(domainKey, null)
    }

    /** CSS for a full page URL (resolves the domain key), or null if none saved. */
    fun getCssForUrl(context: Context, url: String): String? =
        getCss(context, Favicons.keyOf(url))

    /** Save (or clear, when blank) the CSS for a domain. */
    fun setCss(context: Context, domainKey: String, css: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (css.isBlank()) prefs.edit().remove(domainKey).apply()
        else prefs.edit().putString(domainKey, css).apply()
    }
}
