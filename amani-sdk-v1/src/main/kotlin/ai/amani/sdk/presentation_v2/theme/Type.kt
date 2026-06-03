package ai.amani.sdk.presentation_v2.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text styles for the V2 UI. System font family, weights 400/500/600. Colors are
 * applied at call sites from [AmaniV2Palette].
 */
object AmaniV2Type {
    private val family = FontFamily.Default

    /** Large screen title, e.g. "Let's get you verified". */
    val title = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 30.sp)

    /** Section heading, e.g. "Which document will you use?". */
    val heading = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp)

    /** Toolbar / header label. */
    val header = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** Primary body copy. */
    val body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp)

    /** Secondary / muted copy. */
    val bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)

    /** Row / card titles. */
    val rowTitle = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 15.sp)

    /** Captions, helper text. */
    val caption = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp)

    /** Tiny labels (step dot labels, badges). */
    val label = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 11.sp)

    /** Uppercase eyebrow above headings. */
    val eyebrow = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 10.sp)

    /** Button label. */
    val button = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 15.sp)
}
