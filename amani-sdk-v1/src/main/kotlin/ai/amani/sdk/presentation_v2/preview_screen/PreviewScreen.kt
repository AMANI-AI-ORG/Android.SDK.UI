package ai.amani.sdk.presentation_v2.preview_screen

import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File

/** State backing [PreviewScreen]. */
data class PreviewScreenUiState(
    val headerTitle: String,
    val title: String,
    val description: String,
    val confirmButtonText: String,
    val retakeButtonText: String,
    /** Absolute path of the captured frame to preview; null only in previews/inspection. */
    val imagePath: String? = null
)

/**
 * Captured-image confirmation screen. The user confirms a clear capture or retakes it;
 * the "what's next" decision lives in the navigation layer, so this screen only emits intent.
 */
@Composable
fun PreviewScreen(
    state: PreviewScreenUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onRetake: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AmaniV2Dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(state.title, style = AmaniV2Type.heading.scaled(), color = palette.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                state.description,
                style = AmaniV2Type.body.scaled(),
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            // Decode the captured frame, remembered on the path so it isn't repeated on recomposition.
            val captured = remember(state.imagePath) {
                state.imagePath
                    ?.takeIf { File(it).exists() }
                    ?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
            }
            // Center the captured frame between the description and the action buttons.
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.586f)
                    .clip(RoundedCornerShape(AmaniV2Dimens.cardRadius))
                    .background(palette.backgroundWarm, RoundedCornerShape(AmaniV2Dimens.cardRadius)),
                contentAlignment = Alignment.Center
            ) {
                if (captured != null) {
                    // Fit (not Crop) so the whole captured document stays visible.
                    Image(
                        bitmap = captured.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Previews/inspection (or a missing file) fall back to a glyph placeholder.
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = palette.inkLight,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(AmaniV2Dimens.gapSm)
        ) {
            SecondaryButton(
                text = state.retakeButtonText,
                leadingIcon = Icons.Outlined.Refresh,
                modifier = Modifier.weight(1f),
                onClick = onRetake
            )
            PrimaryButton(
                text = state.confirmButtonText,
                modifier = Modifier.weight(1f),
                onClick = onConfirm
            )
        }
    }
}

// region Sample state (also used by previews)

internal val SamplePreviewScreen = PreviewScreenUiState(
    headerTitle = "Verification",
    title = "Is your ID clear and readable?",
    description = "Check that all four corners are visible and there's no glare before continuing.",
    confirmButtonText = "Looks good",
    retakeButtonText = "Retake photo"
)

@Preview(name = "CaptureConfirm", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewScreenPreview() {
    AmaniV2Theme { PreviewScreen(state = SamplePreviewScreen) }
}
