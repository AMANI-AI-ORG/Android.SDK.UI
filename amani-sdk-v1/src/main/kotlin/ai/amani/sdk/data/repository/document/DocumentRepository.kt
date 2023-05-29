package ai.amani.sdk.data.repository.document

import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.modules.document.FileWithType
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity

/**
 * @Author: zekiamani
 * @Date: 1.03.2023
 */
interface DocumentRepository {

    @WorkerThread
    fun upload(
        activity: FragmentActivity,
        docType: String,
        onStart: () -> Unit,
        onComplete: (result: UploadResultModel) -> Unit,
        genericDocumentFlow: GenericDocumentFlow = GenericDocumentFlow.DataFromCamera
        )
}