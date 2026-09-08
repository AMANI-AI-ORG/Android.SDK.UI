package ai.amani.sdk.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import kotlinx.parcelize.Parcelize

/**
 * @Author: @zekiamani
 * @Date: 7.12.2023
 */
@Parcelize
data class FeatureConfig(
    var selfieCaptureVideoRecord: Boolean? = null,
    var idCaptureVideoRecord: Boolean? = null,
    var idCaptureHologramDetection: Boolean? = null,
    @Deprecated(
        "Core SDK 3.21.5 replaced the pose-estimation V2 preparation video with a Lottie " +
            "animation. Use selfiePoseEstimationV2PreparationAnimation instead; this field is ignored."
    )
    var selfiePoseEstimationV2PreparationVideo: Int? = null,
    /**
     * Overrides the Lottie animation played full size on the pose-estimation V2 preparation
     * screen. `null` keeps the animation bundled with this UI SDK
     * (`R.raw.animation_pose_head_rotation`).
     *
     * The file must carry `clockwise` / `counterclockwise` markers and a transparent
     * background — the same asset also plays over the live camera feed.
     */
    @RawRes var selfiePoseEstimationV2PreparationAnimation: Int? = null,
    /**
     * Overrides the Lottie animation played small, in the middle of the camera circle, while
     * the user rotates their head. `null` keeps the bundled animation.
     */
    @RawRes var selfiePoseEstimationV2ProcessingAnimation: Int? = null,
    /**
     * Overrides the icon shown in the camera circle while the user holds their face straight,
     * before the rotation starts. `null` keeps the bundled
     * `R.drawable.ic_pose_head_straight`.
     */
    @DrawableRes var selfiePoseEstimationV2FaceGuideDrawable: Int? = null,
    var uiStyle: UIStyle = UIStyle.V1
): Parcelable
