package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Small rounded status badge with an optional leading icon (HTML pill/badge pattern).
 */
@Composable
fun Pill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    borderColor: Color? = null
) {
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .then(
                if (borderColor != null)
                    Modifier.border(0.5.dp, borderColor, RoundedCornerShape(999.dp))
                else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
        }
        Text(text, style = AmaniV2Type.label, color = contentColor)
    }
}

/** Live-status dot + label pill, e.g. "Detecting..." / "Searching for chip...". */
@Composable
fun StatusPill(
    text: String,
    containerColor: Color = AmaniV2Theme.palette.accent,
    contentColor: Color = AmaniV2Theme.palette.surface,
    dotColor: Color = AmaniV2Theme.palette.surface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, RoundedCornerShape(999.dp))
        )
        Text(text, style = AmaniV2Type.caption, color = contentColor)
    }
}
