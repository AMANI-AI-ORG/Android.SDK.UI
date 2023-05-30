package ai.amani.sdk.presentation.physical_contract_screen

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 13.03.2023
 */
@Parcelize
sealed class GenericDocumentFlow : Parcelable{
    object DataFromCamera: GenericDocumentFlow()
    data class DataFromGallery(val dataList : ArrayList<Uri>): GenericDocumentFlow()
}