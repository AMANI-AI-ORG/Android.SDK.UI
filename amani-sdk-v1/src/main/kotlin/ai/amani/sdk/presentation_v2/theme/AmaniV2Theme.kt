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
    val accentSoft: Color = AmaniV2Colors.PinkSoft,
    val accentSofter: Color = AmaniV2Colors.PinkSofter,
    val ink: Color = AmaniV2Colors.Ink,
    val inkMuted: Color = AmaniV2Colors.InkMuted,
    val inkLight: Color = AmaniV2Colors.InkLight,
    val background: Color = AmaniV2Colors.Bg,
    val backgroundWarm: Color = AmaniV2Colors.BgWarm,
    val border: Color = AmaniV2Colors.Border,
    val success: Color = AmaniV2Colors.Success,
    val danger: Color = AmaniV2Colors.Danger,
    val surface: Color = AmaniV2Colors.White,
    // ── GeneralConfigs-mapped chrome & control colors ────────────────────────────
    // Defaults deliberately equal the values the HTML design used implicitly, so
    // previews and configs without these fields look identical to before.
    /** Header/toolbar zone background (GeneralConfigs.topBarBackground); also the status bar. */
    val topBar: Color = AmaniV2Colors.Bg,
    /** Header title + back icon color (GeneralConfigs.topBarFontColor). */
    val topBarFont: Color = AmaniV2Colors.Ink,
    /** Primary button label/icon (GeneralConfigs.primaryButtonTextColor). */
    val primaryButtonText: Color = AmaniV2Colors.White,
    /** Primary button border (GeneralConfigs.primaryButtonBorderColor). Transparent = none. */
    val primaryButtonBorder: Color = Color.Transparent,
    /** Secondary button fill (GeneralConfigs.secondaryButtonBackgroundColor). */
    val secondaryButtonBackground: Color = AmaniV2Colors.White,
    /** Secondary button label/icon (GeneralConfigs.secondaryButtonTextColor). */
    val secondaryButtonText: Color = AmaniV2Colors.Ink,
    /** Secondary button border (GeneralConfigs.secondaryButtonBorderColor). */
    val secondaryButtonBorder: Color = AmaniV2Colors.Border,
    /** Spinner/loader color (GeneralConfigs.loaderColor). */
    val loader: Color = AmaniV2Colors.Pink,
)

/**
 * Builds an [AmaniV2Palette] from server-config hex strings, falling back to the static
 * defaults for any value that is null/blank/malformed. Each parameter is an optional hex
 * string (`#RRGGBB` / `#AARRGGBB`); see [toAmaniColorOrNull].
 */
fun amaniV2PaletteFromHex(
    accent: String? = null,
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
    topBar: String? = null,
    topBarFont: String? = null,
    primaryButtonText: String? = null,
    primaryButtonBorder: String? = null,
    secondaryButtonBackground: String? = null,
    secondaryButtonText: String? = null,
    secondaryButtonBorder: String? = null,
    loader: String? = null,
): AmaniV2Palette {
    val d = AmaniV2Palette()
    return d.copy(
        accent = accent.toAmaniColorOrNull() ?: d.accent,
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
        // Top bar falls back to the (possibly config-driven) app background rather than
        // the static default, so a config that sets only appBackground still gets a
        // seamless header + status bar.
        topBar = topBar.toAmaniColorOrNull() ?: background.toAmaniColorOrNull() ?: d.topBar,
        topBarFont = topBarFont.toAmaniColorOrNull() ?: ink.toAmaniColorOrNull() ?: d.topBarFont,
        primaryButtonText = primaryButtonText.toAmaniColorOrNull() ?: d.primaryButtonText,
        primaryButtonBorder = primaryButtonBorder.toAmaniColorOrNull() ?: d.primaryButtonBorder,
        secondaryButtonBackground = secondaryButtonBackground.toAmaniColorOrNull() ?: d.secondaryButtonBackground,
        secondaryButtonText = secondaryButtonText.toAmaniColorOrNull() ?: d.secondaryButtonText,
        secondaryButtonBorder = secondaryButtonBorder.toAmaniColorOrNull() ?: d.secondaryButtonBorder,
        // Loader falls back to the config accent so the spinner stays on-brand even when
        // loaderColor is absent.
        loader = loader.toAmaniColorOrNull() ?: accent.toAmaniColorOrNull() ?: d.loader,
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
