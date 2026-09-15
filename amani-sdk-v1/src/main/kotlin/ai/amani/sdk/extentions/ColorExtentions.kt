package ai.amani.sdk.extentions

import android.graphics.Color

/**
 * Parses a config colour, treating "unconfigured" as absent.
 *
 * The core SDK defaults every colour in `GeneralConfigs` to fully transparent rather than to a
 * brand colour nobody chose. Painting that value would hide the view it is applied to, so a
 * transparent (or unparseable) value returns null and the caller keeps its own colour.
 */
fun String?.parseVisibleColorOrNull(): Int? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val parsed = runCatching { Color.parseColor(value) }.getOrNull() ?: return null
    return parsed.takeIf { Color.alpha(it) > 0 }
}
