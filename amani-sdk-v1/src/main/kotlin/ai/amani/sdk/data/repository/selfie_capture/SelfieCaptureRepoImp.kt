package ai.amani.sdk.data.repository.selfie_capture

import ai.amani.sdk.data.mapper.UploadResultModelMapper
import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.IUploadCallBack
import androidx.fragment.app.FragmentActivity
import datamanager.model.customer.Errors

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
class SelfieCaptureRepoImp : SelfieCaptureRepository {

    override fun uploadManualSelfie(
        activity: FragmentActivity,
        docType: String?,
        onStart: () -> Unit,
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    ) {
        var uploadResultSelfie = UploadResultModel()

        runCatching{
            onStart.invoke()
            Amani.sharedInstance()
                .Selfie()
                .upload(
                    activity,
                    docType!!
                ) { isSuccess ->
                    uploadResultSelfie = UploadResultModelMapper.map(
                        isSuccess
                    )

                    onComplete.invoke(uploadResultSelfie)
                }
        }.onFailure {
            uploadResultSelfie.throwable = it
            onComplete.invoke(uploadResultSelfie)
        }
    }

    override fun uploadAutoSelfie(
        activity: FragmentActivity,
        docType: String?,
        onStart: () -> Unit,
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    ) {
        var uploadResultSelfie = UploadResultModel()

        runCatching {
            onStart.invoke()
            Amani.sharedInstance().AutoSelfieCapture().upload(
                activity,
                docType!!
            ) { isSuccess ->
                uploadResultSelfie = UploadResultModelMapper.map(
                    isSuccess
                )

                onComplete.invoke(uploadResultSelfie)
            }
        }.onFailure {
            uploadResultSelfie.throwable = it
            onComplete.invoke(uploadResultSelfie)
        }
    }

    override fun uploadSelfiePoseEstimation(
        activity: FragmentActivity,
        docType: String?,
        onStart: () -> Unit,
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    ) {

        var uploadResultSelfie = UploadResultModel()

        runCatching{
            onStart.invoke()
            Amani.sharedInstance().SelfiePoseEstimation()
                .upload(
                    activity,
                    docType!!
                ) { isSuccess ->
                    uploadResultSelfie = UploadResultModelMapper.map(
                        isSuccess
                    )

                    onComplete.invoke(uploadResultSelfie)
                }
        }.onFailure {
            uploadResultSelfie.throwable = it
            onComplete.invoke(uploadResultSelfie)
        }
    }
}