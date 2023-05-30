package ai.amani.sdk.presentation.selfie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 10.10.2022
 */
@Parcelize
sealed class SelfieType : Parcelable {

    /** SelfieType is AutoSelfie that represents to take selfie automatically with-out capture button*/
    object Auto : SelfieType()

    /** Selfie type is Manual that represent to take selfie manually with capture button*/
    object Manual : SelfieType()

    /** Selfie type is PoseEstimation that represent to take selfie automatically after requested
     * facial pose order finalized correctly by user to verify user is real/fake*/
    data class PoseEstimation(var requestedOrderNumber: Int) : SelfieType()

    object Unknown : SelfieType()
}
