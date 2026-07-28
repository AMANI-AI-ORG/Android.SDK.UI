package ai.amani.sdk.presentation_v2.theme

import androidx.compose.ui.graphics.Color

/**
 * V2 UI design tokens. These are the DEFAULT palette: previews use them directly, and
 * the live theme falls back to them when the server [GeneralConfigs] do not provide an override.
 */
object AmaniV2Colors {
    val Pink = Color(0xFFDC2655)
    val PinkSoft = Color(0xFFFBEAF0)
    val PinkSofter = Color(0xFFFDF4F7)

    val Ink = Color(0xFF0F172A)
    val InkMuted = Color(0xFF64748B)
    val InkLight = Color(0xFF94A3B8)

    val Bg = Color(0xFFFAFAF7)
    val BgWarm = Color(0xFFF1EFE8)
    val Border = Color(0x140F172A) // rgba(15,23,42,0.08)

    val Success = Color(0xFF15803D)
    val Danger = Color(0xFFDC2626)

    val White = Color(0xFFFFFFFF)

    // Step / connector neutrals used by the dot progress indicator
    val DotIdle = BgWarm
    val DotIdleBorder = Color(0xFFD4D2C8)
    val ConnectorIdle = Color(0xFFE5E3D9)
}
