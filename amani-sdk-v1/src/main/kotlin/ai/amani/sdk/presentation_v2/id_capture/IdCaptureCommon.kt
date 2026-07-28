package ai.amani.sdk.presentation_v2.id_capture

import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

/**
 * Shared pieces of the V2 ID-capture leg. The capture is split into two screens —
 * [IdCaptureFrontScreen] and [IdCaptureBackScreen] — each carrying its own SDK setup.
 * This file holds only what both sides share: the toolbar state, the preview placeholder,
 * and the Activity lookup helper.
 */

/**
 * State backing the capture screens. Only [headerTitle] drives the static toolbar; the
 * capture area itself is the shared AmaniAi camera Fragment.
 */
data class IdCaptureUiState(
    val headerTitle: String
)

/**
 * Static stand-in for the camera, shown only in previews/inspection: a dark viewport with
 * a camera glyph and accent corner brackets, so the layout is reviewable without a device.
 */
@Composable
internal fun CaptureFramePlaceholder(modifier: Modifier = Modifier) {
    val palette = AmaniV2Theme.palette
    Box(
        modifier = modifier.background(palette.backgroundWarm),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = palette.inkLight,
            modifier = Modifier.height(40.dp)
        )
        Canvas(Modifier.fillMaxSize().padding(AmaniV2Dimens.screenPadding)) {
            val len = size.minDimension * 0.10f
            val stroke = 4.dp.toPx()
            val color = palette.accent
            // Four L-shaped corner brackets.
            drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, len), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, size.height), Offset(len, size.height), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - len), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke, StrokeCap.Round)
        }
    }
}

/** Unwraps the [FragmentActivity] backing a Compose [Context], or null if there isn't one. */
internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
