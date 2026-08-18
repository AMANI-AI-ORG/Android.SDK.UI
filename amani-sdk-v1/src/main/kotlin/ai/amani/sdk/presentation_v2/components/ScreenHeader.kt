package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Top header with back button and centered title, with optional dot progress
 * beneath it. A fixed-size spacer balances the back button on the trailing side
 * so the title stays optically centered.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    steps: List<DotStep>? = null,
    onBack: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    Column(modifier = modifier.fillMaxWidth()) {
        // Toolbar zone paints GeneralConfigs.topBarBackground (defaults to the app
        // background, keeping the previous seamless look) — the status bar is painted
        // the same color at the activity level so they read as one surface.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.topBar)
                .padding(
                    top = AmaniV2Dimens.topInset,
                    start = AmaniV2Dimens.screenPadding,
                    end = AmaniV2Dimens.screenPadding
                )
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                // The back button keeps the fixed radius; every other button follows config.
                cornerRadius = AmaniV2Dimens.iconButtonRadius,
                // Mirrors iOS makeNavButton: the arrow is topBarFontColor and the button
                // background is that SAME color at 30% opacity (a soft self-tinted chip),
                // rather than a separate surface color.
                containerColor = palette.topBarFont.copy(alpha = 0.3f),
                contentColor = palette.topBarFont,
                onClick = onBack
            )
            Text(title, style = AmaniV2Type.header.scaled(), color = palette.topBarFont)
            Spacer(Modifier.size(AmaniV2Dimens.iconButtonSize.scaled()))
        }
        // Full-bleed hairline divider under the toolbar (iOS parity): separates the header
        // from the content / step bar below and spans edge-to-edge (unlike the padded rows).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border)
        )
        if (steps != null) {
            // The step progress zone sits below the toolbar on the app background
            // (GeneralConfigs.appBackground) — deliberately NOT topBarBackground, so the
            // dots read as screen content, not toolbar chrome.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(horizontal = AmaniV2Dimens.screenPadding)
            ) {
                Spacer(Modifier.height(12.dp))
                DotProgressBar(steps = steps)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

/**
 * Soft rounded icon button used in headers. Its corner radius follows the config
 * `buttonRadiusAndroid` like every other button; the back button opts out via [cornerRadius]
 * and keeps the fixed [AmaniV2Dimens.iconButtonRadius].
 */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = AmaniV2Theme.palette.backgroundWarm,
    contentColor: Color = AmaniV2Theme.palette.ink,
    cornerRadius: Dp = AmaniV2Theme.palette.buttonRadius.dp,
    onClick: () -> Unit = {}
) {
    // Clip to the rounded shape BEFORE .clickable so the press ripple is bounded by the
    // corner radius too — otherwise the ripple draws as a square over the rounded background.
    val shape = RoundedCornerShape(cornerRadius.scaled())
    Box(
        modifier = modifier
            .size(AmaniV2Dimens.iconButtonSize.scaled())
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp.scaled()))
    }
}
