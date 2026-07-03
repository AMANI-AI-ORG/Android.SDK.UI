package ai.amani.sdk.presentation_v2.approved

import ai.amani.sdk.presentation_v2.components.DotProgressBar
import ai.amani.sdk.presentation_v2.components.DotStep
import ai.amani.sdk.presentation_v2.components.HeaderIconButton
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.StepStatus
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import ai.amani.sdk.presentation_v2.theme.toAmaniColorOrNull
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * State backing [ApprovedScreen]. Config-driven where GeneralConfigs offers a field
 * (successTitle / successHeaderText / successInfo1Text / successInfo2Text / successIconColor /
 * continueText); the rest are static with TODO markers (see [ApprovedMapper]).
 */
data class ApprovedUiState(
    val headerTitle: String,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val cardTitle: String,
    val cardSubtitle: String,
    val buttonText: String,
    val iconColorHex: String?,
    /** Step labels shown as an all-completed dot bar; empty hides the bar. */
    val stepLabels: List<String> = emptyList()
)

/**
 * Final success screen shown when every KYC step is APPROVED — the V2 counterpart of v1's
 * CongratulationsFragment, styled after the HTML "Approved" screen: celebratory gradient,
 * all-completed dots, a pulsing check badge, "approved instantly" pill, and an
 * all-checks-passed card above the continue button.
 *
 * [onContinue] finishes the flow (the host returns KYCResult APPROVED and closes, exactly
 * like v1's finishActivity); the header close button and system back do the same.
 */
@Composable
fun ApprovedScreen(
    state: ApprovedUiState,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val accent = state.iconColorHex.toAmaniColorOrNull() ?: palette.accent

    Column(
        modifier = modifier
            .fillMaxSize()
            // HTML: linear-gradient(bg → pinkSoft) celebratory backdrop.
            .background(Brush.verticalGradient(listOf(palette.background, palette.accentSoft)))
    ) {
        // Header: centered title + close (no back — the flow is complete).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AmaniV2Dimens.topInset,
                    start = AmaniV2Dimens.screenPadding,
                    end = AmaniV2Dimens.screenPadding
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.size(AmaniV2Dimens.iconButtonSize.scaled()))
                Text(state.headerTitle, style = AmaniV2Type.header.scaled(), color = palette.ink)
                HeaderIconButton(icon = Icons.Filled.Close, onClick = onContinue)
            }
            if (state.stepLabels.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                DotProgressBar(
                    steps = state.stepLabels.map { DotStep(label = it, status = StepStatus.Completed) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AmaniV2Dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            ApprovedCheckBadge(accent = accent)
            Spacer(Modifier.height(18.dp))
            // "Approved instantly" pill (HTML).
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AmaniV2Dimens.pillRadius))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Bolt, null, tint = accent, modifier = Modifier.size(12.dp))
                Text(
                    state.badgeText.uppercase(),
                    style = AmaniV2Type.label.copy(fontWeight = FontWeight.SemiBold),
                    color = accent
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                state.title,
                style = AmaniV2Type.title.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp).scaled(),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                state.subtitle,
                style = AmaniV2Type.body.scaled(),
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            // "All checks passed" card (HTML).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AmaniV2Dimens.cardRadius))
                    .background(palette.surface)
                    .border(0.5.dp, palette.border, RoundedCornerShape(AmaniV2Dimens.cardRadius))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(
                        state.cardTitle,
                        style = AmaniV2Type.caption.copy(fontWeight = FontWeight.Medium),
                        color = palette.ink
                    )
                    Text(
                        state.cardSubtitle,
                        style = AmaniV2Type.label.copy(fontWeight = FontWeight.Normal),
                        color = palette.inkMuted,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(bottom = 20.dp)
        ) {
            PrimaryButton(text = state.buttonText, onClick = onContinue)
        }
    }
}

/** Accent check circle with a one-shot expanding pulse ring (HTML `pulseOnce`). */
@Composable
private fun ApprovedCheckBadge(accent: Color, modifier: Modifier = Modifier) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pulse.animateTo(1f, androidx.compose.animation.core.tween(durationMillis = 1200))
    }
    Box(modifier = modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(112.dp)
                .graphicsLayer {
                    val p = pulse.value
                    scaleX = 0.8f + p * 0.6f
                    scaleY = 0.8f + p * 0.6f
                    alpha = (1f - p) * 0.35f
                }
                .clip(CircleShape)
                .background(accent)
        )
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
}

@Preview(name = "Approved", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun ApprovedScreenPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        ApprovedScreen(
            state = ApprovedUiState(
                headerTitle = "Verification",
                badgeText = "Approved instantly",
                title = "You're verified",
                subtitle = "Your account is ready to use. Welcome aboard.",
                cardTitle = "All checks passed",
                cardSubtitle = "Document, biometric, and chip verified",
                buttonText = "Continue to app",
                iconColorHex = null,
                stepLabels = listOf("ID", "Selfie", "Address")
            )
        )
    }
}
