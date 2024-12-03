package ai.amani.sdk.data.repository.nfc

import ai.amani.sdk.Amani
import ai.amani.sdk.data.mapper.UploadResultModelMapper
import ai.amani.sdk.interfaces.INfcCallBack
import ai.amani.sdk.interfaces.IUploadCallBack
import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.mrz.MRZResult
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.nfc.Tag
import androidx.fragment.app.FragmentActivity
import datamanager.model.autocrop.Mrz
import datamanager.model.customer.Errors
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 26.09.2022
 */
class NFCRepositoryImp: NFCRepository {

    override fun upload(
        activity: FragmentActivity,
        type: String?,
        onComplete: (uploadResult: UploadResultModel) -> Unit
    ) {

        var uploadResultModel: UploadResultModel

        Amani.sharedInstance().ScanNFC().upload(
            activity,
            type!!
        ) { isSucess ->
            uploadResultModel = UploadResultModelMapper.map(
                isSucess
            )

            onComplete.invoke(uploadResultModel)
        }
    }

    override fun getMRZ(
        type: String,
        onComplete: (mrz: MRZResult) -> Unit,
        onError: (error : AmaniError) -> Unit
    ) {
        Amani.sharedInstance().IDCapture().getMRZ(
            type = type,
            onComplete = {
                Timber.d("Birthdate: ${it.mRZBirthDate}")
                Timber.d("Expirydate: ${it.mRZExpiryDate}")
                Timber.d("DocumentNumber: ${it.mRZDocumentNumber}")
                onComplete.invoke(it)
            },
            onError = {
                onError.invoke(it)
            }
        )
    }

    fun scan(
        tag: Tag,
        context: Context,
        birthDate: String,
        expireDate: String,
        documentNumber: String,
        onComplete: () -> Unit,
        onFailure: () -> Unit
    ) {
        Amani.sharedInstance().ScanNFC().start(
            tag,
            context,
            birthDate,
            expireDate,
            documentNumber
        ) { _, isSuccess, _ ->
            if (isSuccess) onComplete.invoke()
            else onFailure.invoke()
        }
    }
}