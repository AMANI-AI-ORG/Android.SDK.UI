package ai.amani.sdk.presentation_v2.select_document_type

import ai.amani.sdk.extentions.getStepConfig
import ai.amani.sdk.presentation_v2.components.DotStep
import ai.amani.sdk.utils.AmaniDocumentTypes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector
import datamanager.model.config.ResGetConfig
import datamanager.model.config.Version

/**
 * Maps the selected KYC step's server [Version] list + GeneralConfigs into the
 * stateless [SelectDocumentTypeUiState]. Every string is config-driven with a static
 * fallback; the option [DocumentTypeOption.id] is the version `type`, which the
 * navigation layer maps back to the chosen [Version].
 */
internal object SelectDocumentTypeMapper {

    fun toUiState(
        versions: List<Version>,
        config: ResGetConfig?,
        ruleTitle: String? = null
    ): SelectDocumentTypeUiState {
        val general = config?.generalConfigs
        // documentSelection* strings live on the step config; all versions share a stepId.
        val stepConfig = versions.firstOrNull()?.stepId?.let { config?.getStepConfig(it) }

        val options = versions.map { version ->
            DocumentTypeOption(
                id = version.type ?: "",
                title = version.title.orFallback("Document"),
                subtitle = version.informationScreenDesc1.orEmpty(),
                // Key off the document type code; documentId may carry the numeric id at this stage.
                icon = iconFor(version.type ?: version.documentId),
                // Chips on the selected card (design v2.6): the NFC pitch only when this
                // document actually enables NFC (CaptureFlow.isNfcEnabled's check), plus the
                // per-document estimated duration.
                nfcChipLabel = if (version.nfcAndroid ?: version.nfc) {
                    version.v2NfcChipLabel.orFallback("Fastest with NFC")
                } else null,
                estimatedTime = version.v2EstimatedTime.orFallback("~30 sec")
            )
        }

        return SelectDocumentTypeUiState(
            // Nav title is the KYC step's name ("Identification"); the in-content heading
            // below carries the documentSelection* strings.
            headerTitle = ruleTitle.orFallback(
                stepConfig?.documentSelectionTitle.orFallback("Verification")
            ),
            dots = emptyList<DotStep>(),
            eyebrow = "",
            title = stepConfig?.documentSelectionTitle.orFallback("Which document will you use?"),
            subtitle = stepConfig?.documentSelectionDescription
                .orFallback("Choose a government-issued ID. Make sure it's valid and not expired."),
            options = options
        )
    }

    /** Document-type code → icon. Falls back to a generic card glyph. */
    private fun iconFor(typeCode: String?): ImageVector = when (typeCode) {
        AmaniDocumentTypes.IDENTIFICATION -> Icons.Outlined.Badge
        AmaniDocumentTypes.PASSPORT -> Icons.AutoMirrored.Outlined.MenuBook
        AmaniDocumentTypes.DRIVING_LICENSE -> Icons.Outlined.DirectionsCar
        AmaniDocumentTypes.VISA -> Icons.AutoMirrored.Outlined.MenuBook
        else -> Icons.Outlined.CreditCard
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
