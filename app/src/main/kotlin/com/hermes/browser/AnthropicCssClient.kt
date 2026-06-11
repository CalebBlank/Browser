package com.hermes.browser

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Generates / edits CSS from a natural-language prompt via the Anthropic Messages API. The key+model
 * come from Deck ([DeckCredentials]). Output is constrained to raw CSS. Blocking — call off main.
 */
object AnthropicCssClient {
    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val VERSION = "2023-06-01"

    fun generate(
        apiKey: String,
        model: String,
        domain: String,
        currentCss: String,
        prompt: String
    ): Result<String> = runCatching {
        val system = "You write CSS user-styles injected into web pages on \"$domain\". " +
            "Output ONLY raw CSS — no markdown, no code fences, no commentary. Use robust selectors " +
            "and add !important where needed to override the site's own styles. " +
            "IMPORTANT: many sites paint the visible page background on a full-height wrapper/root " +
            "element, NOT on <body>. To change the background reliably, target html, body AND common " +
            "containers together, e.g.: html, body, #root, #__next, #app, [id*=\"app\"], " +
            "[class*=\"app\"], [class*=\"container\"], [class*=\"wrapper\"], main { background: … }. " +
            "When the user is refining, build on the existing CSS rather than discarding it."
        val userContent = buildString {
            if (currentCss.isNotBlank()) append("Existing CSS:\n").append(currentCss).append("\n\n")
            append("Request: ").append(prompt)
        }
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1500)
            .put("system", system)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", userContent)))
            .toString()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", VERSION)
        }
        val responseText = try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw RuntimeException("Claude error $code: ${parseError(text)}")
            text
        } finally {
            conn.disconnect()
        }

        val content = JSONObject(responseText).optJSONArray("content") ?: JSONArray()
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.optJSONObject(i) ?: continue
            if (block.optString("type") == "text") sb.append(block.optString("text"))
        }
        val css = stripFences(sb.toString().trim())
        if (css.isBlank()) throw RuntimeException("Claude returned no CSS.")
        css
    }

    private fun parseError(text: String): String = runCatching {
        JSONObject(text).optJSONObject("error")?.optString("message").orEmpty().ifBlank { text }
    }.getOrDefault(text).take(200)

    /** Strip a ```css ... ``` fence if the model added one despite instructions. */
    private fun stripFences(s: String): String {
        var t = s.trim()
        if (t.startsWith("```")) {
            t = t.substringAfter('\n', "").substringBeforeLast("```").trim()
        }
        return t
    }
}
