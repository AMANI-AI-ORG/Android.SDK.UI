package ai.amani.sdk.data.repository.document

import ai.amani.sdk.Amani
import ai.amani.sdk.data.mapper.UploadResultModelMapper
import ai.amani.sdk.interfaces.IUploadCallBack
import ai.amani.sdk.model.UploadResultModel
import ai.amani.sdk.modules.document.FileWithType
import ai.amani.sdk.modules.document.interfaces.IDocumentCallBack
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import androidx.fragment.app.FragmentActivity
import datamanager.model.customer.Errors

/**
 * @Author: zekiamani
 * @Date: 1.03.2023
 */
class DocumentRepoImp: DocumentRepository {

    override fun upload(
        activity: FragmentActivity,
        docType: String,
        onStart: () -> Unit,
        onComplete: (result: UploadResultModel) -> Unit,
        genericDocumentFlow: GenericDocumentFlow
    ) {
        var uploadResultModel: UploadResultModel

        onStart.invoke()

        when (genericDocumentFlow) {
            is GenericDocumentFlow.DataFromCamera -> {
                Amani.sharedInstance().Document().upload(
                    activity,
                    docType,
                    object : IUploadCallBack {
                        override fun cb(isSucess: Boolean) {
                            uploadResultModel = UploadResultModelMapper.map(
                                isSucess
                            )
                            onComplete.invoke(uploadResultModel)
                        }
                    }
                )
            }

            is GenericDocumentFlow.DataFromGallery -> {

                val docList = arrayListOf<FileWithType>()
                val pickedUri = genericDocumentFlow.dataList.first()
                val fileData = activity.contentResolver.openInputStream(pickedUri)
                    ?.use { it.readBytes() }
                // A picked file is not always a PDF: the `documentSource` config also allows an
                // image from the gallery, so the real type is read from the content provider.
                val mimeType = activity.contentResolver.getType(pickedUri) ?: "application/pdf"
                docList.add(FileWithType(fileData!!, mimeType))

                Amani.sharedInstance().Document().upload(
                    activity,
                    docType,
                    docList,
                    object : IUploadCallBack {
                        override fun cb(isSucess: Boolean) {
                            uploadResultModel = UploadResultModelMapper.map(
                                isSucess)

                            onComplete.invoke(uploadResultModel)
                        }
                    }
                )
            }
        }
    }
}