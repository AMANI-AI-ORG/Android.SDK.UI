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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { idx, step ->
            DotItem(step = step, onDark = onDark, modifier = Modifier.weight(1f))
            if (idx < steps.lastIndex) {
                val filled = step.status == StepStatus.Completed
                val lineColor = when {
                    filled -> palette.accent
                    onDark -> Color.White.copy(alpha = 0.2f)
                    else -> AmaniV2Colors.ConnectorIdle
                }
                // The connector occupies the same height as the dot row and centers the line
                // within it, so it sits between the dots — never over the labels below.
                Box(
                    modifier = Modifier
                        .width(28.dp.scaled())
                        .height(DotRowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(lineColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun DotItem(
    step: DotStep,
    onDark: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.height(DotRowHeight),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StepStatus.Completed -> Dot(10.dp, palette.accent)
                StepStatus.Rejected -> Dot(10.dp, palette.danger)
                StepStatus.Current -> Dot(
                    size = 10.dp,
                    color = palette.accent,
                    haloColor = if (onDark) palette.accent.copy(alpha = 0.3f) else palette.accentSoft
                )
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
        Text(step.label, style = AmaniV2Type.label.copy(fontWeight = weight).scaled(), color = labelColor)
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
