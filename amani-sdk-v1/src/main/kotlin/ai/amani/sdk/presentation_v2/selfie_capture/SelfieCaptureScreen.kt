package ai.amani.sdk.presentation_v2.selfie_capture

import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.IFragmentCallBack
import ai.amani.sdk.modules.selfie.pose_estimation.observable.OnFailurePoseEstimation
import ai.amani.sdk.modules.selfie.pose_estimation.observable.PoseEstimationObserver
import ai.amani.sdk.presentation.selfie.SelfieType
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.id_capture.CaptureFramePlaceholder
import ai.amani.sdk.presentation_v2.id_capture.findFragmentActivity
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import datamanager.model.config.Version
import timber.log.Timber
import java.io.File

/**
 * State backing [SelfieCaptureScreen]. Only [headerTitle] drives the static toolbar; the
 * area below is the live AmaniAi selfie camera.
 */
data class SelfieCaptureUiState(
    val headerTitle: String
)

/**
 * Selfie capture screen. Only the toolbar is statically drawn; everything below it is the
 * live AmaniAi selfie Fragment (see [SelfieCameraHost]). The concrete variant (auto,
 * manual, pose estimation, pose estimation V2) is resolved from the [version] via
 * [SelfieTypeResolver], mirroring v1's SelfieCaptureFragment.
 *
 * In `@Preview`/inspection there is no Activity to host a Fragment, so a static framed
 * placeholder stands in for the camera.
 */
@Composable
fun SelfieCaptureScreen(
    state: SelfieCaptureUiState,
    version: Version,
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
                SelfieCameraHost(
                    version = version,
                    onCaptured = onCaptured,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Embeds the shared AmaniAi selfie camera into Compose. Resolves the selfie variant from
 * [version] and starts the matching SDK flow (the same calls v1's SelfieCaptureFragment
 * makes), committing the returned Fragment into the hosted container. The captured frame
 * is persisted and its path handed back via [onCaptured].
 *
 * The Fragment is committed and removed with the composable's lifecycle so navigating away
 * (or retaking) tears the camera down cleanly.
 */
@Composable
private fun SelfieCameraHost(
    version: Version,
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

    DisposableEffect(activity, version.type) {
        val fm = activity?.supportFragmentManager
        var selfieFragment: Fragment? = null

        if (fm != null && activity != null) {
            // Persist the captured frame (or use the file the SDK already wrote) and hand
            // the absolute path back, marshalled onto the UI thread.
            val deliver: (Bitmap?, File?) -> Unit = { bitmap, file ->
                activity.runOnUiThread {
                    val path = file?.absolutePath
                        ?: bitmap?.let { BitmapUtils.saveBitmapAsFile(it, "selfie", activity)?.absolutePath }
                    if (path != null) currentOnCaptured(path)
                    else Timber.e("V2 selfie capture: failed to persist captured frame")
                }
            }

            selfieFragment = createSelfieFragment(
                activity = activity,
                containerId = containerId,
                version = version,
                deliver = deliver
            )

            selfieFragment?.let { frag ->
                fm.beginTransaction()
                    .replace(containerId, frag)
                    .commitAllowingStateLoss()
            } ?: Timber.e("V2 selfie capture: SDK returned no selfie fragment")
        }

        onDispose {
            val manager = activity?.supportFragmentManager
            val frag = selfieFragment
            if (manager != null && frag != null && !manager.isStateSaved) {
                manager.beginTransaction().remove(frag).commitAllowingStateLoss()
            }
        }
    }
}

/**
 * Builds the AmaniAi selfie Fragment for the variant resolved from [version], replicating
 * the per-type SDK setup of v1's SelfieCaptureFragment (auto / manual / pose estimation /
 * pose estimation V2). The capture step id is the literal `XXX_SE_0` v1 passes; the real
 * `version.type` is used only on upload.
 */
@OptIn(ai.amani.base.annotiations.AmaniExperimental::class)
private fun createSelfieFragment(
    activity: FragmentActivity,
    containerId: Int,
    version: Version,
    deliver: (Bitmap?, File?) -> Unit
): Fragment? {
    val videoRecord = version.videoRecord
    val poseObserver = object : PoseEstimationObserver {
        override fun onSuccess(bitmap: Bitmap?) = deliver(bitmap, null)
        override fun onFailure(reason: OnFailurePoseEstimation, currentAttempt: Int) {}
        override fun onError(error: Error) {}
    }

    return when (SelfieTypeResolver.resolve(version)) {
        SelfieType.Manual -> Amani.sharedInstance().Selfie().start(
            "XXX_SE_0",
            object : IFragmentCallBack {
                override fun cb(bitmap: Bitmap?, isDestroyed: Boolean?, file: File?) {
                    deliver(bitmap, file)
                }
            }
        )

        SelfieType.Auto -> {
            Amani.sharedInstance().AutoSelfieCapture().setCustomUI(
                ai.amani.R.color.white,
                20,
                ai.amani.R.color.white,
                true,
                100,
                10,
                version.faceIsTooFarText ?: "Close enough",
                version.faceNotInsideText ?: "Face not found",
                version.holdStableText ?: "Please hold stable",
                version.selfieAlertDescription ?: "Failed",
                ai.amani.R.color.white,
                ai.amani.R.color.approve_green
            )
            val container = activity.findViewById<FrameLayout>(containerId)
            Amani.sharedInstance().AutoSelfieCapture().start(
                "XXX_SE_0",
                null,
                container,
                object : IFragmentCallBack {
                    override fun cb(bitmap: Bitmap?, manualButtonActivated: Boolean?, file: File?) {
                        deliver(bitmap, file)
                    }
                }
            )
        }

        is SelfieType.PoseEstimation -> Amani.sharedInstance().SelfiePoseEstimation()
            .Builder()
            .requestedPoseNumber(version.selfieType ?: 1)
            .ovalViewAnimationDurationMilSec(500)
            .videoRecord(videoRecord = videoRecord)
            .observe(poseObserver)
            .userInterfaceColors(
                ai.amani.R.color.white,
                ai.amani.R.color.approve_green,
                ai.amani.R.color.error_red,
                ai.amani.R.color.color_black,
                ai.amani.R.color.color_black,
                ai.amani.R.color.color_black,
                ai.amani.R.color.white,
                ai.amani.R.color.white
            )
            .userInterfaceTexts(
                faceNotInside = version.faceNotInsideText ?: "Your face is not inside the area",
                faceNotStraight = version.faceNotStraightText ?: "Your face is not straight",
                faceIsTooFar = version.faceIsTooFarText ?: "Your face is too far from camera",
                holdPhoneVertically = version.holdStableText ?: "Please keep straight the phone",
                alertTitle = version.selfieAlertTitle ?: "Verification Failed",
                alertDescription = version.selfieAlertDescription ?: "Failed",
                alertTryAgain = version.selfieAlertTryAgain ?: "Try Again",
                turnLeft = version.turnLeftText ?: "Turn left",
                turnRight = version.turnRightText ?: "Turn right",
                turnUp = version.turnUpText ?: "Turn up",
                turnDown = version.turnDownText ?: "Turn down",
                faceStraight = version.keepStraightText ?: "Look straight"
            )
            .build(activity)

        SelfieType.PoseEstimationV2 -> Amani.sharedInstance().SelfiePoseEstimation()
            .BuilderV2()
            .userInterfaceColors(
                overlayBackgroundColor = ai.amani.R.color.white,
                appFontColor = ai.amani.R.color.color_black
            )
            .userInterfaceTexts(
                faceStraight = version.keepStraightText,
                turnLeft = version.turnLeftText,
                turnRight = version.turnRightText,
                faceNotInside = version.faceNotInsideText,
                faceTooFar = version.faceIsTooFarText,
                holdPhoneVertically = version.holdStableText,
                alertTitle = version.selfieAlertTitle,
                alertDescription = version.selfieAlertDescription,
                alertTryAgain = version.selfieAlertTryAgain
            )
            .videoRecord(videoRecord = videoRecord)
            .ovalViewAnimationDurationMilSec(500)
            .observe(poseObserver)
            .build(activity)

        else -> null
    }
}
