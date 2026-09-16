package ai.amani.sdk.presentation_v2.document_info

import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

/**
 * State backing [DocumentInfoScreen]. Every string is config-driven with a fallback, so a
 * partial config still renders a usable screen.
 */
data class DocumentInfoUiState(
    val headerTitle: String,
    val headline: String,
    val details: String,
    val buttonText: String
)

/**
 * Maps a `gallery` document [Version] (+ app [GeneralConfigs]) into [DocumentInfoUiState],
 * reading exactly the fields the V1 information screen reads: the headline from
 * `basicInfoText`, the details from the step's `captureDescription`, the button from
 * `captureDocumentText` and the header from the step's `captureTitle`.
 */
internal object DocumentInfoMapper {

    fun toUiState(version: Version, general: GeneralConfigs?): DocumentInfoUiState {
        val step = version.steps?.firstOrNull()
        return DocumentInfoUiState(
            headerTitle = step?.captureTitle.orFallback("Upload document"),
            headline = version.basicInfoText.orFallback("Upload your document to continue."),
            details = step?.captureDescription.orEmpty().trim(),
            // GeneralConfigs has no generic upload label, so a document without
            // `captureDocumentText` falls back to the general continue label.
            buttonText = version.captureDocumentText
                .orFallback(general?.continueText.orFallback("Continue"))
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
