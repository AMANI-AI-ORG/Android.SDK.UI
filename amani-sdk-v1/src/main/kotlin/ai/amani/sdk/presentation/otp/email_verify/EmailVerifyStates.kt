package ai.amani.sdk.presentation.otp.email_verify

import android.provider.ContactsContract.CommonDataKinds.Email
import androidx.annotation.StringRes

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
sealed class EmailVerifyStates {
    object Empty: EmailVerifyStates()
    object Success: EmailVerifyStates()

    object Loading: EmailVerifyStates()

    data class InvalidEmail(val message: String): EmailVerifyStates()

    data class SnakeMessage(val message: String): EmailVerifyStates()
}