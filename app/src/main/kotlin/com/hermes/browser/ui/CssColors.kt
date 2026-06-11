package com.hermes.browser.ui

/**
 * Recognizes color tokens in CSS text — hex (#rgb/#rgba/#rrggbb/#rrggbbaa), rgb()/rgba(), and the
 * common CSS named colors — so the editor can show a swatch next to each. Parsing yields an opaque
 * ARGB int (alpha dropped, since the picker edits RGB).
 */
object CssColors {
    private val tokenRe = Regex("#[0-9a-fA-F]{3,8}|rgba?\\([^)]*\\)|[a-zA-Z]+", RegexOption.IGNORE_CASE)

    /** (token, opaque ARGB int, start offset) for every color in [css]. */
    fun findAll(css: String): List<Triple<String, Int, Int>> =
        tokenRe.findAll(css).mapNotNull { m ->
            val c = parse(m.value) ?: return@mapNotNull null
            Triple(m.value, c, m.range.first)
        }.toList()

    fun parse(token: String): Int? {
        val t = token.trim()
        return when {
            t.startsWith("#") -> parseHex(t)
            t.startsWith("rgb", ignoreCase = true) -> parseRgb(t)
            else -> named[t.lowercase()]
        }
    }

    private fun parseHex(t: String): Int? {
        val h = t.removePrefix("#")
        // Normalize to AARRGGBB (CSS hex puts alpha LAST: #rgba / #rrggbbaa).
        val argb = when (h.length) {
            3 -> "FF" + h.map { "$it$it" }.joinToString("")                       // rgb
            4 -> { val e = h.map { "$it$it" }.joinToString(""); e.substring(6) + e.substring(0, 6) } // rgba -> aarrggbb
            6 -> "FF$h"                                                            // rrggbb
            8 -> h.substring(6) + h.substring(0, 6)                                // rrggbbaa -> aarrggbb
            else -> return null
        }
        return runCatching { argb.toLong(16).toInt() }.getOrNull()
    }

    private fun parseRgb(t: String): Int? {
        val parts = t.substringAfter('(').substringBefore(')').split(',').map { it.trim() }
        if (parts.size < 3) return null
        val r = parts[0].toFloatOrNull()?.toInt()?.coerceIn(0, 255) ?: return null
        val g = parts[1].toFloatOrNull()?.toInt()?.coerceIn(0, 255) ?: return null
        val b = parts[2].toFloatOrNull()?.toInt()?.coerceIn(0, 255) ?: return null
        val a = if (parts.size >= 4) ((parts[3].toFloatOrNull() ?: 1f) * 255).toInt().coerceIn(0, 255) else 255
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    // Common CSS named colors (opaque ARGB). Not exhaustive, but covers what people actually type.
    private val named: Map<String, Int> = mapOf(
        "black" to 0xFF000000.toInt(), "white" to 0xFFFFFFFF.toInt(), "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF008000.toInt(), "blue" to 0xFF0000FF.toInt(), "yellow" to 0xFFFFFF00.toInt(),
        "cyan" to 0xFF00FFFF.toInt(), "aqua" to 0xFF00FFFF.toInt(), "magenta" to 0xFFFF00FF.toInt(),
        "fuchsia" to 0xFFFF00FF.toInt(), "gray" to 0xFF808080.toInt(), "grey" to 0xFF808080.toInt(),
        "silver" to 0xFFC0C0C0.toInt(), "maroon" to 0xFF800000.toInt(), "olive" to 0xFF808000.toInt(),
        "lime" to 0xFF00FF00.toInt(), "teal" to 0xFF008080.toInt(), "navy" to 0xFF000080.toInt(),
        "purple" to 0xFF800080.toInt(), "orange" to 0xFFFFA500.toInt(), "pink" to 0xFFFFC0CB.toInt(),
        "hotpink" to 0xFFFF69B4.toInt(), "deeppink" to 0xFFFF1493.toInt(), "lightpink" to 0xFFFFB6C1.toInt(),
        "brown" to 0xFFA52A2A.toInt(), "gold" to 0xFFFFD700.toInt(), "beige" to 0xFFF5F5DC.toInt(),
        "tan" to 0xFFD2B48C.toInt(), "coral" to 0xFFFF7F50.toInt(), "salmon" to 0xFFFA8072.toInt(),
        "crimson" to 0xFFDC143C.toInt(), "khaki" to 0xFFF0E68C.toInt(), "indigo" to 0xFF4B0082.toInt(),
        "violet" to 0xFFEE82EE.toInt(), "turquoise" to 0xFF40E0D0.toInt(), "lavender" to 0xFFE6E6FA.toInt(),
        "plum" to 0xFFDDA0DD.toInt(), "orchid" to 0xFFDA70D6.toInt(), "tomato" to 0xFFFF6347.toInt(),
        "skyblue" to 0xFF87CEEB.toInt(), "lightblue" to 0xFFADD8E6.toInt(), "steelblue" to 0xFF4682B4.toInt(),
        "royalblue" to 0xFF4169E1.toInt(), "dodgerblue" to 0xFF1E90FF.toInt(), "darkblue" to 0xFF00008B.toInt(),
        "midnightblue" to 0xFF191970.toInt(), "cornflowerblue" to 0xFF6495ED.toInt(),
        "lightgreen" to 0xFF90EE90.toInt(), "darkgreen" to 0xFF006400.toInt(), "forestgreen" to 0xFF228B22.toInt(),
        "seagreen" to 0xFF2E8B57.toInt(), "limegreen" to 0xFF32CD32.toInt(), "olivedrab" to 0xFF6B8E23.toInt(),
        "lightyellow" to 0xFFFFFFE0.toInt(), "ivory" to 0xFFFFFFF0.toInt(), "snow" to 0xFFFFFAFA.toInt(),
        "azure" to 0xFFF0FFFF.toInt(), "mintcream" to 0xFFF5FFFA.toInt(), "ghostwhite" to 0xFFF8F8FF.toInt(),
        "whitesmoke" to 0xFFF5F5F5.toInt(), "gainsboro" to 0xFFDCDCDC.toInt(), "lightgray" to 0xFFD3D3D3.toInt(),
        "lightgrey" to 0xFFD3D3D3.toInt(), "darkgray" to 0xFFA9A9A9.toInt(), "darkgrey" to 0xFFA9A9A9.toInt(),
        "dimgray" to 0xFF696969.toInt(), "dimgrey" to 0xFF696969.toInt(), "slategray" to 0xFF708090.toInt(),
        "slategrey" to 0xFF708090.toInt(), "darkslategray" to 0xFF2F4F4F.toInt(), "chocolate" to 0xFFD2691E.toInt(),
        "sienna" to 0xFFA0522D.toInt(), "peru" to 0xFFCD853F.toInt(), "goldenrod" to 0xFFDAA520.toInt(),
        "darkred" to 0xFF8B0000.toInt(), "firebrick" to 0xFFB22222.toInt(), "darkorange" to 0xFFFF8C00.toInt(),
        "rebeccapurple" to 0xFF663399.toInt(), "transparent" to 0x00000000,
    )
}
