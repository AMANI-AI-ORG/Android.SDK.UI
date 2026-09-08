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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
     * card. Config-driven: Version.v2DocumentQuality1..3.
     */
    val qualityChecks: List<String> = emptyList(),
    /**
     * Eyebrow header of the checklist card ("ID QUALITY CHECKS"). Blank/null hides the
     * header line. Config-driven: Version.v2DocumentQualityHeader.
     */
    val qualityChecksHeader: String? = null
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
        // No scroll container: the texts, the frame and the checklist all have to be on screen
        // at once. Everything but the frame takes its natural height, and the frame area takes
        // whatever is left over — so the layout adapts to the window instead of extending past
        // it. A very short window shrinks the frame, it never pushes content out of view.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
            Spacer(Modifier.height(AmaniV2Dimens.gapMd))
            // The captured frame arrives as an in-memory bitmap straight from the camera
            // callback, already upright — no decode, no EXIF handling needed.
            val captured = state.bitmap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Transparent behind the frame: the photo keeps its own aspect ratio, so
                    // a colored box would show through as letterbox bars beside or under it.
                    .background(if (captured == null) palette.backgroundWarm else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (captured != null) {
                    // Never crop a capture the user is being asked to judge: the frame keeps
                    // its aspect ratio and is scaled down until its height fits the area, so
                    // an ID keeps its corners and a portrait selfie keeps the head and
                    // shoulders the camera actually recorded.
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
            if (state.qualityChecks.isNotEmpty()) {
                Spacer(Modifier.height(AmaniV2Dimens.gapMd))
                QualityChecksCard(state.qualityChecks, state.qualityChecksHeader)
            }
            Spacer(Modifier.height(AmaniV2Dimens.gapMd))
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
private fun QualityChecksCard(items: List<String>, header: String? = null) {
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
        // Eyebrow header ("ID QUALITY CHECKS") above the checklist rows (design v2.6).
        header?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = AmaniV2Type.eyebrow.scaled(), color = palette.inkMuted)
        }
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
    qualityChecks = listOf("Sharp & in focus", "Document fully visible", "No glare or shadows"),
    qualityChecksHeader = "ID QUALITY CHECKS"
)

internal val SampleSelfiePreviewScreen = PreviewScreenUiState(
    headerTitle = "Review your selfie",
    title = "Looks good?",
    description = "Make sure your face is clear and well illuminated.",
    confirmButtonText = "Looks good",
    retakeButtonText = "Retake selfie",
    qualityChecks = listOf("Face clearly visible", "Well lit, no harsh shadows", "Eyes open and looking forward"),
    qualityChecksHeader = "SELFIE QUALITY CHECKS"
)

/**
 * A stand-in for a real capture, drawn at the aspect ratio the camera actually delivers, so
 * the previews show the same layout the user gets instead of the placeholder glyph:
 *
 *  - [IdCard] 1.586:1 — the ID-1 card ratio the rear camera crop hands to the ID steps.
 *  - [SelfiePortrait] 3:4 — the front-camera still on most devices.
 *  - [SelfieTall] 9:16 — the front-camera still on tall sensors, the tightest fit for a
 *    portrait frame in a fixed area.
 */
private enum class SimulatedCapture(val widthPx: Int, val heightPx: Int) {
    IdCard(1000, 630),
    SelfiePortrait(900, 1200),
    SelfieTall(720, 1280)
}

/**
 * Paints a recognisable stand-in for [capture] — a document face with a photo box and text
 * lines, or a head-and-shoulders silhouette — so a preview shows how the real frame sits in
 * the area at its own aspect ratio. Preview-only: nothing on the runtime path draws this.
 */
private fun simulatedCapture(capture: SimulatedCapture): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(
        capture.widthPx, capture.heightPx, android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    val w = capture.widthPx.toFloat()
    val h = capture.heightPx.toFloat()

    if (capture == SimulatedCapture.IdCard) {
        // Card body, portrait box on the left, data lines on the right.
        paint.color = android.graphics.Color.parseColor("#D9E2EC")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = android.graphics.Color.parseColor("#F7FAFC")
        canvas.drawRoundRect(w * 0.04f, h * 0.06f, w * 0.96f, h * 0.94f, 24f, 24f, paint)
        paint.color = android.graphics.Color.parseColor("#B6C4D2")
        canvas.drawRoundRect(w * 0.08f, h * 0.18f, w * 0.34f, h * 0.82f, 12f, 12f, paint)
        paint.color = android.graphics.Color.parseColor("#8494A5")
        var lineY = h * 0.22f
        while (lineY < h * 0.78f) {
            canvas.drawRoundRect(w * 0.40f, lineY, w * 0.90f, lineY + h * 0.05f, 8f, 8f, paint)
            lineY += h * 0.12f
        }
    } else {
        // Selfie: a lit background with a head-and-shoulders silhouette, centred horizontally
        // and sitting in the upper half, as a front camera frames it.
        paint.color = android.graphics.Color.parseColor("#25303C")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = android.graphics.Color.parseColor("#3A4A5A")
        canvas.drawCircle(w * 0.5f, h * 0.34f, w * 0.30f, paint)
        canvas.drawRoundRect(w * 0.16f, h * 0.62f, w * 0.84f, h, w * 0.3f, w * 0.3f, paint)
    }
    return bitmap
}

/** ID confirmation with a simulated 1.586:1 card capture — the whole card stays visible. */
@Preview(name = "CaptureConfirm — ID 1.586:1", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewScreenPreview() {
    AmaniV2Theme {
        PreviewScreen(
            state = SamplePreviewScreen.copy(bitmap = simulatedCapture(SimulatedCapture.IdCard))
        )
    }
}

/** Selfie confirmation with a simulated 3:4 front-camera capture — scaled to fit, not cropped. */
@Preview(name = "SelfieConfirm — selfie 3:4", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SelfiePreviewScreenPreview() {
    AmaniV2Theme {
        PreviewScreen(
            state = SampleSelfiePreviewScreen.copy(
                bitmap = simulatedCapture(SimulatedCapture.SelfiePortrait)
            )
        )
    }
}

/** The tall 9:16 selfie: the height drives the scale, the whole frame still fits the area. */
@Preview(name = "SelfieConfirm — selfie 9:16", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SelfieTallPreviewScreenPreview() {
    AmaniV2Theme {
        PreviewScreen(
            state = SampleSelfiePreviewScreen.copy(
                bitmap = simulatedCapture(SimulatedCapture.SelfieTall)
            )
        )
    }
}

/** Short window (landscape phone): the frame shrinks, nothing scrolls out of reach. */
@Preview(name = "SelfieConfirm — short window", showBackground = true, widthDp = 640, heightDp = 400)
@Composable
private fun SelfiePreviewScreenShortPreview() {
    AmaniV2Theme {
        PreviewScreen(
            state = SampleSelfiePreviewScreen.copy(
                bitmap = simulatedCapture(SimulatedCapture.SelfiePortrait)
            )
        )
    }
}

/** Small phone (320dp wide, 640dp tall): the tightest window the layout has to fit into. */
@Preview(name = "CaptureConfirm — small phone", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun PreviewScreenSmallPhonePreview() {
    AmaniV2Theme {
        PreviewScreen(
            state = SamplePreviewScreen.copy(bitmap = simulatedCapture(SimulatedCapture.IdCard))
        )
    }
}

/** No frame handed over (placeholder path) — kept so that fallback stays visible in review. */
@Preview(name = "CaptureConfirm — no frame", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewScreenPlaceholderPreview() {
    AmaniV2Theme { PreviewScreen(state = SamplePreviewScreen) }
}
