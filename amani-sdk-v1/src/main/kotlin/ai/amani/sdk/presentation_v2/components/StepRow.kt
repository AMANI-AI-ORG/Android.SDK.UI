package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Colors
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp

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
    // Config-driven per-status styling (server StepConfig). Null falls back to the built-in
    // palette styling for [status].
    //
    // [accentColor] is StepConfig.buttonColor — a saturated fill driving the border, badge
    // fill, trailing icon tint, and status-label text. [badgeTextColor] is
    // StepConfig.buttonTextColor — used only for the badge foreground (number/check/close),
    // which sits on the accent fill.
    val accentColor: Color? = null,
    val badgeTextColor: Color? = null
)

/**
 * A single verification step card (done / active / locked / rejected), with an
 * optional inline error block.
 */
@Composable
fun StepRow(
    step: VerificationStep,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val rejected = step.status == StepRowStatus.Rejected
    val active = step.status == StepRowStatus.Active
    val processing = step.status == StepRowStatus.Processing

    // Config-driven accent (server StepConfig.buttonColor): the row gets a light transparent
    // tint of the button color as background plus a solid button-color border, with the
    // status label / badge / icon in the saturated color. Without config, the built-in palette.
    val accent = step.accentColor
    val containerColor = when (step.status) {
        StepRowStatus.Active, StepRowStatus.Processing -> accent?.copy(alpha = 0.10f) ?: palette.accentSofter
        StepRowStatus.Rejected -> accent?.copy(alpha = 0.10f) ?: Color(0xFFFEF2F2)
        else -> palette.surface
    }
    val borderColor = when (step.status) {
        StepRowStatus.Active, StepRowStatus.Processing -> accent ?: palette.accent
        StepRowStatus.Rejected -> accent ?: palette.danger
        else -> palette.border
    }
    val borderWidth = if (active || rejected || processing) 1.5.dp else 0.5.dp
    val rowAlpha = if (step.status == StepRowStatus.Locked) 0.6f else 1f
    val rowShape = RoundedCornerShape(14.dp.scaled())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .background(containerColor, rowShape)
            .border(borderWidth, borderColor, rowShape)
            .padding(horizontal = 16.dp.scaled(), vertical = 14.dp.scaled())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp.scaled())
        ) {
            StepBadge(step.status, step.index, accent, step.badgeTextColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    step.title,
                    style = AmaniV2Type.body.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = AmaniV2Type.body.fontSize.scaled()
                    ),
                    color = palette.ink
                )
                step.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    // The status label uses the saturated accent color to stay legible on the
                    // soft tint, falling back to the built-in per-status color.
                    val subtitleColor = accent ?: when (step.status) {
                        StepRowStatus.Done, StepRowStatus.Active, StepRowStatus.Processing -> palette.accent
                        StepRowStatus.Rejected -> palette.danger
                        StepRowStatus.Locked -> palette.inkLight
                    }
                    val subtitleWeight = if (step.status == StepRowStatus.Locked) FontWeight.Normal else FontWeight.Medium
                    Text(
                        subtitle,
                        style = AmaniV2Type.label.copy(
                            fontWeight = subtitleWeight,
                            fontSize = AmaniV2Type.label.fontSize.scaled()
                        ),
                        color = subtitleColor,
                        modifier = Modifier.padding(top = 2.dp.scaled())
                    )
                }
            }
            when (step.status) {
                StepRowStatus.Active -> Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent ?: palette.accent, modifier = Modifier.size(16.dp.scaled()))
                StepRowStatus.Processing -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp.scaled()),
                    color = accent ?: palette.accent,
                    strokeWidth = 2.dp
                )
                StepRowStatus.Locked -> Icon(Icons.Filled.Lock, null, tint = palette.inkLight, modifier = Modifier.size(14.dp.scaled()))
                StepRowStatus.Rejected -> Icon(Icons.Filled.Warning, null, tint = accent ?: palette.danger, modifier = Modifier.size(18.dp.scaled()))
                StepRowStatus.Done -> {}
            }
        }

        if (step.error != null) {
            InlineError(step.error)
        }
    }
}

@Composable
private fun StepBadge(status: StepRowStatus, index: Int, accent: Color? = null, badgeTextColor: Color? = null) {
    val palette = AmaniV2Theme.palette
    val onAccent = badgeTextColor ?: palette.surface
    // The badge is the saturated accent fill carrying the on-fill foreground
    // (number/check/close). The Locked badge keeps neutral grey for not-yet-reached steps.
    val (bg, fg) = when (status) {
        StepRowStatus.Done, StepRowStatus.Active, StepRowStatus.Processing -> (accent ?: palette.accent) to onAccent
        StepRowStatus.Rejected -> (accent ?: palette.danger) to onAccent
        StepRowStatus.Locked -> AmaniV2Colors.BgWarm to palette.inkMuted
    }
    Box(
        modifier = Modifier
            .size(32.dp.scaled())
            .background(bg, RoundedCornerShape(10.dp.scaled())),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            StepRowStatus.Done -> Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(16.dp.scaled()))
            StepRowStatus.Rejected -> Icon(Icons.Filled.Close, null, tint = fg, modifier = Modifier.size(18.dp.scaled()))
            else -> Text("$index", style = AmaniV2Type.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = AmaniV2Type.bodySmall.fontSize.scaled()), color = fg)
        }
    }
}

@Composable
private fun InlineError(error: StepError) {
    val palette = AmaniV2Theme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp.scaled())
            .background(palette.surface, RoundedCornerShape(10.dp.scaled()))
            .border(0.5.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp.scaled()))
            .padding(horizontal = 12.dp.scaled(), vertical = 10.dp.scaled()),
        horizontalArrangement = Arrangement.spacedBy(10.dp.scaled())
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = palette.danger, modifier = Modifier.size(16.dp.scaled()))
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
