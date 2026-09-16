package ai.amani.sdk.presentation_v2.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Corner radius of every config-styled surface (buttons, fields, step cards), read from
 * `GeneralConfigs.buttonRadiusAndroid`.
 */
@Composable
fun configCornerRadius(): Dp {
    val radiusPx = AmaniV2Theme.palette.buttonRadius
    return with(LocalDensity.current) { radiusPx.toDp() }
}

/**
 * Rounded shape that honours the configured radius and only reduces it when the surface is too
 * small to take it.
 *
 */
class CappedCornerShape(private val radius: Dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val requested = with(density) { radius.toPx() }
        val largestPossible = size.minDimension / 2f
        val effective = requested.coerceIn(0f, largestPossible)
        return Outline.Rounded(RoundRect(size.toRect(), CornerRadius(effective, effective)))
    }
}
