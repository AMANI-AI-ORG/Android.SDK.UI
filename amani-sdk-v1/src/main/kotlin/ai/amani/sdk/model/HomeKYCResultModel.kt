package ai.amani.sdk.model

import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import ai.amani.sdk.presentation.selfie.SelfieType
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 5.10.2022
 */
@Parcelize
data class HomeKYCResultModel(
    val docID: String = "",
    val docType: String = "",
    val selfieType: SelfieType = SelfieType.Manual,
    val nfcOnly: Boolean = false,
    val genericDocumentFlow: GenericDocumentFlow = GenericDocumentFlow.DataFromCamera
):Parcelable
