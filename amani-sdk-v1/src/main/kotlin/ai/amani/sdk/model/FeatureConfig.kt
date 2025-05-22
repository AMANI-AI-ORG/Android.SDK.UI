package ai.amani.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: @zekiamani
 * @Date: 7.12.2023
 */
@Parcelize
data class FeatureConfig(
    var selfieCaptureVideoRecord: Boolean? = null,
    var idCaptureVideoRecord: Boolean? = null,
    var idCaptureHologramDetection: Boolean? = null
): Parcelable
