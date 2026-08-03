package ai.amani.sdk.presentation_v2.selfie_capture

import ai.amani.sdk.presentation_v2.navigation.SelfieGuideStep
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

    /**
     * Builds the pre-selfie guide state (V2 redesign of the legacy selfie instruction
     * animations) for the given [step]. The header reuses the per-step
     * [captureTitle][datamanager.model.config.Step.captureTitle]; the instructional copy is
     * static for now.
     *
     * TODO(copy/config): these guide strings are hardcoded English placeholders. The
     *  [SelfieGuideStep.First] copy comes straight from the redesign; the
     *  [SelfieGuideStep.Second] (pose-estimation) copy is a stand-in until design provides it.
     *  Both should become config-driven (like the other `v2Document…` fields) — colors
     *  already resolve from config via the theme palette.
     */
    fun toGuideState(version: Version, step: SelfieGuideStep): SelfieGuideUiState {
        val stepConfig = version.steps?.firstOrNull()
        val headerTitle = stepConfig?.captureTitle.orFallback("Selfie")
        return if (step == SelfieGuideStep.First) {
            SelfieGuideUiState(
                headerTitle = headerTitle,
                // TODO(copy): from redesign — make config-driven.
                title = "Let's take your selfie",
                description = "Look straight at the camera and keep your face centered in the frame.",
                checklistHeader = "Before you start",
                checklistItems = listOf(
                    "Good, even lighting on your face",
                    "Remove glasses, hats, or masks",
                    "Hold the phone at eye level"
                ),
                buttonText = "Open camera"
            )
        } else {
            SelfieGuideUiState(
                headerTitle = headerTitle,
                // TODO(copy): pose-estimation guide copy not in design yet — placeholder.
                title = "Follow the movements",
                description = "You'll be asked to turn your head to a few positions. Keep your face inside the frame the whole time.",
                checklistHeader = "Before you start",
                checklistItems = listOf(
                    "Stay in a well-lit area",
                    "Keep your whole face visible",
                    "Move slowly when prompted"
                ),
                buttonText = "Open camera"
            )
        }
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
