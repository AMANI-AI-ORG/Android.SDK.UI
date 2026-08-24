package ai.amani.sdk.presentation_v2.selfie_capture

import ai.amani.amani_sdk.R
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * State backing [SelfieGuideScreen] — the pre-selfie guide (V2 redesign of the legacy selfie
 * instruction animations). All copy is resolved by
 * [SelfieMapper.toGuideState]; the looping illustration comes from the raw Lottie asset passed
 * separately (so this state stays resource-free and preview-friendly).
 */
data class SelfieGuideUiState(
    val headerTitle: String,
    val title: String,
    val description: String,
    val checklistHeader: String,
    val checklistItems: List<String>,
    val buttonText: String
)

/**
 * Pre-selfie guide screen shown *before* the selfie camera. The V2 counterpart of the legacy
 * selfie instruction animations (SelfieCaptureFragment played
 * `animation_first_selfie_instruction` / `animation_second_selfie_instruction` ahead of the
 * camera). The looping [animationRes] plays as an instructional illustration and the user
 * advances explicitly with "Open camera"; the pose-estimation flow chains two of these
 * screens (see [ai.amani.sdk.presentation_v2.navigation.CaptureFlow.selfieAfterGuide]).
 *
 * Deliberately has no step-progress dots (per redesign): just the illustration and a quality
 * checklist. Stateless — receives a [SelfieGuideUiState] and emits [onContinue] / [onBack].
 */
@Composable
fun SelfieGuideScreen(
    state: SelfieGuideUiState,
    @RawRes animationRes: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
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
            Text(state.title, style = AmaniV2Type.heading.scaled(), color = palette.ink)
            Spacer(Modifier.height(6.dp))
            Text(state.description, style = AmaniV2Type.body.scaled(), color = palette.inkMuted)
            Spacer(Modifier.height(18.dp))
            SelfieGuideIllustration(
                animationRes = animationRes,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            SelfieGuideChecklist(header = state.checklistHeader, items = state.checklistItems)
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
 * The looping selfie illustration with a small dark face badge in the top-right corner (per
 * redesign). In `@Preview`/inspection there is no window to host the Lottie view, so a static
 * placeholder stands in.
 */
@Composable
private fun SelfieGuideIllustration(
    @RawRes animationRes: Int,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    Box(modifier = modifier.height(260.dp.scaled()), contentAlignment = Alignment.Center) {
        if (LocalInspectionMode.current) {
            Box(
                modifier = Modifier
                    .size(160.dp.scaled(), 200.dp.scaled())
                    .background(palette.accentSofter, RoundedCornerShape(24.dp))
                    .border(1.dp, palette.accentSoft, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Face, contentDescription = null, tint = palette.accent, modifier = Modifier.size(48.dp))
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
        // Dark face-scan badge (design): ink circle + white glyph, both config-driven.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(44.dp.scaled())
                .background(palette.ink, RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Face, contentDescription = null, tint = palette.surface, modifier = Modifier.size(22.dp.scaled()))
        }
    }
}

/** "Before you start" quality checklist card (design). */
@Composable
private fun SelfieGuideChecklist(
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

@Preview(name = "SelfieGuide — first", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewSelfieGuideFirst() {
    AmaniV2Theme {
        SelfieGuideScreen(
            state = SelfieGuideUiState(
                headerTitle = "Selfie",
                title = "Let's take your selfie",
                description = "Look straight at the camera and keep your face centered in the frame.",
                checklistHeader = "Before you start",
                checklistItems = listOf(
                    "Good, even lighting on your face",
                    "Remove glasses, hats, or masks",
                    "Hold the phone at eye level"
                ),
                buttonText = "Open camera"
            ),
            animationRes = R.raw.animation_first_selfie_instruction,
            onContinue = {}
        )
    }
}
