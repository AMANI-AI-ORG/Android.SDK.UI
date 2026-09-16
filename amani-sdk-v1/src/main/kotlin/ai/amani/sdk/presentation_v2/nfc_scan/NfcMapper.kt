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

        val animationStates = NfcV2AnimationCopy.statesFor(version)

        return NfcTexts(
            headerTitle = version.nfcTitle.orFallback("NFC"),
            title = version.nfcPleaseHold.orFallback("Hold your ID to your phone"),
            descriptions = descriptions,
            // v2 searching text wins when present; the v1 dialog title stays the fallback.
            searchingLabel = version.v2NfcSearchingText.nonBlank()
                ?: version.nfcDialogTitle.orFallback("Searching for chip..."),
            cancelButtonText = (version.cancelButtonText ?: general?.tryAgainText).orFallback("Cancel"),
            continueButtonText = general?.continueText.orFallback("Start scan"),
            // TODO(config): no server field for the chip-data header yet — add one and read it
            //  here instead of the static fallback.
            mrzCheckHeaderTitle = version.nfcConfigureTitle.orFallback("Chip data"),
            // TODO(config): no server field for the "read from chip" eyebrow yet.
            mrzCheckEyebrow = "Read from chip",
            mrzCheckTitle = version.nfcConfigureTitle.orFallback("Check your details"),
            mrzCheckDescription = version.nfcFailedDescription.orFallback(
                "This was read securely from your ID's chip. Confirm it matches your document."
            ),
            // TODO(config): no server field for the footer hint yet.
            mrzCheckHint = "These values were read from your ID. " +
                "Tap any field to correct it if something looks wrong.",
            mrzConfirmButtonText = (general?.confirmText ?: general?.continueText)
                .orFallback("Confirm"),
            birthDateLabel = version.documentDateOfBirth.orFallback("Date of birth"),
            expiryDateLabel = version.documentDateOfExpiry.orFallback("Date of expiry"),
            documentNoLabel = version.documentNoTitle.orFallback("Document number"),
            modalReadyTitle = version.nfcTitle.orFallback("Ready to scan"),
            modalReadyDescription = descriptions.first(),
            modalScanningTitle = version.nfcDialogTitle.orFallback("Scanning…"),
            modalScanningDescription = version.nfcDialogDescription.orFallback("Keep the document steady."),
            modalFailedText = version.nfcFailed.orFallback("Couldn't read the chip. Try again."),
            // Same "read complete" copy the animation ends on (nfcV2.animationStates.success).
            modalDoneText = animationStates["success"].orFallback("Chip verified"),
            enableNfcHeader = version.enableNfcHeader.orFallback("Turn on NFC"),
            enableNfcDescription = version.enableNfcDescription.orFallback(
                "NFC is off. Turn it on to scan your document's chip."
            ),
            enableNfcButton = general?.tryAgainText.orFallback("Open settings"),
            animationColorHex = version.nfcAnimationColor.nonBlank(),
            animationStates = animationStates,
            animationHint = version.nfcV2?.animationHint
                .orFallback("Follow the instructions in the animation below")
        )
    }

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

    private fun String?.orFallback(fallback: String): String = nonBlank() ?: fallback
}
