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
        bitmap: android.graphics.Bitmap?
    ): PreviewScreenUiState {
        val step = version.steps?.firstOrNull()
        return PreviewScreenUiState(
            // The v2 confirmation strings live on the Version; the step strings remain the
            // fallback so older configs still render.
            headerTitle = version.v2DocumentConfirmationNavTitle.orFallback(
                step?.confirmationTitle.orFallback("Review your selfie")
            ),
            title = version.v2DocumentConfirmationHeader.orFallback("Looks good?"),
            description = version.v2DocumentConfirmationSubtitle.orFallback(
                step?.confirmationDescription.orFallback(
                    "Make sure your face is clear and well illuminated."
                )
            ),
            confirmButtonText = (step?.confirm ?: general?.confirmText).orFallback("Looks good"),
            retakeButtonText = general?.tryAgainText.orFallback("Retake selfie"),
            bitmap = bitmap,
            qualityChecks = listOfNotNull(
                version.v2DocumentQuality1.nonBlank() ?: "Face clearly visible",
                version.v2DocumentQuality2.nonBlank() ?: "Well lit, no harsh shadows",
                version.v2DocumentQuality3.nonBlank() ?: "Eyes open and looking forward"
            ),
            qualityChecksHeader = version.v2DocumentQualityHeader.orFallback("SELFIE QUALITY CHECKS")
        )
    }

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
