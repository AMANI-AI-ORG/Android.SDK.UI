package ai.amani.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 9.12.2022
 */
@Parcelize
data class ProfileInfoModel(
    var mrzModel: MRZModel? = null,
    var registerConfig: RegisterConfig? = null
): Parcelable
