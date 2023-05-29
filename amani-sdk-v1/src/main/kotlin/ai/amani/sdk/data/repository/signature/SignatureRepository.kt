package ai.amani.sdk.data.repository.signature

import ai.amani.sdk.model.UploadResultModel
import androidx.fragment.app.FragmentActivity

/**
 * @Author: zekiamani
 * @Date: 5.01.2023
 */
interface SignatureRepository {

    fun uploadSignature(
        onComplete: (uploadResultModel: UploadResultModel) -> Unit
    )
}