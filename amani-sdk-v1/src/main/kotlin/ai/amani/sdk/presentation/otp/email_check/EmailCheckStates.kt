package ai.amani.sdk.presentation.otp.email_check

import androidx.annotation.StringRes

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
sealed class EmailCheckStates {
    object Empty: EmailCheckStates()
    object Loading: EmailCheckStates()

    object Success: EmailCheckStates()

    data class InvalidOTP(val message: String): EmailCheckStates()

    data class SnackMessage(val message: String): EmailCheckStates()

    data class BackPressAvailability(val available: Boolean): EmailCheckStates()
}