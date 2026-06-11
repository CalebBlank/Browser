package com.hermes.browser

import android.content.Context
import android.net.Uri

/**
 * Reads the Claude credentials from the same-signed Deck app (content://com.hermes.deck.settings),
 * so the browser's custom-CSS AI reuses Deck's key instead of asking for a second one. Returns null
 * if Deck isn't installed, the provider is unreachable, or no key is set.
 */
object DeckCredentials {
    private val URI = Uri.parse("content://com.hermes.deck.settings")

    /** (apiKey, model) or null. */
    fun read(context: Context): Pair<String, String>? = runCatching {
        context.contentResolver.query(URI, null, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val key = c.getString(c.getColumnIndexOrThrow("claude_api_key")).orEmpty()
            val model = c.getString(c.getColumnIndexOrThrow("claude_model")).orEmpty()
                .ifBlank { "claude-opus-4-8" }
            if (key.isNotBlank()) key to model else null
        }
    }.getOrNull()
}
