package ai.amani.sdk.presentation_v2.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Adaptive sizing for the V2 UI. The live window is bucketed into width / height size
 * classes and the UI behaves differently per bucket. Reads [LocalConfiguration] so it
 * recomposes on rotation / window-resize.
 *
 * Width breakpoints (four tiers):
 *   Small     < 360dp   → small / older phone
 *   Compact   360–599   → standard phone (the "default")
 *   Medium    600–839   → large landscape phone / unfolded foldable / small tablet
 *   Expanded  ≥ 840dp   → tablet / desktop / large foldable
 *
 * Height breakpoints (used only to damp very short windows):
 *   Compact  < 480dp   → phone, landscape (short window)
 *   Medium   480–899
 *   Expanded ≥ 900dp
 */
enum class AmaniV2WidthClass { Small, Compact, Medium, Expanded }
enum class AmaniV2HeightClass { Compact, Medium, Expanded }

/** Snapshot of the current window size classes plus the raw dp extents. */
data class AmaniV2WindowSize(
    val widthClass: AmaniV2WidthClass,
    val heightClass: AmaniV2HeightClass,
    val widthDp: Float,
    val heightDp: Float
) {
    val isCompactWidth: Boolean get() = widthClass == AmaniV2WidthClass.Small || widthClass == AmaniV2WidthClass.Compact
    val isExpandedWidth: Boolean get() = widthClass == AmaniV2WidthClass.Expanded
}

/** Reads the live window and classifies it into width / height size classes. */
@Composable
@ReadOnlyComposable
fun amaniV2WindowSize(): AmaniV2WindowSize {
    val config = LocalConfiguration.current
    val w = config.screenWidthDp.toFloat()
    val h = config.screenHeightDp.toFloat()
    val widthClass = when {
        w < 360f -> AmaniV2WidthClass.Small
        w < 600f -> AmaniV2WidthClass.Compact
        w < 840f -> AmaniV2WidthClass.Medium
        else -> AmaniV2WidthClass.Expanded
    }
    val heightClass = when {
        h < 480f -> AmaniV2HeightClass.Compact
        h < 900f -> AmaniV2HeightClass.Medium
        else -> AmaniV2HeightClass.Expanded
    }
    return AmaniV2WindowSize(widthClass, heightClass, w, h)
}

/**
 * Discrete scale step per width class. A very short window (height Compact) nudges the
 * factor back down so fixed-height controls don't overflow vertically.
 */
@Composable
@ReadOnlyComposable
fun amaniV2Scale(): Float {
    val size = amaniV2WindowSize()
    val base = when (size.widthClass) {
        AmaniV2WidthClass.Small -> 1.04f
        AmaniV2WidthClass.Compact -> 1.10f
        AmaniV2WidthClass.Medium -> 1.22f
        AmaniV2WidthClass.Expanded -> 1.35f
    }
    return if (size.heightClass == AmaniV2HeightClass.Compact) base * 0.9f else base
}

/** Scales a [Dp] base size for the current window size class. */
@Composable
@ReadOnlyComposable
fun Dp.scaled(): Dp = this * amaniV2Scale()

/** Scales a [TextUnit] (sp) base size for the current window size class. */
@Composable
@ReadOnlyComposable
fun TextUnit.scaled(): TextUnit = this * amaniV2Scale()

/**
 * Scales a [TextStyle]'s font size and line height for the current window size class, so
 * `style = AmaniV2Type.body.scaled()` grows text consistently with the buttons/icons. Leaves
 * unspecified metrics untouched.
 */
@Composable
@ReadOnlyComposable
fun TextStyle.scaled(): TextStyle {
    val s = amaniV2Scale()
    return copy(
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize * s else fontSize,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight * s else lineHeight
    )
}

/**
 * Max width the main content column should occupy. On phones (Small / Compact) it's unbounded
 * ([Dp.Unspecified] → fill width); on medium / expanded windows we cap it so the form stays a
 * comfortable reading measure and centers instead of stretching across the whole screen.
 */
@Composable
@ReadOnlyComposable
fun amaniV2ContentMaxWidth(): Dp = when (amaniV2WindowSize().widthClass) {
    AmaniV2WidthClass.Small, AmaniV2WidthClass.Compact -> Dp.Unspecified
    AmaniV2WidthClass.Medium -> 540.dp
    AmaniV2WidthClass.Expanded -> 620.dp
}
