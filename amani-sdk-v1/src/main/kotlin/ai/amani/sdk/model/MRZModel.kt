package ai.amani.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 28.09.2022
 */
@Parcelize
data class MRZModel(
    var birthDate: String = "",
    var expireDate: String = "",
    var docNumber: String = ""
): Parcelable
