package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Full-width pink primary button. */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val shape = RoundedCornerShape(palette.buttonRadius.dp.scaled())
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(AmaniV2Dimens.buttonHeight.scaled()),
        shape = shape,
        // GeneralConfigs.primaryButtonBorderColor; the default is transparent (borderless).
        border = BorderStroke(1.dp, palette.primaryButtonBorder),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.primaryButtonText,
            // Disabled = same accent, just faded (non-clickable look via opacity).
            disabledContainerColor = palette.accent.copy(alpha = 0.4f),
            disabledContentColor = palette.primaryButtonText.copy(alpha = 0.7f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp.scaled()))
            }
            Text(text, style = AmaniV2Type.button.copy(fontSize = AmaniV2Type.button.fontSize.scaled()))
        }
    }
}

/** Full-width white secondary button with a hairline border. */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(AmaniV2Dimens.buttonHeight.scaled()),
        shape = RoundedCornerShape(palette.buttonRadius.dp.scaled()),
        // GeneralConfigs.secondaryButton{BorderColor, BackgroundColor, TextColor}; the
        // defaults reproduce the previous hardcoded look (white fill, hairline border, ink).
        border = BorderStroke(1.dp, palette.secondaryButtonBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.secondaryButtonBackground,
            contentColor = palette.secondaryButtonText
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp.scaled()))
            }
            Text(text, style = AmaniV2Type.button.copy(fontSize = AmaniV2Type.button.fontSize.scaled()))
        }
    }
}
