package ai.amani.sdk.presentation.otp.questions

/**
 * @Author: @zekiamani
 * @Date: 15.01.2024
 */
sealed class QuestionnaireState {
    object Loading: QuestionnaireState()

    object Success: QuestionnaireState()
    object Empty: QuestionnaireState()

    data class SubmitButtonClickable(val clickable: Boolean): QuestionnaireState()

    data class Error(val message: String): QuestionnaireState()
}