package com.hermes.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves a site's favicon the robust way. Many modern sites (Vox Media — Polygon/The Verge — being
 * a prime example) serve NO `/favicon.ico` (it 404s) and instead declare the icon via
 * `<link rel="icon" href="...">` in the page `<head>`, often pointing at a CDN/build path. So we
 * parse the head first, then fall back to `/favicon.ico`.
 *
 * First-party only — we fetch the site's own pages/icons, never a third-party favicon service
 * (Google/DuckDuckGo), which would leak every domain the user visits. The site already knows you're
 * there. Bitmaps and genuine misses are cached per domain for the process lifetime.
 *
 * Limitation: only sees icons present in the server-rendered HTML. A few JS-only SPAs that inject the
 * `<link>` at runtime fall through to `/favicon.ico` or come up neutral — the eventual fix is the
 * planned WebExtension (custom-CSS/AI feature), which can read the live DOM.
 */
object Favicons {
    // A real Firefox/GeckoView UA — a bare Java UA gets 403'd by some CDNs.
    private const val UA = "Mozilla/5.0 (Android 14; Mobile; rv:151.0) Gecko/151.0 Firefox/151.0"
    private const val HEAD_CAP = 128 * 1024 // some sites bloat <head> with inline CSS above the links

    private val cache = ConcurrentHashMap<String, Bitmap>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    /** The per-domain cache key (host without a leading www.), or null if [pageUrl] has no host. */
    fun keyOf(pageUrl: String): String? =
        runCatching { URL(pageUrl).host?.removePrefix("www.") }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Already-resolved icon for a domain key (from [keyOf]), or null. Cheap, main-thread safe. */
    fun cached(domainKey: String): Bitmap? = cache[domainKey]

    /** Resolve the favicon for a page. BLOCKING — call off the main thread. */
    fun fetch(pageUrl: String): Bitmap? {
        val url = runCatching { URL(pageUrl) }.getOrNull() ?: return null
        val key = url.host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: return null
        cache[key]?.let { return it }
        if (key in misses) return null

        val head = fetchHead(pageUrl) // (html, finalUrlAfterRedirects) — null only on a network error
        val iconUrl = head?.let { pickIcon(it.first, it.second) }
        val bmp = (iconUrl?.let { decode(it) }) ?: decode("${url.protocol}://${url.host}/favicon.ico")

        if (bmp != null) {
            cache[key] = bmp
            return bmp
        }
        // Only remember a MISS when the site was actually reachable (its HTML loaded). A transient
        // timeout/IO blip must not permanently mark a domain that does have an icon as iconless.
        if (head != null) misses.add(key)
        return null
    }

    private fun fetchHead(pageUrl: String): Pair<String, String>? = runCatching {
        val conn = (URL(pageUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
            connectTimeout = 4000; readTimeout = 4000
        }
        val html = conn.inputStream.use { input ->
            val buf = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            while (buf.size() < HEAD_CAP) {
                val n = input.read(chunk)
                if (n < 0) break
                buf.write(chunk, 0, n)
            }
            buf.toString("UTF-8")
        }
        // Resolve relative icon hrefs against the FINAL url (after redirects), not the requested one.
        html to (conn.url?.toString() ?: pageUrl)
    }.getOrNull()

    private fun pickIcon(html: String, baseUrl: String): String? {
        val headEnd = html.indexOf("</head>", ignoreCase = true).let { if (it >= 0) it else html.length }
        val head = html.substring(0, headEnd)
        val relRe = Regex("rel\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val hrefRe = Regex("href\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val sizeRe = Regex("sizes\\s*=\\s*[\"'](\\d+)x\\d+[\"']", RegexOption.IGNORE_CASE)
        var bestHref: String? = null
        var bestScore = Int.MIN_VALUE
        for (m in Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(head)) {
            val tag = m.value
            val rel = relRe.find(tag)?.groupValues?.get(1)?.lowercase() ?: continue
            if (!rel.contains("icon")) continue
            val href = hrefRe.find(tag)?.groupValues?.get(1)
                ?.takeIf { it.isNotBlank() && !it.startsWith("data:") } ?: continue
            val size = sizeRe.find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 32
            // Prefer a moderate size (best near ~128, falling off above 256) — avoid grabbing a 16px
            // sliver or an oversized 512 banner.
            val score = if (size > 256) 256 - (size - 256) else size
            if (score > bestScore) { bestScore = score; bestHref = href }
        }
        return bestHref?.let { runCatching { URL(URL(baseUrl), it).toString() }.getOrNull() }
    }

    private fun decode(iconUrl: String): Bitmap? = runCatching {
        val conn = (URL(iconUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
            connectTimeout = 4000; readTimeout = 4000
        }
        conn.inputStream.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
