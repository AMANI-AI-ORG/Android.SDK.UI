package ai.amani.sdk.presentation_v2.id_capture

import ai.amani.sdk.presentation_v2.navigation.CaptureSide
import ai.amani.sdk.presentation_v2.preview_screen.PreviewScreenUiState
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

/**
 * Maps a chosen [Version] + the side being captured into the stateless UI states of the
 * V2 capture screens. The v2 counterpart of the chrome v1 sets in
 * IDCaptureFront/BackSideFrag.toolBar() and PreviewScreenFragment.setToolBarTitle():
 * the per-step strings ([Step.captureTitle]/[Step.confirmationTitle]/…) drive both legs,
 * and only the step index changes between front and back.
 *
 * Step index follows v1: [CaptureSide.Front] → `steps[0]`, [CaptureSide.Back] → `steps[1]`.
 * Every string is config-driven with a static fallback so a partial config still renders.
 */
internal object CaptureMapper {

    private fun Version.stepFor(side: CaptureSide) =
        steps?.getOrNull(if (side == CaptureSide.Back) 1 else 0)

    fun toIdCaptureState(
        version: Version,
        side: CaptureSide
    ): IdCaptureUiState {
        val step = version.stepFor(side)
        // Only the toolbar is statically drawn; the area below is the live AmaniAi camera,
        // so the capture screen needs nothing more than the per-side title.
        return IdCaptureUiState(
            headerTitle = step?.captureTitle.orFallback("Verification")
        )
    }

    fun toPreviewScreenState(
        version: Version,
        side: CaptureSide,
        general: GeneralConfigs?,
        bitmap: android.graphics.Bitmap?
    ): PreviewScreenUiState {
        val step = version.stepFor(side)
        return PreviewScreenUiState(
            // The v2 confirmation strings live on the Version; the per-side step strings
            // remain the fallback so older configs still render.
            headerTitle = version.v2DocumentConfirmationNavTitle.orFallback(
                step?.confirmationTitle.orFallback("Review your ID")
            ),
            title = version.v2DocumentConfirmationHeader.orFallback("Looks good?"),
            description = version.v2DocumentConfirmationSubtitle.orFallback(
                step?.confirmationDescription.orFallback(
                    "Make sure all text is sharp and fully visible."
                )
            ),
            confirmButtonText = (step?.confirm ?: general?.confirmText).orFallback("Looks good"),
            retakeButtonText = general?.tryAgainText.orFallback("Retake photo"),
            bitmap = bitmap,
            qualityChecks = listOfNotNull(
                version.v2DocumentQuality1.nonBlank() ?: "Sharp & in focus",
                version.v2DocumentQuality2.nonBlank() ?: "Document fully visible",
                version.v2DocumentQuality3.nonBlank() ?: "No glare or shadows"
            ),
            qualityChecksHeader = version.v2DocumentQualityHeader.orFallback("ID QUALITY CHECKS")
        )
    }

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
