package ai.amani.sdk.presentation.otp.profile_info

import android.provider.ContactsContract.Profile

/**
 * @Author: @zekiamani
 * @Date: 10.01.2024
 */
sealed class ProfileInfoState {

    object Empty: ProfileInfoState()

    object Loading: ProfileInfoState()
    object NameIsEmpty: ProfileInfoState()
    object SurnameIsEmpty: ProfileInfoState()
    object BirthdateIsEmpty: ProfileInfoState()
    data class Error(val message: String): ProfileInfoState()
}