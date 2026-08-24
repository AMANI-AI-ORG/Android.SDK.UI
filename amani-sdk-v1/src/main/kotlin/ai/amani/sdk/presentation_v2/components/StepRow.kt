package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Row states for a verification step. [Processing] is the transient state while the
 * captured document is uploading / awaiting the server verdict: it looks like [Active]
 * but shows a spinner instead of the "go" arrow and is not tappable.
 */
enum class StepRowStatus { Done, Active, Locked, Rejected, Processing }

data class StepError(val title: String?, val message: String)

data class VerificationStep(
    val index: Int,
    val title: String,
    // Blank/null means the row shows no status subtitle line. Config-driven, this carries
    // the server StepConfig.buttonText for the step's status.
    val subtitle: String?,
    val status: StepRowStatus,
    val error: StepError? = null,
    // StepConfig.buttonColor for the step's current status: card wash, border and badge fill.
    // An Active step ignores it and uses the brand accent (iOS HomeV2StepCard parity).
    val fillColor: Color? = null,
    // StepConfig.buttonTextColor: the color drawn on that fill (the badge glyph).
    val textColor: Color? = null,
    // Id of the backing KYC rule, so a tap on an actionable row can start that exact step
    // (used by free selection when no mandatory gating is configured). Null in previews.
    val ruleId: String? = null,
    // Primary-button label to show while THIS step is the selected one (e.g. "Start with
    // Identification"). Set only for actionable rows; null otherwise.
    val ctaLabel: String? = null
)

/**
 * A single verification step card (done / active / locked / rejected), with an
 * optional inline error block.
 */
@Composable
fun StepRow(
    step: VerificationStep,
    modifier: Modifier = Modifier,
    // Whether this row is the currently-selected step (chosen by tapping). Drives the
    // prominent border + tinted fill. Selection lives in the screen; a Processing row is
    // always highlighted regardless.
    selected: Boolean = false,
    // When non-null the whole card is tappable — used for actionable rows (Active/Rejected)
    // so the user can pick that step. Null → non-interactive (Done/Locked/Processing).
    onClick: (() -> Unit)? = null
) {
    val palette = AmaniV2Theme.palette

    val fontColor = palette.ink
    val mutedColor = fontColor.copy(alpha = 0.45f)
    val inactiveSurface = fontColor.copy(alpha = 0.07f)
    val inactiveBorder = fontColor.copy(alpha = 0.18f)
    val inactiveBadge = fontColor.copy(alpha = 0.12f)

    val configFill = step.fillColor ?: palette.surface
    val configText = step.textColor ?: palette.ink

    val statusColor = when (step.status) {
        StepRowStatus.Active -> palette.accent
        StepRowStatus.Locked -> inactiveBadge
        else -> configFill
    }
    val statusTextColor = when (step.status) {
        StepRowStatus.Active -> Color.White
        StepRowStatus.Locked -> mutedColor
        else -> configText
    }

    val isSelected = selected || step.status == StepRowStatus.Processing
    val containerColor = when (step.status) {
        StepRowStatus.Active -> configFill.copy(alpha = 0.07f)
        StepRowStatus.Locked -> inactiveSurface
        else -> statusColor.copy(alpha = 0.07f)
    }
    val borderColor = when (step.status) {
        StepRowStatus.Active, StepRowStatus.Rejected -> palette.accent
        StepRowStatus.Locked -> inactiveBorder
        else -> statusColor
    }
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val badgeFill = if (step.status == StepRowStatus.Locked) inactiveBadge else statusColor
    val titleColor = if (step.status == StepRowStatus.Locked) mutedColor else fontColor
    val contentAlpha = if (step.status == StepRowStatus.Rejected) 0.75f else 1f

    val rowShape = RoundedCornerShape(palette.buttonRadius.dp.scaled())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(containerColor)
            .border(borderWidth, borderColor, rowShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp.scaled(), vertical = 12.dp.scaled())
    ) {
        Column(modifier = Modifier.alpha(contentAlpha)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp.scaled())
            ) {
                StepBadge(
                    status = step.status,
                    index = step.index,
                    fill = badgeFill,
                    glyph = statusTextColor
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        step.title,
                        style = AmaniV2Type.body.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp.scaled()
                        ),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    step.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            subtitle,
                            style = AmaniV2Type.bodySmall.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp.scaled()
                            ),
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp.scaled())
                        )
                    }
                }
                when (step.status) {
                    StepRowStatus.Active -> Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp.scaled())
                    )
                    StepRowStatus.Locked -> Icon(
                        Icons.Filled.Lock,
                        null,
                        tint = fontColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp.scaled())
                    )
                    StepRowStatus.Processing -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp.scaled()),
                        color = statusColor,
                        strokeWidth = 2.dp
                    )
                    StepRowStatus.Rejected, StepRowStatus.Done -> {}
                }
            }

            if (step.error != null) {
                InlineError(step.error, statusColor)
            }
        }
    }
}

/**
 * Solid status badge: [fill] is the state's color (config `buttonColor`, brand accent while
 * active) and [glyph] the color drawn on it (config `buttonTextColor`, white while active).
 */
@Composable
private fun StepBadge(status: StepRowStatus, index: Int, fill: Color, glyph: Color) {
    Box(
        modifier = Modifier
            .size(32.dp.scaled())
            .background(fill, RoundedCornerShape(10.dp.scaled())),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            StepRowStatus.Done -> Icon(Icons.Filled.Check, null, tint = glyph, modifier = Modifier.size(18.dp.scaled()))
            StepRowStatus.Rejected -> Icon(Icons.Filled.Close, null, tint = glyph, modifier = Modifier.size(18.dp.scaled()))
            else -> Text("$index", style = AmaniV2Type.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp.scaled()), color = glyph)
        }
    }
}

@Composable
private fun InlineError(error: StepError, statusColor: Color) {
    val palette = AmaniV2Theme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp.scaled())
            .background(palette.background, RoundedCornerShape(10.dp.scaled()))
            .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp.scaled()))
            .padding(horizontal = 12.dp.scaled(), vertical = 10.dp.scaled()),
        horizontalArrangement = Arrangement.spacedBy(10.dp.scaled())
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = palette.ink, modifier = Modifier.size(16.dp.scaled()))
        Column(modifier = Modifier.weight(1f)) {
            val hasTitle = !error.title.isNullOrBlank()
            if (hasTitle) {
                Text(error.title!!, style = AmaniV2Type.caption.copy(fontWeight = FontWeight.Medium, fontSize = AmaniV2Type.caption.fontSize.scaled()), color = palette.ink)
            }
            Text(
                error.message,
                style = AmaniV2Type.caption.copy(fontSize = AmaniV2Type.caption.fontSize.scaled()),
                color = palette.ink.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = if (hasTitle) 3.dp.scaled() else 0.dp)
            )
        }
    }
}
