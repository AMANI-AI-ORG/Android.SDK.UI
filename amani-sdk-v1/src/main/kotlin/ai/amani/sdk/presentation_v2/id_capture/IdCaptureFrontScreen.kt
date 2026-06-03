package ai.amani.sdk.presentation_v2.id_capture

import ai.amani.sdk.Amani
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.utils.AppConstant
import ai.amani.sdk.utils.BitmapUtils
import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import timber.log.Timber
import java.io.File

/**
 * Front-side document capture screen. Only the toolbar is statically drawn; everything
 * below it is the live AmaniAi capture Fragment (see [IdCaptureFrontCameraHost]),
 * positioned beneath the header. The front leg carries the full SDK setup (manual-crop
 * timeout + per-version hologram detection).
 *
 * In `@Preview`/inspection there is no Activity to host a Fragment, so a static framed
 * placeholder stands in for the camera.
 */
@Composable
fun IdCaptureFrontScreen(
    state: IdCaptureUiState,
    versionType: String,
    videoRecord: Boolean,
    hologramDetection: Boolean,
    onCaptured: (filePath: String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            if (LocalInspectionMode.current) {
                CaptureFramePlaceholder(Modifier.fillMaxSize())
            } else {
                IdCaptureFrontCameraHost(
                    versionType = versionType,
                    videoRecord = videoRecord,
                    hologramDetection = hologramDetection,
                    onCaptured = onCaptured,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Embeds the shared AmaniAi ID-capture camera for the FRONT side into Compose: sets the
 * fixed manual-crop timeout, the per-version video-record flag, and the per-version
 * hologram-detection flag, then `start(..., frontSide = true)`. The captured bitmap is
 * persisted as "frontSide" and its path handed back via [onCaptured].
 *
 * The Fragment is committed and removed with the composable's lifecycle so navigating away
 * (or retaking) tears the camera down cleanly.
 */
@Composable
private fun IdCaptureFrontCameraHost(
    versionType: String,
    videoRecord: Boolean,
    hologramDetection: Boolean,
    onCaptured: (filePath: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current.findFragmentActivity()
    val containerId = rememberSaveable { View.generateViewId() }
    val currentOnCaptured by rememberUpdatedState(onCaptured)

    AndroidView(
        modifier = modifier,
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } }
    )

    DisposableEffect(activity, versionType) {
        val fm = activity?.supportFragmentManager
        var captureFragment: androidx.fragment.app.Fragment? = null

        if (fm != null) {
            // Front-side SDK setup: fixed manual-crop timeout, video record, and the
            // per-version hologram-detection flag.
            Amani.sharedInstance().IDCapture().apply {
                setManualCropTimeOut(AppConstant.ID_CAPTURE_TIME_OUT)
                videoRecord(videoRecord)
                hologramDetection(hologramDetection)
            }

            val container = activity.findViewById<FrameLayout>(containerId)
            captureFragment = Amani.sharedInstance().IDCapture().start(
                activity,
                container,
                versionType,
                /* frontSide = */ true
            ) { bitmap: Bitmap?, _, _ ->
                activity.runOnUiThread {
                    if (bitmap != null) {
                        val file: File? = BitmapUtils.saveBitmapAsFile(bitmap, "frontSide", activity)
                        if (file != null) {
                            currentOnCaptured(file.absolutePath)
                        } else {
                            Timber.e("V2 ID capture (front): failed to persist captured bitmap")
                        }
                    }
                }
            }

            captureFragment?.let { frag ->
                fm.beginTransaction()
                    .replace(containerId, frag)
                    .commitAllowingStateLoss()
            } ?: Timber.e("V2 ID capture (front): SDK returned no capture fragment")
        }

        onDispose {
            val manager = activity?.supportFragmentManager
            val frag = captureFragment
            if (manager != null && frag != null && !manager.isStateSaved) {
                manager.beginTransaction().remove(frag).commitAllowingStateLoss()
            }
        }
    }
}

@Preview(name = "IdCapture — front", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewIdCaptureFront() {
    AmaniV2Theme {
        IdCaptureFrontScreen(
            state = IdCaptureUiState(headerTitle = "Verification"),
            versionType = "",
            videoRecord = false,
            hologramDetection = false,
            onCaptured = {}
        )
    }
}
