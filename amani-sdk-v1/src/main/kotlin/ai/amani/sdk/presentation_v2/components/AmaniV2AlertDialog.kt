package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.CappedCornerShape
import ai.amani.sdk.presentation_v2.theme.configCornerRadius
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The V2 counterpart of v1's `Fragment.alertDialog`: a centred card that states what went wrong
 * and offers the way out, in the flow's own palette, typography and button styles.
 *
 * Deliberately not a platform dialog and not a toast — a toast disappears while the user is still
 * reading it and says nothing about what happens next, which is why a failed step used to look
 * like the screen simply went backwards on its own.
 *
 * @param title short statement of what happened
 * @param description what the user should do about it
 * @param confirmText label of the action that continues the flow
 * @param onConfirm invoked when the user takes that action
 * @param dismissText optional second action; null hides it
 */
@Composable
fun AmaniV2AlertDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
    accent: Color = AmaniV2Theme.palette.accent,
    dismissText: String? = null,
    onDismiss: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            // Swallow taps on the scrim: the dialog states a decision the flow needs.
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(AmaniV2Dimens.screenPadding)
                .fillMaxWidth()
                .background(palette.surface, CappedCornerShape(configCornerRadius()))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = AmaniV2Type.rowTitle.scaled(),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = AmaniV2Type.bodySmall.scaled(),
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = confirmText, onClick = onConfirm)
            if (dismissText != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = dismissText, onClick = onDismiss)
            }
        }
    }
}

@Preview(name = "Alert dialog", showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun AmaniV2AlertDialogPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        AmaniV2AlertDialog(
            title = "Try again",
            description = "We couldn't read the machine-readable zone. Capture the back of your " +
                "document again in good lighting.",
            confirmText = "OK",
            onConfirm = {}
        )
    }
}
