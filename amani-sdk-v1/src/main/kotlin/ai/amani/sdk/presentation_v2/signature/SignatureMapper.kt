package ai.amani.sdk.presentation_v2.signature

import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

/**
 * Maps a signature [Version] (+ app [GeneralConfigs]) into the stateless [SignatureUiState].
 * Mirrors the strings v1's SignatureFragment reads from the same config: the toolbar title is
 * `steps[0].captureTitle` and the two buttons are the shared confirm / try-again texts.
 * Every value is config-driven with a fallback so a partial config still renders.
 */
internal object SignatureMapper {

    fun toUiState(version: Version, general: GeneralConfigs?): SignatureUiState {
        val step = version.steps?.firstOrNull()
        return SignatureUiState(
            headerTitle = step?.captureTitle.orFallback("Sign the contract"),
            instruction = (step?.captureDescription ?: version.informationScreenDesc1)
                .orFallback("Sign inside the box below, then confirm."),
            confirmButtonText = general?.confirmText.orFallback("Confirm"),
            tryAgainButtonText = general?.tryAgainText.orFallback("Try again")
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
