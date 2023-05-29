package ai.amani.sdk.presentation.selfie

/**
 * @Author: zekiamani
 * @Date: 10.10.2022
 */
sealed class SelfieCaptureUIState {

    object FirstAnimation: SelfieCaptureUIState()

    object SecondAnimation : SelfieCaptureUIState()

    data class Navigate(val navigateTo : SelfieType): SelfieCaptureUIState()

    object Empty: SelfieCaptureUIState()

}