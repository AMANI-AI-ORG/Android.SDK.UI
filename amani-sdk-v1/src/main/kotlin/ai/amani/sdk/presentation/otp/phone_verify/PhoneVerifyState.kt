package ai.amani.sdk.presentation.otp.phone_verify

/**
 * @Author: @zekiamani
 * @Date: 22.12.2023
 */
sealed class PhoneVerifyState {

    object Empty: PhoneVerifyState()
    object Success: PhoneVerifyState()

    object Loading: PhoneVerifyState()

    data class InvalidPhoneNumber(val message: String? = "Phone number is invalid"): PhoneVerifyState()

    data class SnakeMessage(val message: String): PhoneVerifyState()
}