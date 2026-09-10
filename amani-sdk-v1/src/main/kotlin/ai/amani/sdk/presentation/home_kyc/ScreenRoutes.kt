package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.DocumentSource

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
    object NFCScanScreen: ScreenRoutes
    object SignatureScreen: ScreenRoutes
    object PhysicalContractScreen: ScreenRoutes

    /**
     * No screen: the document of a `documentSource` that picks straight from storage is picked
     * right where the flow would have opened the physical contract capture screen.
     */
    data class DocumentPickerScreen(val source: DocumentSource): ScreenRoutes

    /**
     * Information screen shown before a `gallery` document is picked, carrying the version's
     * `basicInfoText` and the button that opens the gallery.
     */
    object DocumentInfoScreen: ScreenRoutes

    /**
     * Speech verification (document id "ST"). Hosts the OPTIONAL standalone
     * AmaniSpeechVerifier module (a `compileOnly` dependency, not bundled). Unlike the other
     * routes it has no nav-graph destination — HomeKYCFragment commits the module's fragment
     * over the nav host and drives its upload directly.
     */
    object SpeechVerifierScreen: ScreenRoutes
}