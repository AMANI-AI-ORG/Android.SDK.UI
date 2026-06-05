package ai.amani.sdk.presentation_v2.selfie_capture

import ai.amani.sdk.presentation_v2.preview_screen.PreviewScreenUiState
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

/**
 * Maps a selfie [Version] into the stateless UI states of the V2 selfie screens. The
 * selfie counterpart of CaptureMapper: the per-step strings drive the chrome, and every
 * string is config-driven with a static fallback. Selfies are single-sided, so only
 * `steps[0]` is consulted (mirroring v1 SelfieCaptureFragment.toolBar()).
 */
internal object SelfieMapper {

    fun toSelfieCaptureState(version: Version): SelfieCaptureUiState {
        val step = version.steps?.firstOrNull()
        return SelfieCaptureUiState(
            headerTitle = step?.captureTitle.orFallback("Verification")
        )
    }

    fun toPreviewScreenState(
        version: Version,
        general: GeneralConfigs?,
        imagePath: String?
    ): PreviewScreenUiState {
        val step = version.steps?.firstOrNull()
        return PreviewScreenUiState(
            headerTitle = step?.confirmationTitle.orFallback("Verification"),
            title = step?.confirmationTitle.orFallback("Is your selfie clear?"),
            description = step?.confirmationDescription.orFallback(
                "Make sure your face is well lit and fully visible before continuing."
            ),
            confirmButtonText = (step?.confirm ?: general?.confirmText).orFallback("Looks good"),
            retakeButtonText = general?.tryAgainText.orFallback("Retake selfie"),
            imagePath = imagePath
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
