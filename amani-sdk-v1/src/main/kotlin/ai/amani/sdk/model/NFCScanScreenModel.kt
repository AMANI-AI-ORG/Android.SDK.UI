package ai.amani.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 28.09.2022
 */
@Parcelize
data class NFCScanScreenModel(
    val configModel: ConfigModel,
    val mrzModel: MRZModel,
    val nfcOnly: Boolean = false // false means processing with NFC + ID together
                                 // true means that processing with onlyNFC without ID, this state
                                 // will be affect also HomeKYCScreen upload state
): Parcelable

