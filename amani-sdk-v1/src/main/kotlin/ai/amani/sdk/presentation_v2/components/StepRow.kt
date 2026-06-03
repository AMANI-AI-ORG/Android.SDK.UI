package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Colors
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class StepRowStatus { Done, Active, Locked, Rejected }

data class StepError(val title: String?, val message: String)

data class VerificationStep(
    val index: Int,
    val title: String,
    // Nullable: blank/null means the row shows no status subtitle line.
    val subtitle: String?,
    val status: StepRowStatus,
    val error: StepError? = null
)

/**
 * A single verification step card. Renders the four states from the HTML
 * (done / active / locked / rejected), with an optional inline error block.
 */
@Composable
fun StepRow(
    step: VerificationStep,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val rejected = step.status == StepRowStatus.Rejected
    val active = step.status == StepRowStatus.Active

    val containerColor = when (step.status) {
        StepRowStatus.Active -> palette.accentSofter
        StepRowStatus.Rejected -> Color(0xFFFEF2F2)
        else -> palette.surface
    }
    val borderColor = when (step.status) {
        StepRowStatus.Active -> palette.accent
        StepRowStatus.Rejected -> palette.danger
        else -> palette.border
    }
    val borderWidth = if (active || rejected) 1.5.dp else 0.5.dp
    val rowAlpha = if (step.status == StepRowStatus.Locked) 0.6f else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .background(containerColor, RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepBadge(step.status, step.index)
            Column(modifier = Modifier.weight(1f)) {
                Text(step.title, style = AmaniV2Type.body.copy(fontWeight = FontWeight.Medium), color = palette.ink)
                step.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    val subtitleColor = when (step.status) {
                        StepRowStatus.Done, StepRowStatus.Active -> palette.accent
                        StepRowStatus.Rejected -> palette.danger
                        StepRowStatus.Locked -> palette.inkLight
                    }
                    val subtitleWeight = if (step.status == StepRowStatus.Locked) FontWeight.Normal else FontWeight.Medium
                    Text(
                        subtitle,
                        style = AmaniV2Type.label.copy(fontWeight = subtitleWeight),
                        color = subtitleColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            when (step.status) {
                StepRowStatus.Active -> Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                StepRowStatus.Locked -> Icon(Icons.Filled.Lock, null, tint = palette.inkLight, modifier = Modifier.size(14.dp))
                StepRowStatus.Rejected -> Icon(Icons.Filled.Warning, null, tint = palette.danger, modifier = Modifier.size(18.dp))
                StepRowStatus.Done -> {}
            }
        }

        if (step.error != null) {
            InlineError(step.error)
        }
    }
}

@Composable
private fun StepBadge(status: StepRowStatus, index: Int) {
    val palette = AmaniV2Theme.palette
    val (bg, fg) = when (status) {
        StepRowStatus.Done, StepRowStatus.Active -> palette.accent to palette.surface
        StepRowStatus.Rejected -> palette.danger to palette.surface
        StepRowStatus.Locked -> AmaniV2Colors.BgWarm to palette.inkMuted
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(bg, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            StepRowStatus.Done -> Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(16.dp))
            StepRowStatus.Rejected -> Icon(Icons.Filled.Close, null, tint = fg, modifier = Modifier.size(18.dp))
            else -> Text("$index", style = AmaniV2Type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = fg)
        }
    }
}

@Composable
private fun InlineError(error: StepError) {
    val palette = AmaniV2Theme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(palette.surface, RoundedCornerShape(10.dp))
            .border(0.5.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = palette.danger, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            val hasTitle = !error.title.isNullOrBlank()
            if (hasTitle) {
                Text(error.title!!, style = AmaniV2Type.caption.copy(fontWeight = FontWeight.Medium), color = palette.ink)
            }
            Text(
                error.message,
                style = AmaniV2Type.caption,
                color = palette.inkMuted,
                modifier = Modifier.padding(top = if (hasTitle) 3.dp else 0.dp)
            )
        }
    }
}
