package ai.amani.sdk.presentation_v2.address_verify

import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

/**
 * Maps an address-document [Version] (+ app [GeneralConfigs]) into the stateless
 * [AddressVerifyUiState]. Mirrors the strings v1's PhysicalContractFragment reads from the
 * same config: the toolbar title is `steps[0].captureTitle`, and the capture fragment's
 * retry/confirm labels come from the shared config texts (v1 hardcoded these two).
 * Every value is config-driven with a fallback so a partial config still renders.
 */
internal object AddressVerifyMapper {

    fun toUiState(version: Version, general: GeneralConfigs?): AddressVerifyUiState {
        val step = version.steps?.firstOrNull()
        return AddressVerifyUiState(
            headerTitle = step?.captureTitle.orFallback("Verify address"),
            instruction = (step?.captureDescription ?: version.informationScreenDesc1)
                .orFallback("Photograph your document, or upload it as a PDF."),
            // v1 exposes PDF picking via a bare toolbar icon (no config text exists for it).
            // TODO: config-driven
            uploadPdfButtonText = "Upload PDF instead",
            tryAgainText = general?.tryAgainText.orFallback("Try again"),
            confirmText = general?.continueText.orFallback("Continue")
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
