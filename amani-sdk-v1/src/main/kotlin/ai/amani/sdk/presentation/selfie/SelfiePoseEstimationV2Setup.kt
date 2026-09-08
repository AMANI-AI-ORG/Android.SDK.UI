package ai.amani.sdk.presentation.selfie

import ai.amani.amani_sdk.R
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.modules.selfie.pose_estimation.SelfiePoseEstimation
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import datamanager.model.config.PoseEstimationV2Preparation
import timber.log.Timber

/**
 * Pose-estimation V2 artwork + preparation-screen wiring, shared by the V1 (Fragment) and V2
 * (Compose) selfie screens so both flows configure the Core SDK builder identically.
 *
 * Core SDK 3.21.5 removed the preparation *video* and made every piece of pose-V2 artwork
 * host-supplied: it plays whatever `preparationAnimation(...)` / `processingAnimation(...)`
 * are given and silently skips what is missing (no preparation animation → no preparation
 * screen; no processing animation → the overlay falls back to its own spinning arrow; no face
 * guide → no icon). This UI SDK therefore ships the artwork itself, and a host app only
 * overrides it through the [FeatureConfig] fields (see `AmaniSDKUI.setSelfiePoseEstimationV2*`).
 */
internal object SelfiePoseEstimationV2Setup {

    /** Full-size preparation animation: opaque, it is the only thing on that screen. */
    private const val PREPARATION_OPACITY = 1f

    /**
     * Centre hint + face guide during capture: drawn over the live camera preview, so they
     * stay slightly transparent and never hide the user's own face.
     */
    private const val PROCESSING_OPACITY = 0.8f

    /** Disc painted behind the head on the preparation screen when config carries no color. */
    private const val DEFAULT_DISC_COLOR = "#FFFFFF"

    /** Head-rotation Lottie bundled with this SDK; carries both rotation directions. */
    @RawRes
    private val BUNDLED_ANIMATION = R.raw.animation_pose_head_rotation

    /** Front pose of the same head, shown while the user holds still. */
    @DrawableRes
    private val BUNDLED_FACE_GUIDE = R.drawable.ic_pose_head_straight

    @RawRes
    fun preparationAnimation(featureConfig: FeatureConfig): Int =
        featureConfig.selfiePoseEstimationV2PreparationAnimation ?: BUNDLED_ANIMATION

    @RawRes
    fun processingAnimation(featureConfig: FeatureConfig): Int =
        featureConfig.selfiePoseEstimationV2ProcessingAnimation ?: BUNDLED_ANIMATION

    @DrawableRes
    fun faceGuideDrawable(featureConfig: FeatureConfig): Int =
        featureConfig.selfiePoseEstimationV2FaceGuideDrawable ?: BUNDLED_FACE_GUIDE

    /**
     * Applies both animations and — when the step's server config carries a
     * [PoseEstimationV2Preparation] block — the preparation screen itself.
     *
     * The preparation screen stays server-gated (v1 parity: no `poseEstimationV2Preparation`,
     * no preparation step), while the capture-screen hint and the face guide are always
     * configured: they are the flow's own guidance, not an optional extra screen.
     *
     * @param preparation the step version's preparation block, or `null` to skip that screen.
     * @param featureConfig the host app's artwork overrides.
     * @param discColorHex disc behind the head on the preparation screen; falls back to the
     *   config overlay color, then white.
     */
    fun apply(
        builder: SelfiePoseEstimation.V2Builder,
        preparation: PoseEstimationV2Preparation?,
        featureConfig: FeatureConfig,
        discColorHex: String? = null
    ): SelfiePoseEstimation.V2Builder {
        // Capture screen: rotation hint + the icon that precedes it. Both share one opacity.
        builder.processingAnimation(
            animation = processingAnimation(featureConfig),
            faceGuideDrawable = faceGuideDrawable(featureConfig),
            opacity = PROCESSING_OPACITY
        )

        if (preparation == null) return builder

        val overlay = hexOrNull(preparation.overlayColor)
        builder.showPreparationScreenWithHexColors(
            message = preparation.message,
            buttonText = preparation.buttonText,
            buttonTextColor = hexOrNull(preparation.buttonTextColor),
            buttonBackgroundColor = hexOrNull(preparation.buttonBackgroundColor),
            buttonRadiusDp = PREPARATION_BUTTON_RADIUS_DP,
            overlayColor = overlay
        )
        builder.preparationAnimation(
            animation = preparationAnimation(featureConfig),
            backgroundColor = hexOrNull(discColorHex) ?: overlay ?: DEFAULT_DISC_COLOR,
            opacity = PREPARATION_OPACITY
        )
        return builder
    }

    /** Corner radius of the preparation start button (matches the V2 design's pill buttons). */
    private const val PREPARATION_BUTTON_RADIUS_DP = 28f

    /**
     * The Core SDK throws on an unparseable hex string, and these values are server content —
     * so a malformed color is dropped here and the Core default is used instead of crashing
     * the selfie step.
     */
    private fun hexOrNull(hex: String?): String? {
        val value = hex?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            Color.parseColor(value)
            value
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Pose estimation V2: unparseable color '%s', using SDK default", value)
            null
        }
    }
}
