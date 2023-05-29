package ai.amani.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author: zekiamani
 * @Date: 13.10.2022
 */
@Parcelize
data class RegisterConfig(
    val tcNumber: String,
    val token: String,
    val language: String = "tr",
    val location: Boolean = true,
    val userFullName: String? = null,
    val userEmail: String? = null,
    val userPhoneNumber: String? = null,
    val birthDate: String? = null,
    val expireDate: String? = null,
    val documentNumber: String? = null
): Parcelable
