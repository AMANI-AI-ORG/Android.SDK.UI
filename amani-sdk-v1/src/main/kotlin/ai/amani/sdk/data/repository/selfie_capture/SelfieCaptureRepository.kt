package ai.amani.sdk.data.repository.selfie_capture

import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.interfaces.IUploadCallBack
import androidx.fragment.app.FragmentActivity

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
interface SelfieCaptureRepository {

    fun uploadManualSelfie (
        activity: FragmentActivity,
        docType: String?,
        onStart: () -> Unit,
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    )

    fun uploadAutoSelfie(
        activity: FragmentActivity,
        docType: String?,
        onStart: () -> Unit,
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    )

    fun uploadSelfiePoseEstimation(
        activity: FragmentActivity,
        docType: String?,
        onStart: () -> Unit,
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    )
}