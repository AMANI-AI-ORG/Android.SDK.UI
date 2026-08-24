package ai.amani.sdk.presentation_v2.id_capture

import ai.amani.amani_sdk.R
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.navigation.CaptureSide
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
import android.content.Context
import android.widget.ImageView
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * State backing [IdCaptureGuideScreen] — the pre-capture guide (prototype screens 7 & 10),
 * the V2 redesign of the legacy "Upload Front/Back Side" Lottie screens. All copy is
 * resolved by [CaptureMapper.toGuideState]; the looping illustration comes from the raw
 * Lottie asset passed separately (so this state stays resource-free and preview-friendly).
 */
data class IdCaptureGuideUiState(
    val headerTitle: String,
    val eyebrow: String,
    val title: String,
    val description: String,
    val checklistHeader: String,
    val checklistItems: List<String>,
    val buttonText: String
)

/**
 * Resolves the pre-capture guide Lottie animation for a document by its type + side, mirroring
 * when the document has no type-specific animation. So the guide shows the right document
 * illustration per identity type
 */
@RawRes
fun idGuideAnimationRes(context: Context, versionType: String?, side: CaptureSide): Int {
    val sideSuffix = if (side == CaptureSide.Back) "back" else "front"
    val type = versionType?.trim()?.lowercase()
    if (!type.isNullOrEmpty()) {
        // getIdentifier is the Android equivalent of iOS's LottieAnimation.named(name): resolve
        // the raw resource whose name matches the document type + side at runtime.
        val id = context.resources.getIdentifier("${type}_$sideSuffix", "raw", context.packageName)
        if (id != 0) return id
    }
    return if (side == CaptureSide.Back) R.raw.xx_id_back else R.raw.xx_id_front
}

/**
 * Pre-capture guide screen shown *before* the camera for each document side. Mirrors the
 * legacy flow (IDCaptureFront/BackSideFrag played `xx_id_front` / `xx_id_back` ahead of the
 * capture Fragment) but redesigned: the animation now loops as an instructional illustration
 * and the user advances explicitly with "Open camera" (rather than the old auto-advance on
 * animation end), alongside a quality checklist.
 *
 * Stateless: receives an [IdCaptureGuideUiState] plus the [animationRes] to play, and emits
 * [onContinue] / [onBack] intents. [badgeIcon] is the small accent badge over the
 * illustration (camera for the front, flip for the back).
 */
@Composable
fun IdCaptureGuideScreen(
    state: IdCaptureGuideUiState,
    @RawRes animationRes: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    badgeIcon: ImageVector = Icons.Outlined.PhotoCamera
) {
    val palette = AmaniV2Theme.palette
    val contentMaxWidth = amaniV2ContentMaxWidth()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(state.eyebrow.uppercase(), style = AmaniV2Type.eyebrow.scaled(), color = palette.inkLight)
            Spacer(Modifier.height(8.dp))
            Text(state.title, style = AmaniV2Type.heading.scaled(), color = palette.ink)
            Spacer(Modifier.height(6.dp))
            Text(state.description, style = AmaniV2Type.body.scaled(), color = palette.inkMuted)
            Spacer(Modifier.height(18.dp))
            GuideIllustration(
                animationRes = animationRes,
                badgeIcon = badgeIcon,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            GuideChecklist(header = state.checklistHeader, items = state.checklistItems)
            Spacer(Modifier.height(24.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                // Clear the system navigation bar so the button isn't overlapped.
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(bottom = 20.dp)
        ) {
            PrimaryButton(
                text = state.buttonText,
                leadingIcon = Icons.Outlined.PhotoCamera,
                onClick = onContinue
            )
        }
    }
}

/**
 * The looping document illustration with a small accent badge in the top-right corner. In
 * `@Preview`/inspection there is no window to host the Lottie view, so a static placeholder
 * stands in.
 */
@Composable
private fun GuideIllustration(
    @RawRes animationRes: Int,
    badgeIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    Box(modifier = modifier.height(220.dp.scaled()), contentAlignment = Alignment.Center) {
        if (LocalInspectionMode.current) {
            // No Lottie in inspection: a soft framed card with the badge glyph.
            Box(
                modifier = Modifier
                    .size(150.dp.scaled(), 200.dp.scaled())
                    .background(palette.accentSofter, RoundedCornerShape(24.dp))
                    .border(1.dp, palette.accentSoft, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(badgeIcon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(40.dp))
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    LottieAnimationView(ctx).apply {
                        setAnimation(animationRes)
                        repeatCount = LottieDrawable.INFINITE
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        playAnimation()
                    }
                }
            )
        }
        // Accent badge — camera on the front guide, flip on the back (design screens 7 & 10).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(30.dp.scaled())
                .background(palette.accent, RoundedCornerShape(999.dp))
                .border(4.dp, palette.accentSoft, RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(badgeIcon, contentDescription = null, tint = palette.surface, modifier = Modifier.size(15.dp.scaled()))
        }
    }
}

/** "Before you shoot" quality checklist card (design screens 7 & 10). */
@Composable
private fun GuideChecklist(
    header: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val cardShape = RoundedCornerShape(AmaniV2Dimens.gapMd.scaled())
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface, cardShape)
            .border(0.5.dp, palette.border, cardShape)
            .padding(14.dp.scaled())
    ) {
        Text(header.uppercase(), style = AmaniV2Type.eyebrow.scaled(), color = palette.inkLight)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp.scaled())) {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp.scaled())
                            .background(palette.accentSoft, RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(11.dp.scaled())
                        )
                    }
                    Spacer(Modifier.size(10.dp.scaled()))
                    Text(item, style = AmaniV2Type.bodySmall.scaled(), color = palette.ink)
                }
            }
        }
    }
}

@Preview(name = "IdCaptureGuide — front", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewIdCaptureGuideFront() {
    AmaniV2Theme {
        IdCaptureGuideScreen(
            state = IdCaptureGuideUiState(
                headerTitle = "Front of ID",
                eyebrow = "Step 1 of 2 · Photo capture",
                title = "Photograph the front side",
                description = "Take the photo in a bright area and make sure the document fits fully in the frame.",
                checklistHeader = "Before you shoot",
                checklistItems = listOf(
                    "Bright, even lighting",
                    "All four corners visible",
                    "No glare on the photo or text"
                ),
                buttonText = "Open camera"
            ),
            animationRes = R.raw.xx_id_front,
            onContinue = {}
        )
    }
}

@Preview(name = "IdCaptureGuide — back", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewIdCaptureGuideBack() {
    AmaniV2Theme {
        IdCaptureGuideScreen(
            state = IdCaptureGuideUiState(
                headerTitle = "Back of ID",
                eyebrow = "Step 2 of 2 · Photo capture",
                title = "Now flip it over",
                description = "We'll read the machine-readable zone (MRZ) on the back of your card.",
                checklistHeader = "Before you shoot",
                checklistItems = listOf(
                    "MRZ lines fully readable",
                    "Barcode not covered by fingers",
                    "Flat surface, no tilt"
                ),
                buttonText = "Open camera"
            ),
            animationRes = R.raw.xx_id_back,
            onContinue = {},
            badgeIcon = Icons.Filled.Autorenew
        )
    }
}
