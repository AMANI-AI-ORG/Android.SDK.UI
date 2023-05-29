package ai.amani.sdk.presentation.home_kyc

/**
 * @Author: zekiamani
 * @Date: 12.09.2022
 */
sealed interface ScreenRoutes{
    object SelectDocumentTypeScreen: ScreenRoutes
    object IDFrontSideScreen: ScreenRoutes
    object IDBackSideScreen: ScreenRoutes
    object SelfieCaptureScreen: ScreenRoutes
    object HomeKYCScreen: ScreenRoutes
    object PreviewScreen: ScreenRoutes
    object NFCScanScreen: ScreenRoutes
    object SignatureScreen: ScreenRoutes
    object PhysicalContractScreen: ScreenRoutes
}