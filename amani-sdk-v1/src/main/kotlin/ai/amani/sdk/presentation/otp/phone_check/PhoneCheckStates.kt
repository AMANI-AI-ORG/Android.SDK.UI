package ai.amani.sdk.presentation.otp.phone_check

import ai.amani.sdk.presentation.otp.email_check.EmailCheckStates
import ai.amani.sdk.presentation.otp.email_verify.EmailVerifyStates

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
sealed class PhoneCheckStates {
    object Empty: PhoneCheckStates()
    object Success: PhoneCheckStates()

    object Loading: PhoneCheckStates()

    object InvalidOTP: PhoneCheckStates()

    data class SnakeMessage(val message: String): PhoneCheckStates()

    data class BackPressAvailability(val available: Boolean): PhoneCheckStates()
}