package ai.amani.sdk.data.repository.id_capture

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
class IDCaptureRepoImp: IDCaptureRepository {

    override fun upload(
        activity: FragmentActivity,
        docType: String,
        onStart: () -> Unit,
        onComplete: (result: UploadResultModel) -> Unit) {

        var uploadResultModel = UploadResultModel()

        runCatching {
            onStart.invoke()
            Amani.sharedInstance()
                .IDCapture()
                .upload(
                    activity,
                    docType
                ) { isSucess ->
                    uploadResultModel = UploadResultModelMapper.map(
                        isSucess
                    )
                    onComplete.invoke(uploadResultModel)
                }
        }.onFailure {
            uploadResultModel.throwable = it
            onComplete.invoke(uploadResultModel)
        }
    }
}