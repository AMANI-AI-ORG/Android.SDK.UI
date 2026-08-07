package ai.amani.sdk.presentation_v2.nfc_scan

import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

/**
 * Maps a NFC-enabled [Version] (+ app [GeneralConfigs]) into the config-driven [NfcTexts]
 * that back the V2 NFC screen. Mirrors the strings v1 NFCScanFragment reads from the same
 * config (nfcTitle / nfcDescription* / nfcDialog* / nfcFailed / documentDate* / …), each
 * with a fallback so a missing key never blanks the UI.
 */
internal object NfcMapper {

    fun texts(version: Version, general: GeneralConfigs?): NfcTexts {
        val descriptions = listOfNotNull(
            version.nfcDescription1.nonBlank(),
            version.nfcDescription2.nonBlank(),
            version.nfcDescription3.nonBlank()
        ).ifEmpty {
            listOf("Place the top back of your phone on the chip side of your ID and hold it still.")
        }

        return NfcTexts(
            headerTitle = version.nfcTitle.orFallback("NFC"),
            title = version.nfcPleaseHold.orFallback("Hold your ID to your phone"),
            descriptions = descriptions,
            // v2 searching text wins when present; the v1 dialog title stays the fallback.
            searchingLabel = version.v2NfcSearchingText.nonBlank()
                ?: version.nfcDialogTitle.orFallback("Searching for chip..."),
            cancelButtonText = (version.cancelButtonText ?: general?.tryAgainText).orFallback("Cancel"),
            continueButtonText = general?.continueText.orFallback("Start scan"),
            mrzCheckTitle = version.nfcConfigureTitle.orFallback("Check your document details"),
            mrzCheckDescription = version.nfcFailedDescription.orFallback("We couldn't read the chip. Confirm these values and try again."),
            birthDateLabel = version.documentDateOfBirth.orFallback("Date of birth"),
            expiryDateLabel = version.documentDateOfExpiry.orFallback("Date of expiry"),
            documentNoLabel = version.documentNoTitle.orFallback("Document number"),
            modalReadyTitle = version.nfcTitle.orFallback("Ready to scan"),
            modalReadyDescription = descriptions.first(),
            modalScanningTitle = version.nfcDialogTitle.orFallback("Scanning…"),
            modalScanningDescription = version.nfcDialogDescription.orFallback("Keep the document steady."),
            modalFailedText = version.nfcFailed.orFallback("Couldn't read the chip. Try again."),
            // TODO: config-driven
            modalDoneText = "Chip verified",
            enableNfcHeader = version.enableNfcHeader.orFallback("Turn on NFC"),
            enableNfcDescription = version.enableNfcDescription.orFallback(
                "NFC is off. Turn it on to scan your document's chip."
            ),
            enableNfcButton = general?.tryAgainText.orFallback("Open settings"),
            animationColorHex = version.nfcAnimationColor.nonBlank()
        )
    }

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

    private fun String?.orFallback(fallback: String): String = nonBlank() ?: fallback
}
