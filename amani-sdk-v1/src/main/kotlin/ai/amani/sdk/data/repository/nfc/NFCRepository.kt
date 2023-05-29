package ai.amani.sdk.data.repository.nfc

import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.model.mrz.MRZResult
import androidx.fragment.app.FragmentActivity
import datamanager.model.customer.Errors

/**
 * @Author: zekiamani
 * @Date: 26.09.2022
 */
interface NFCRepository {

    fun upload(
        activity: FragmentActivity,
        type: String?,
        onComplete: (uploadResult: UploadResultModel) -> Unit
    )

    fun getMRZ(
        type: String,
        onComplete: (mrz: MRZResult) -> Unit,
        onError: (error: Errors) -> Unit
    )
}