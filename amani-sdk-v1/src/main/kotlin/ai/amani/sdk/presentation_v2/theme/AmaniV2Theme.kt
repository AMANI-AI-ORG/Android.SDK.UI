package ai.amani.sdk.presentation_v2.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Runtime palette for the V2 UI. Screens read colors from [LocalAmaniV2Palette]
 * instead of referencing [AmaniV2Colors] directly, so the live flow can override
 * brand colors coming from the server config.
 */
@Immutable
data class AmaniV2Palette(
    val accent: Color = AmaniV2Colors.Pink,
    val accentDark: Color = AmaniV2Colors.PinkDark,
    val accentSoft: Color = AmaniV2Colors.PinkSoft,
    val accentSofter: Color = AmaniV2Colors.PinkSofter,
    val accentCoral: Color = AmaniV2Colors.Coral,
    val ink: Color = AmaniV2Colors.Ink,
    val inkSoft: Color = AmaniV2Colors.InkSoft,
    val inkMuted: Color = AmaniV2Colors.InkMuted,
    val inkLight: Color = AmaniV2Colors.InkLight,
    val background: Color = AmaniV2Colors.Bg,
    val backgroundWarm: Color = AmaniV2Colors.BgWarm,
    val border: Color = AmaniV2Colors.Border,
    val success: Color = AmaniV2Colors.Success,
    val danger: Color = AmaniV2Colors.Danger,
    val dangerBg: Color = AmaniV2Colors.DangerBg,
    val surface: Color = AmaniV2Colors.White,
)

/**
 * Builds an [AmaniV2Palette] from server-config hex strings, falling back to the static
 * defaults for any value that is null/blank/malformed. Each parameter is an optional hex
 * string (`#RRGGBB` / `#AARRGGBB`); see [toAmaniColorOrNull].
 */
fun amaniV2PaletteFromHex(
    accent: String? = null,
    accentDark: String? = null,
    accentSoft: String? = null,
    accentSofter: String? = null,
    ink: String? = null,
    inkMuted: String? = null,
    inkLight: String? = null,
    background: String? = null,
    backgroundWarm: String? = null,
    surface: String? = null,
    success: String? = null,
    danger: String? = null,
): AmaniV2Palette {
    val d = AmaniV2Palette()
    return d.copy(
        accent = accent.toAmaniColorOrNull() ?: d.accent,
        accentDark = accentDark.toAmaniColorOrNull() ?: d.accentDark,
        accentSoft = accentSoft.toAmaniColorOrNull() ?: d.accentSoft,
        accentSofter = accentSofter.toAmaniColorOrNull() ?: d.accentSofter,
        ink = ink.toAmaniColorOrNull() ?: d.ink,
        inkMuted = inkMuted.toAmaniColorOrNull() ?: d.inkMuted,
        inkLight = inkLight.toAmaniColorOrNull() ?: d.inkLight,
        background = background.toAmaniColorOrNull() ?: d.background,
        backgroundWarm = backgroundWarm.toAmaniColorOrNull() ?: d.backgroundWarm,
        surface = surface.toAmaniColorOrNull() ?: d.surface,
        success = success.toAmaniColorOrNull() ?: d.success,
        danger = danger.toAmaniColorOrNull() ?: d.danger,
    )
}

val LocalAmaniV2Palette: ProvidableCompositionLocal<AmaniV2Palette> =
    staticCompositionLocalOf { AmaniV2Palette() }

/**
 * Wraps V2 content with the resolved [palette]. Pass an overridden palette built
 * from the server GeneralConfigs in the live flow; omit it (default) for previews.
 */
@Composable
fun AmaniV2Theme(
    palette: AmaniV2Palette = AmaniV2Palette(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAmaniV2Palette provides palette,
        content = content
    )
}

/** Convenience accessor: `AmaniV2Theme.palette.accent`. */
object AmaniV2Theme {
    val palette: AmaniV2Palette
        @Composable
        get() = LocalAmaniV2Palette.current
}
