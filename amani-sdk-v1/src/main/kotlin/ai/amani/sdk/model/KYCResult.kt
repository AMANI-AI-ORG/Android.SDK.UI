package ai.amani.sdk.model

import ai.amani.base.util.JSONConvertable
import ai.amani.sdk.utils.ProfileStatus
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 9.12.2022
 */
@Parcelize
data class KYCResult(
    val errorCode: Int? = null,
    val generalException: Throwable? = null,
    val profileStatus: String = ProfileStatus.INCOMPLETE
):Parcelable, JSONConvertable


