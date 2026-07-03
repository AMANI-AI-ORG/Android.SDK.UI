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
        imagePath: String?
    ): PreviewScreenUiState {
        val step = version.stepFor(side)
        return PreviewScreenUiState(
            headerTitle = step?.confirmationTitle.orFallback("Verification"),
            title = step?.confirmationTitle.orFallback("Is your ID clear and readable?"),
            description = step?.confirmationDescription.orFallback(
                "Check that all four corners are visible and there's no glare before continuing."
            ),
            confirmButtonText = (step?.confirm ?: general?.confirmText).orFallback("Looks good"),
            retakeButtonText = general?.tryAgainText.orFallback("Retake photo"),
            imagePath = imagePath,
            // TODO: config-driven
            qualityChecks = listOf(
                "Sharp & in focus",
                "Document fully visible",
                "No glare or shadows"
            )
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
