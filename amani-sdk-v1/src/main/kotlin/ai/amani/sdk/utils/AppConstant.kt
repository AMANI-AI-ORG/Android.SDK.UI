package ai.amani.sdk.utils

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
object AppConstant {
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_PROCESSING = "PROCESSING"
    const val STATUS_NOT_UPLOADED = "NOT_UPLOADED"
    const val STATUS_PENDING_REVIEW = "PENDING_REVIEW"
    const val STATUS_AUTOMATICALLY_REJECTED = "AUTOMATICALLY_REJECTED"
    const val ID_CAPTURE_TIME_OUT = 15
    const val SIGNATURE_NUMBER = 1
    const val REGISTER_CONFIG = "register_config"
    const val KYC_RESULT = "kyc_result"
    const val IDENTIFIER_PROFILE_INFO = "profile_info"
    const val IDENTIFIER_PHONE_OTP = "phone_otp"
    const val IDENTIFIER_EMAIL_OTP = "email_otp"
    const val IDENTIFIER_QUESTIONNAIRE = "questionnaire"
    const val FEATURE_CONFIG = "feature_config"


    /**
     * Holds the list of identifier names of the steps that will not shown as button.
     * Instead of that, the identifiers will navigated to its own screen directly.
     */
    val STEPS_BEFORE_KYC_FLOW = listOf(IDENTIFIER_PROFILE_INFO,IDENTIFIER_PHONE_OTP,IDENTIFIER_EMAIL_OTP, IDENTIFIER_QUESTIONNAIRE)
}