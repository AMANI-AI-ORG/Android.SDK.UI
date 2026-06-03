package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Top header with back button and centered title, with optional dot progress
 * beneath it (HTML headerWithProgress). A fixed-size spacer balances the back
 * button on the trailing side so the title stays optically centered.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    steps: List<DotStep>? = null,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AmaniV2Dimens.topInset, start = AmaniV2Dimens.screenPadding, end = AmaniV2Dimens.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
            Text(title, style = AmaniV2Type.header, color = AmaniV2Theme.palette.ink)
            Spacer(Modifier.size(AmaniV2Dimens.iconButtonSize))
        }
        if (steps != null) {
            Spacer(Modifier.height(24.dp))
            DotProgressBar(steps = steps)
        }
        Spacer(Modifier.height(if (steps != null) 18.dp else 12.dp))
    }
}

/** Soft rounded icon button used in headers (HTML ICON_BTN). */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = AmaniV2Theme.palette.backgroundWarm,
    contentColor: Color = AmaniV2Theme.palette.ink,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(AmaniV2Dimens.iconButtonSize)
            .background(containerColor, RoundedCornerShape(AmaniV2Dimens.iconButtonRadius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
    }
}
