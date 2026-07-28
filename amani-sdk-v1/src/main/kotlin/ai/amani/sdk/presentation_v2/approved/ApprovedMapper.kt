package ai.amani.sdk.presentation_v2.approved

import ai.amani.sdk.model.customer.Rule
import datamanager.model.config.GeneralConfigs

/**
 * Maps the app [GeneralConfigs] (+ the completed KYC rules) into the stateless
 * [ApprovedUiState]. Config-driven where a GeneralConfigs field exists — the same
 * success* fields v1's CongratulationsFragment reads — with fallbacks; the remaining
 * copy has no config source yet and is static.
 */
internal object ApprovedMapper {

    fun toUiState(general: GeneralConfigs?, rules: List<Rule>?): ApprovedUiState =
        ApprovedUiState(
            headerTitle = general?.successTitle.orFallback("Verification"),
            // TODO: config-driven
            badgeText = "Approved instantly",
            title = general?.successHeaderText.orFallback("You're verified"),
            subtitle = general?.successInfo1Text.orFallback(
                "Your account is ready to use. Welcome aboard."
            ),
            // TODO: config-driven
            cardTitle = "All checks passed",
            cardSubtitle = general?.successInfo2Text.orFallback(
                "Document, biometric, and chip verified"
            ),
            buttonText = general?.continueText.orFallback("Continue to app"),
            iconColorHex = general?.successIconColor?.takeIf { it.isNotBlank() },
            stepLabels = rules.orEmpty().mapNotNull { it.title?.takeIf { t -> t.isNotBlank() } }
        )

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
