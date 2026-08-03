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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Height of the dot row at the top of each step column. The connectors share this height so
 * their center line lands exactly on the dots, independent of the (scaled) label beneath.
 */
private val DotRowHeight = 14.dp

enum class StepStatus { Completed, Current, Rejected, Pending }

data class DotStep(
    val label: String,
    val status: StepStatus
)

/**
 * Horizontal dot progress indicator with connectors. Works on light surfaces by
 * default; pass [onDark] for the camera/selfie screens.
 */
@Composable
fun DotProgressBar(
    steps: List<DotStep>,
    modifier: Modifier = Modifier,
    onDark: Boolean = false
) {
    val palette = AmaniV2Theme.palette
    fun connectorColor(completed: Boolean): Color = when {
        completed -> palette.accent
        onDark -> Color.White.copy(alpha = 0.2f)
        else -> AmaniV2Colors.ConnectorIdle
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Each step owns a weight(1f) column with its dot centered and the connector drawn as
        // two half-lines *behind* the dot (left half + right half). Because the columns are
        // adjacent, the right half of one column meets the left half of the next exactly under
        // the dots — so the line runs continuously from dot to dot (iOS parity), instead of the
        // old short segment that left a gap before each dot. A segment is "completed"-colored
        // when the step to its LEFT is completed (matching the previous per-connector logic).
        steps.forEachIndexed { idx, step ->
            DotItem(
                step = step,
                onDark = onDark,
                // No line to the left of the first dot / right of the last dot.
                leftColor = if (idx == 0) null else connectorColor(steps[idx - 1].status == StepStatus.Completed),
                rightColor = if (idx == steps.lastIndex) null else connectorColor(step.status == StepStatus.Completed),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DotItem(
    step: DotStep,
    onDark: Boolean,
    leftColor: Color?,
    rightColor: Color?,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DotRowHeight),
            contentAlignment = Alignment.Center
        ) {
            // Connector line behind the dot: left half + right half, each filling to the column
            // edge so it meets the neighbouring column's half exactly under the dots.
            Row(
                modifier = Modifier.matchParentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(leftColor ?: Color.Transparent)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(rightColor ?: Color.Transparent)
                )
            }
            when (step.status) {
                StepStatus.Completed -> Dot(10.dp, palette.accent)
                StepStatus.Rejected -> Dot(10.dp, palette.danger)
                // Single solid dot — no inner/outer halo. The dot takes the "inner" color
                // (the brand accent) directly, so current reads as one clean filled circle.
                StepStatus.Current -> Dot(10.dp, palette.accent)
                StepStatus.Pending -> {
                    val bg = if (onDark) Color.White.copy(alpha = 0.1f) else AmaniV2Colors.DotIdle
                    val border = if (onDark) Color.White.copy(alpha = 0.3f) else AmaniV2Colors.DotIdleBorder
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .border(1.5.dp, border, CircleShape)
                    )
                }
            }
        }
        val labelColor = when (step.status) {
            StepStatus.Completed -> if (onDark) Color.White.copy(alpha = 0.85f) else palette.ink
            StepStatus.Current -> if (onDark) Color.White else palette.accent
            StepStatus.Rejected -> palette.danger
            StepStatus.Pending -> if (onDark) Color.White.copy(alpha = 0.5f) else palette.inkLight
        }
        val weight = if (step.status == StepStatus.Pending) FontWeight.Normal else FontWeight.Medium
        // Multi-word labels wrap word-by-word (e.g. "Customer Confirmation" → "Customer" /
        // "Confirmation"), up to 3 lines; anything longer then ellipsizes. Fills the column
        // width so wrapping has room and the text stays centered under the dot.
        Text(
            step.label,
            style = AmaniV2Type.label.copy(fontWeight = weight).scaled(),
            color = labelColor,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Dot(size: androidx.compose.ui.unit.Dp, color: Color, haloColor: Color? = null) {
    Box(contentAlignment = Alignment.Center) {
        if (haloColor != null) {
            Box(
                modifier = Modifier
                    .size(size + 8.dp)
                    .clip(CircleShape)
                    .background(haloColor)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}
