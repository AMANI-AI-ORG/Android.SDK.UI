package ai.amani.sdk.presentation_v2.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses a hex color string (as delivered by the server GeneralConfigs) into a
 * Compose [Color]. Supports `#RGB`, `#RRGGBB` and `#AARRGGBB` — with or without the
 * leading `#`. Returns null when the value is blank or malformed, so callers can
 * fall back to a static default.
 *
 * All V2 brand colors arrive as hex strings from config, so every screen ultimately
 * resolves its colors through this parser via [amaniV2PaletteFromHex].
 */
fun String?.toAmaniColorOrNull(): Color? {
    val raw = this?.trim()?.removePrefix("#")?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        when (raw.length) {
            3 -> { // #RGB -> #RRGGBB
                val (r, g, b) = Triple(raw[0], raw[1], raw[2])
                Color(0xFF000000L or "$r$r$g$g$b$b".toLong(16))
            }
            6 -> Color(0xFF000000L or raw.toLong(16))   // #RRGGBB
            8 -> Color(raw.toLong(16))                   // #AARRGGBB
            else -> null
        }
    }.getOrNull()
}
