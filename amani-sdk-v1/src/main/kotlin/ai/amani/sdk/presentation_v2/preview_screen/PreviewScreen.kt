package ai.amani.sdk.presentation_v2.preview_screen

import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** State backing [PreviewScreen]. */
data class PreviewScreenUiState(
    val headerTitle: String,
    val title: String,
    val description: String,
    val confirmButtonText: String,
    val retakeButtonText: String,
    /**
     * The just-captured frame, handed over in memory (see [CapturedFrame]) so it keeps the
     * orientation the camera delivered — no file round-trip, no EXIF handling. Null only in
     * previews/inspection (renders the placeholder).
     */
    val bitmap: android.graphics.Bitmap? = null,
    /**
     * Reassurance checklist shown under the photo (HTML confirm design). Empty hides the
     * card (e.g. selfie confirm).
     */
    // TODO: config-driven
    val qualityChecks: List<String> = emptyList()
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(state.title, style = AmaniV2Type.heading.scaled(), color = palette.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                state.description,
                style = AmaniV2Type.body.scaled(),
                color = palette.inkMuted
            )
            Spacer(Modifier.height(20.dp))
            // The captured frame arrives as an in-memory bitmap straight from the camera
            // callback, already upright — no decode, no EXIF handling needed.
            val captured = state.bitmap
            // Size the frame to the *photo's* aspect ratio so it fills the full width with no
            // dead side/top margins, and shows as large as possible (HTML-style responsive).
            val imageAspect = remember(captured) {
                captured?.takeIf { it.height > 0 }?.let { it.width.toFloat() / it.height } ?: 1.586f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspect)
                    .clip(RoundedCornerShape(AmaniV2Dimens.cardRadius))
                    .background(
                        if (captured != null) Color.Black else palette.backgroundWarm,
                        RoundedCornerShape(AmaniV2Dimens.cardRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (captured != null) {
                    // Box already matches the photo aspect, so Crop fills it exactly (no gaps).
                    Image(
                        bitmap = captured.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
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
            if (state.qualityChecks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                QualityChecksCard(state.qualityChecks)
            }
            Spacer(Modifier.height(20.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Clear the system navigation bar so the buttons aren't overlapped.
                .navigationBarsPadding()
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

/** Reassurance checklist card under the photo (HTML confirm design). */
@Composable
private fun QualityChecksCard(items: List<String>) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmaniV2Dimens.fieldRadius))
            .background(palette.surface)
            .border(0.5.dp, palette.border, RoundedCornerShape(AmaniV2Dimens.fieldRadius))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(palette.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(item, style = AmaniV2Type.bodySmall.scaled(), color = palette.ink)
            }
        }
    }
}

// region Sample state (also used by previews)

internal val SamplePreviewScreen = PreviewScreenUiState(
    headerTitle = "Verification",
    title = "Is your ID clear and readable?",
    description = "Check that all four corners are visible and there's no glare before continuing.",
    confirmButtonText = "Looks good",
    retakeButtonText = "Retake photo",
    qualityChecks = listOf("Sharp & in focus", "Document fully visible", "No glare or shadows")
)

@Preview(name = "CaptureConfirm", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewScreenPreview() {
    AmaniV2Theme { PreviewScreen(state = SamplePreviewScreen) }
}
