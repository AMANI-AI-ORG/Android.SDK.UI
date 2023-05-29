package ai.amani.sdk.data.repository.id_capture

import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.interfaces.IUploadCallBack
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Flow

/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */

interface IDCaptureRepository {

    @WorkerThread
    fun upload(
        activity: FragmentActivity,
        docType: String,
        onStart: () -> Unit,
        onComplete: (result: UploadResultModel) -> Unit,
    )
}