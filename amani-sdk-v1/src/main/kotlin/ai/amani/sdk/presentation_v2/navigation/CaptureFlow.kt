package ai.amani.sdk.presentation_v2.navigation

import ai.amani.sdk.extentions.getStepConfig
import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import ai.amani.sdk.utils.AppConstant.STATUS_PENDING_REVIEW
import ai.amani.sdk.utils.AppConstant.STATUS_PROCESSING
import datamanager.model.config.Version

/**
 * Navigation logic for the V2 capture flow, reusing the shared SDK data layer
 * ([CachingHomeKYC], the [getStepConfig] extension) instead of re-implementing it.
 *
 * It mirrors three pieces of v1 behaviour without the Fragment/NavDirections coupling:
 *  - `HomeKYCViewModel.setVersionList` → [prepareVersions]
 *  - the single-vs-multi version branch in `HomeKYCViewModel.navigateScreen` → callers
 *    use [visibleVersions]/[initialCaptureFor]
 *  - the front/back side decision in `PreviewScreenViewModel.navigateScreen` →
 *    [resolveAfterConfirm]
 *
 * Kept free of Compose so it stays unit-testable; the nav host just calls these and
 * turns the results into [AmaniV2Navigator] moves.
 */
internal object CaptureFlow {

    private val DONE_STATUSES = setOf(STATUS_APPROVED, STATUS_PENDING_REVIEW, STATUS_PROCESSING)

    /** First KYC step still needing the user's action (mirrors v1 sequential unlock). */
    fun firstActionableRule(): Rule? =
        CachingHomeKYC.onlyKYCRules?.firstOrNull { it.status !in DONE_STATUSES }

    /**
     * Builds and caches the version list for [rule] from the step config, stamping each
     * version with its documentId/stepId. Mirrors `HomeKYCViewModel.setVersionList`.
     */
    fun prepareVersions(rule: Rule): List<Version> {
        val config = CachingHomeKYC.appConfig ?: return emptyList()
        val sortOrder = rule.sortOrder ?: return emptyList()
        val stepConfig = config.getStepConfig(sortOrder)
        val versions = mutableListOf<Version>()
        stepConfig.mDocuments?.forEach { documentList ->
            documentList?.versions?.forEach { version ->
                version.documentId = documentList.id.toString()
                version.stepId = sortOrder
            }
            documentList?.versions?.let { versions.addAll(it) }
        }
        CachingHomeKYC.versionsList = versions
        return versions
    }

    /** The selectable (non-hidden) versions of the currently prepared step. */
    fun visibleVersions(): List<Version> =
        CachingHomeKYC.versionsList.orEmpty().filter { it.isHidden == false || it.isHidden == null }

    fun versionByType(versionType: String): Version? =
        CachingHomeKYC.versionsList.orEmpty().firstOrNull { it.type == versionType }

    /**
     * Initial capture destination for a chosen [version]. Only the photo-ID documents
     * route into the V2 capture flow (matching v1's SelectDocumentType → IDFrontSide);
     * other document kinds (physical contract, etc.) have no V2 screen yet.
     */
    fun initialCaptureFor(version: Version): AmaniV2Destination? {
        val type = version.type ?: return null
        return when (version.documentId) {
            AmaniDocumentTypes.IDENTIFICATION,
            AmaniDocumentTypes.PASSPORT,
            AmaniDocumentTypes.DRIVING_LICENSE ->
                AmaniV2Destination.Capture(type, CaptureSide.Front)

            else -> null // TODO(wiring): physical contract / other document V2 screens
        }
    }

    /**
     * Where to go after the user confirms a captured side. The core "skip the back side"
     * rule: a two-sided document (`version.steps.size > 1`) whose front was just confirmed
     * advances to the back-side capture; anything else (single-sided, or the back already
     * done) finishes the capture leg — represented by `null` (the host pops to Home).
     *
     * NOTE(wiring): v1 also branches into the NFC scan here when the version enables NFC.
     * That screen doesn't exist in V2 yet, so it's deferred.
     */
    fun resolveAfterConfirm(version: Version, side: CaptureSide): AmaniV2Destination? {
        val isTwoSided = (version.steps?.size ?: 0) > 1
        return if (side == CaptureSide.Front && isTwoSided) {
            AmaniV2Destination.Capture(version.type ?: return null, CaptureSide.Back)
        } else {
            null
        }
    }
}
