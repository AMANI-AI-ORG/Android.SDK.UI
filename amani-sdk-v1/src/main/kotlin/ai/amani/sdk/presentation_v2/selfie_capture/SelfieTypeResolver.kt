package ai.amani.sdk.presentation_v2.selfie_capture

import ai.amani.sdk.presentation.selfie.SelfieType
import datamanager.model.config.Version

/**
 * Resolves which selfie variant a step uses from its server [Version], isolating the
 * decision in one place. Mirrors v1's `SelfieCaptureViewModel.initialViewState`:
 * `version.selfieType` is an Int code from the config —
 *   0  → Auto (auto-capture, no shutter button)
 *  -1  → Manual (capture button)
 *  -2  → Pose Estimation V2 (continuous 360° head-rotation liveness)
 *   n  → Pose Estimation (requesting the n-th facial pose order)
 *
 * The capture screen and the upload dispatch both read the same resolution, so the
 * variant choice never diverges between mounting the camera and uploading the result.
 */
internal object SelfieTypeResolver {

    fun resolve(version: Version): SelfieType = when (val code = version.selfieType) {
        0 -> SelfieType.Auto
        -1 -> SelfieType.Manual
        -2 -> SelfieType.PoseEstimationV2
        null -> SelfieType.Manual
        else -> SelfieType.PoseEstimation(code)
    }
}
