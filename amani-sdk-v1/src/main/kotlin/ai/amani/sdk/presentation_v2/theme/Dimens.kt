package ai.amani.sdk.presentation_v2.theme

import androidx.compose.ui.unit.dp

/** Shared spacing / radius tokens for the V2 UI. */
object AmaniV2Dimens {
    val screenPadding = 20.dp
    val topInset = 44.dp

    val cardRadius = 18.dp
    val buttonRadius = 16.dp
    val pillRadius = 999.dp
    val fieldRadius = 12.dp
    val iconButtonRadius = 12.dp

    // Base height; scaled per window size class at call sites via .scaled().
    val buttonHeight = 54.dp
    val iconButtonSize = 36.dp

    val gapXs = 6.dp
    val gapSm = 10.dp
    val gapMd = 14.dp
    val gapLg = 20.dp
}
