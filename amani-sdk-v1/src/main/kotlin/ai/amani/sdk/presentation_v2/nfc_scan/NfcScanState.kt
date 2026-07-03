package ai.amani.sdk.presentation_v2.nfc_scan

import ai.amani.sdk.model.MRZModel

/**
 * Config-driven strings for the V2 NFC leg. Every value is resolved from the server
 * [datamanager.model.config.Version] / [datamanager.model.config.GeneralConfigs] in
 * [NfcMapper], each with a sensible fallback — the screen never hardcodes copy.
 *
 * [animationColorHex] is the optional `nfcAnimationColor` from config; the screen parses
 * it and tints the pulsing rings / chip badge with it, falling back to the brand accent.
 */
data class NfcTexts(
    val headerTitle: String,
    val title: String,
    val descriptions: List<String>,
    val searchingLabel: String,
    val cancelButtonText: String,
    val continueButtonText: String,
    // MRZ correction (v1 ShowMRZCheck)
    val mrzCheckTitle: String,
    val mrzCheckDescription: String,
    val birthDateLabel: String,
    val expiryDateLabel: String,
    val documentNoLabel: String,
    // Scanning modal (v1 NFCScanningBottomDialog)
    val modalReadyTitle: String,
    val modalReadyDescription: String,
    val modalScanningTitle: String,
    val modalScanningDescription: String,
    val modalFailedText: String,
    val modalDoneText: String,
    // NFC-disabled system prompt (v1 alertDialog → NFC settings)
    val enableNfcHeader: String,
    val enableNfcDescription: String,
    val enableNfcButton: String,
    val animationColorHex: String?
)

/**
 * Which face of the NFC screen is shown. Mirrors v1 NFCScanFragment's two layouts:
 *  - [FetchingMrz]: reading the MRZ off the just-captured ID (progress).
 *  - [ReadyToScan]: the HTML "hold your ID to your phone" screen with the pulsing rings.
 *  - [MrzCheck]: the editable MRZ fields shown after a misread (v1 `ShowMRZCheck`).
 */
enum class NfcPhase { FetchingMrz, ReadyToScan, MrzCheck }

/**
 * Scanning bottom-sheet modal phases — the V2 counterpart of v1 NFCScanningBottomDialog.
 * The user taps Start on the main screen to open the modal ([Waiting], reader armed); when
 * the ID chip touches the phone it switches to [Scanning] (dots sweep left→right) and then
 * [Done] / [Error].
 */
enum class NfcModalPhase { Waiting, Scanning, Error, Done }

/**
 * Immutable UI state for [NfcScanScreen]. [modal] is null while the bottom modal is
 * hidden; [nfcDisabled] drives the enable-NFC dialog (device NFC is off).
 */
data class NfcScanUiState(
    val phase: NfcPhase,
    val mrz: MRZModel,
    val texts: NfcTexts,
    val modal: NfcModalPhase? = null,
    val nfcDisabled: Boolean = false
)

/** One-shot effects from [NfcScanViewModel]. */
sealed interface NfcScanEffect {
    /**
     * The NFC leg is over. [success] true → the chip was read, so the host uploads the
     * ID *and* NFC together (v1 `IDCapture().withNFC(true)`); false → out of attempts, so
     * the host uploads the ID only (`withNFC(false)`).
     */
    data class Finished(val success: Boolean) : NfcScanEffect

    /** User asked to turn NFC on — open the system NFC settings screen. */
    data object OpenNfcSettings : NfcScanEffect
}
