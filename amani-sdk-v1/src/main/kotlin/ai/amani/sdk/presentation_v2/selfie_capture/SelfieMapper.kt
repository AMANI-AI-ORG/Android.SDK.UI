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
     * Builds the pre-selfie guide state for the given [step]. Config-driven from the selfie
     * [Version]'s shared `v2Guide…` fields ([SelfieGuideStep.First] → primary, `Second` → pose),
     * with the built-in redesign copy as fallback. Header/button come from [general].
     */
    fun toGuideState(
        version: Version,
        step: SelfieGuideStep,
        general: GeneralConfigs?
    ): SelfieGuideUiState {
        val headerTitle = version.steps?.firstOrNull()?.captureTitle
            .orFallback(general?.v2SelfieText.orFallback("Selfie"))
        val button = general?.v2OpenCameraButtonText.orFallback("Open camera")
        val checklistHeader = version.v2GuideChecklistHeader.orFallback("Before you start")
        return if (step == SelfieGuideStep.First) {
            SelfieGuideUiState(
                headerTitle = headerTitle,
                title = version.v2GuideTitle.orFallback("Let's take your selfie"),
                description = version.v2GuideDescription
                    .orFallback("Look straight at the camera and keep your face centered in the frame."),
                checklistHeader = checklistHeader,
                checklistItems = listOf(
                    version.v2GuideCheck1.orFallback("Good, even lighting on your face"),
                    version.v2GuideCheck2.orFallback("Remove glasses, hats, or masks"),
                    version.v2GuideCheck3.orFallback("Hold the phone at eye level")
                ),
                buttonText = button
            )
        } else {
            SelfieGuideUiState(
                headerTitle = headerTitle,
                title = version.v2GuideSecondTitle.orFallback("Follow the movements"),
                description = version.v2GuideSecondDescription
                    .orFallback("You'll be asked to turn your head to a few positions. Keep your face inside the frame the whole time."),
                checklistHeader = checklistHeader,
                checklistItems = listOf(
                    version.v2GuideSecondCheck1.orFallback("Stay in a well-lit area"),
                    version.v2GuideSecondCheck2.orFallback("Keep your whole face visible"),
                    version.v2GuideSecondCheck3.orFallback("Move slowly when prompted")
                ),
                buttonText = button
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
