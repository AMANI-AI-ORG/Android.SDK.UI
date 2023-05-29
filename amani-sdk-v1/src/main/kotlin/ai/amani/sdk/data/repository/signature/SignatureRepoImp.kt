package ai.amani.sdk.data.repository.signature

import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.IUploadCallBack
import ai.amani.sdk.model.UploadResultModel
import datamanager.model.customer.Errors

/**
 * @Author: zekiamani
 * @Date: 5.01.2023
 */
class SignatureRepoImp: SignatureRepository {

    override fun uploadSignature(
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    ) {

        val uploadSignature = UploadResultModel()

        runCatching {
            Amani.sharedInstance().Signature()
                .upload(object : IUploadCallBack {
                    override fun cb(isSucess: Boolean, result: String?, errors: List<Errors?>?) {
                        onComplete.invoke(uploadSignature)
                    }
                })
        }.onFailure {
            uploadSignature.throwable = it
            onComplete.invoke(uploadSignature)
        }
    }
}
