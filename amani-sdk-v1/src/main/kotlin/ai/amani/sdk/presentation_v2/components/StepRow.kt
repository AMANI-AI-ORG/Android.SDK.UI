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
    // Config-driven per-status styling (server StepConfig.buttonColor), resolved for the
    // step's *current* status in HomeKYCMapper. Null falls back to the built-in palette.
    // [accentColor] is the status color used for the border (full), the inner fill (a light
    // translucent wash), the number-badge fill and the trailing icon.
    // The number-badge glyph itself is always white (per the HTML design).
    val accentColor: Color? = null,
    // StepConfig.buttonTextColor — drives the step's title/subtitle text color (kept distinct
    // from the badge glyph, which stays white). Null falls back to [accentColor].
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

    // HTML "second screen" step buttons: a light, slightly transparent wash of the status
    // color *inside*, a full-strength border, and a full-color number badge with white text.
    // The color is the step's *current* status color (StepConfig.buttonColor, resolved
    // per-status in HomeKYCMapper.stepStyle — incl. the `processing` variant while uploading /
    // awaiting the verdict). Without config (previews) it falls back to the built-in palette.
    val accent = step.accentColor ?: when (step.status) {
        StepRowStatus.Rejected -> palette.danger
        StepRowStatus.Locked -> palette.inkLight
        else -> palette.accent
    }
    // Only the selected step (or one in progress) gets the prominent colored border + tinted
    // fill; every other row is plain white with a hairline, so the chosen step is
    // unmistakable. Uses the SAME structure and tokens as the document-select card
    // (SelectDocumentTypeScreen): the brand [palette.accent] border + [palette.accentSofter]
    // fill — deliberately the brand color (not the per-status config color, which a tenant
    // may set low-contrast) so the selection border is always clearly visible.
    val isSelected = selected || step.status == StepRowStatus.Processing
    val containerColor = if (isSelected) palette.accentSofter else palette.surface
    val borderColor = if (isSelected) palette.accent else palette.border
    val borderWidth = if (isSelected) 1.5.dp else 0.5.dp
    // StepConfig.buttonTextColor drives the title/subtitle text (distinct from the accent
    // fill and the always-white badge glyph). Falls back to the accent when config is absent.
    val textColor = step.textColor ?: accent

    // A locked step is dimmed to read as not-yet-available, but not so faint that its number
    // badge becomes unreadable (the badge itself uses a readable scheme below).
    val rowAlpha = if (step.status == StepRowStatus.Locked) 0.6f else 1f
    val rowShape = RoundedCornerShape(14.dp.scaled())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .background(containerColor, rowShape)
            .border(borderWidth, borderColor, rowShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            // Slightly smaller than the document-select cards (44dp icon / 18dp radius there).
            .padding(horizontal = 16.dp.scaled(), vertical = 14.dp.scaled())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp.scaled())
        ) {
            StepBadge(step.status, step.index, accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    step.title,
                    // Bold, full-strength, larger title so it reads as the row's headline.
                    style = AmaniV2Type.body.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp.scaled()
                    ),
                    // Step text uses the config buttonTextColor (falls back to accent).
                    color = textColor,
                    // TODO(width): keep the title on one line for now so many-step lists
                    // don't get tall from wrapping. Revisit if we want full multi-line titles.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                step.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    val subtitleWeight = if (step.status == StepRowStatus.Locked) FontWeight.Normal else FontWeight.Medium
                    Text(
                        subtitle,
                        style = AmaniV2Type.bodySmall.copy(
                            fontWeight = subtitleWeight,
                            fontSize = 13.sp.scaled()
                        ),
                        // Same buttonTextColor as the title, only slightly dimmed for hierarchy.
                        color = textColor.copy(alpha = 0.9f),
                        // Single line too (see title TODO).
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp.scaled())
                    )
                }
            }
            when (step.status) {
                StepRowStatus.Active -> Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent, modifier = Modifier.size(16.dp.scaled()))
                StepRowStatus.Processing -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp.scaled()),
                    color = accent,
                    strokeWidth = 2.dp
                )
                StepRowStatus.Locked -> Icon(Icons.Filled.Lock, null, tint = accent, modifier = Modifier.size(14.dp.scaled()))
                StepRowStatus.Rejected -> Icon(Icons.Filled.Warning, null, tint = accent, modifier = Modifier.size(18.dp.scaled()))
                StepRowStatus.Done -> {}
            }
        }

        if (step.error != null) {
            InlineError(step.error, accent)
        }
    }
}

@Composable
private fun StepBadge(status: StepRowStatus, index: Int, accent: Color) {
    val palette = AmaniV2Theme.palette
    // Active/done/rejected: full status-color fill + white glyph (HTML). Locked: a light warm
    // fill with a dark muted number, so the number stays readable through the row's dimming
    // (a white number on the grey lock color washed out at low opacity).
    val locked = status == StepRowStatus.Locked
    val bg = if (locked) palette.backgroundWarm else accent
    val fg = if (locked) palette.inkMuted else Color.White
    Box(
        modifier = Modifier
            .size(36.dp.scaled())
            .background(bg, RoundedCornerShape(11.dp.scaled())),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            StepRowStatus.Done -> Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(18.dp.scaled()))
            StepRowStatus.Rejected -> Icon(Icons.Filled.Close, null, tint = fg, modifier = Modifier.size(18.dp.scaled()))
            else -> Text("$index", style = AmaniV2Type.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp.scaled()), color = fg)
        }
    }
}

@Composable
private fun InlineError(error: StepError, accent: Color) {
    val palette = AmaniV2Theme.palette
    // A white inset note on the light-wash card: full status-color border + icon ([accent]),
    // dark readable text.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp.scaled())
            .background(palette.surface, RoundedCornerShape(10.dp.scaled()))
            .border(0.5.dp, accent, RoundedCornerShape(10.dp.scaled()))
            .padding(horizontal = 12.dp.scaled(), vertical = 10.dp.scaled()),
        horizontalArrangement = Arrangement.spacedBy(10.dp.scaled())
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = accent, modifier = Modifier.size(16.dp.scaled()))
        Column(modifier = Modifier.weight(1f)) {
            val hasTitle = !error.title.isNullOrBlank()
            if (hasTitle) {
                Text(error.title!!, style = AmaniV2Type.caption.copy(fontWeight = FontWeight.Medium, fontSize = AmaniV2Type.caption.fontSize.scaled()), color = palette.ink)
            }
            Text(
                error.message,
                style = AmaniV2Type.caption.copy(fontSize = AmaniV2Type.caption.fontSize.scaled()),
                color = palette.inkMuted,
                modifier = Modifier.padding(top = if (hasTitle) 3.dp.scaled() else 0.dp)
            )
        }
    }
}
