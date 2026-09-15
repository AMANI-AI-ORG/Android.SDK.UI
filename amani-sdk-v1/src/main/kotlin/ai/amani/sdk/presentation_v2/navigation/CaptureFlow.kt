package ai.amani.sdk.presentation_v2.navigation

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation.selfie.SelfieType
import ai.amani.sdk.presentation_v2.selfie_capture.SelfieTypeResolver
import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.utils.AmaniDocumentTypes
import datamanager.model.config.Version
import timber.log.Timber

/**
 * Navigation logic for the V2 capture flow, reusing the shared SDK data layer
 * ([CachingHomeKYC]) instead of re-implementing it.
 *
 * It mirrors two pieces of v1 behaviour without the Fragment/NavDirections coupling:
 *  - `HomeKYCViewModel.setVersionList` → [prepareVersions]
 *  - the single-vs-multi version branch in `HomeKYCViewModel.navigateScreen` →
 *    [startDestination] / [directDestinationFor]
 *  - the front/back side decision in `PreviewScreenViewModel.navigateScreen` →
 *    [resolveAfterConfirm]
 *
 * Which step is actionable is *not* decided here: that uses the view model's live overlays
 * (processing / verdict / mandatory lock) via `HomeKYCMapper.resolveActiveRule`, so this
 * object never reads the cache's stale `rule.status` for sequencing.
 *
 * Kept free of Compose so it stays unit-testable; the nav host just calls these and
 * turns the results into [AmaniV2Navigator] moves.
 */
internal object CaptureFlow {

    /**
     * Title of the KYC rule the prepared versions belong to ("Identification", "Selfie",
     * …) — the document-type chooser's nav title. Set by [prepareVersions].
     */
    var currentRuleTitle: String? = null
        private set

    /** Step config of the currently prepared rule — the document chooser's string source. */
    var currentStepConfig: datamanager.model.config.StepConfig? = null
        private set

    /**
     * Builds and caches the version list for [rule] from the step config, stamping each
     * version with its documentId/stepId. Mirrors `HomeKYCViewModel.setVersionList`.
     */
    fun prepareVersions(rule: Rule): List<Version> {
        currentRuleTitle = rule.title
        val config = CachingHomeKYC.appConfig ?: run {
            Timber.e("V2 capture flow: no app config, the step cannot be prepared")
            return emptyList()
        }
        val sortOrder = rule.sortOrder ?: run {
            Timber.e("V2 capture flow: rule ${rule.id} has no sort order")
            return emptyList()
        }
        // Resolved by rule id, not by sortOrder position: sortOrder is 0-based on profiles that
        // carry a before-KYC step, which shifted the lookup onto the previous step's config.
        val stepConfig = config.stepConfigs?.firstOrNull { it.id != null && it.id == rule.id }
            ?: run {
                Timber.e("V2 capture flow: no step config matches rule ${rule.id}")
                return emptyList()
            }
        currentStepConfig = stepConfig
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
     * Capture destination for a single chosen [version], keyed on its `documentId` —
     * the V2 port of v1's `HomeKYCViewModel.navigateScreen` `when (documentID)` (the
     * "single version" branch): each document kind opens its own capture screen.
     *
     *  - ID / Passport / Driving licence / Visa → the front-side capture *guide*
     *    (prototype screen 7), which then opens the camera
     *  - Selfie → selfie capture
     *  - Signature → sign-contract (digital signature) screen
     *  - Physical contract / utility bill ("IB") → verify-address screen
     *  - NFC-only → no V2 screen yet (deferred, returns null)
     *
     * This is also what the document-type chooser calls once the user picks a card.
     */
    fun directDestinationFor(version: Version): AmaniV2Destination? {
        val type = version.type ?: run {
            Timber.e("V2 capture flow: version of ${version.documentId} has no type")
            return null
        }
        return when (version.documentId) {
            AmaniDocumentTypes.IDENTIFICATION,
            AmaniDocumentTypes.PASSPORT,
            AmaniDocumentTypes.DRIVING_LICENSE,
            AmaniDocumentTypes.VISA ->
                // Land on the front-side guide first (prototype screen 7); its "Open camera"
                // action navigates on to Capture(Front).
                AmaniV2Destination.CaptureGuide(type, CaptureSide.Front)

            AmaniDocumentTypes.SELFIE ->
                // Land on the selfie guide first (legacy instruction animation), unless the
                // pose-estimation V2 flow — which v1 skips the animations for entirely.
                if (SelfieTypeResolver.resolve(version) == SelfieType.PoseEstimationV2)
                    AmaniV2Destination.SelfieCapture(type)
                else
                    AmaniV2Destination.SelfieGuide(type, SelfieGuideStep.First)

            AmaniDocumentTypes.SIGNATURE ->
                AmaniV2Destination.Signature(type)

            AmaniDocumentTypes.PHYSICAL_CONTRACT ->
                // The config decides where the document comes from: the camera keeps the
                // capture screen, a gallery document explains itself first, and a PDF goes
                // straight to the documents provider.
                documentSourceDestination(version, type)

            // Speech verification — hosts the optional AmaniSpeechVerifier module (single
            // document, like selfie: never lands on the document-type chooser).
            AmaniDocumentTypes.SPEECH ->
                AmaniV2Destination.SpeechVerify(type)

            AmaniDocumentTypes.NFC -> null

            // Every other document id is a generic document, exactly like v1: its `when` on
            // documentId ends with `else -> PhysicalContractScreen`, so a profile-specific id
            // ("IA", "PD", …) is captured or uploaded through the generic document flow instead
            // of being dropped. Returning null here left those steps doing nothing at all.
            else -> documentSourceDestination(version, type)
        }
    }

    /**
     * Where a generic (physical contract) document goes, decided by its `documentSource` config:
     * the camera keeps the capture screen, a gallery document explains itself first, and a PDF
     * goes straight to the documents provider.
     */
    private fun documentSourceDestination(version: Version, type: String): AmaniV2Destination {
        val source = DocumentSource.from(version.documentSource)
        val destination = when (source) {
            DocumentSource.Camera -> AmaniV2Destination.AddressVerify(type)
            DocumentSource.Gallery -> AmaniV2Destination.DocumentInfo(type)
            DocumentSource.PdfFile -> AmaniV2Destination.DocumentPick(type)
        }
        Timber.i(
            "V2 generic document: source=$source (config: ${version.documentSource}), " +
                "destination=$destination"
        )
        return destination
    }

    /**
     * Where a selfie guide's "Open camera" action goes, mirroring v1's per-type animation
     * sequence (SelfieCaptureViewModel): the pose-estimation flow shows a *second* instruction
     * animation after the first, so [SelfieGuideStep.First] advances to
     * [AmaniV2Destination.SelfieGuide] `Second` there; every other case (auto / manual, or the
     * already-shown second step) opens the camera. Pose-estimation V2 never reaches a guide, so
     * it isn't handled here.
     */
    fun selfieAfterGuide(version: Version, step: SelfieGuideStep): AmaniV2Destination {
        val type = version.type ?: return AmaniV2Destination.HomeKYC
        val isPoseEstimation = SelfieTypeResolver.resolve(version) is SelfieType.PoseEstimation
        return if (step == SelfieGuideStep.First && isPoseEstimation)
            AmaniV2Destination.SelfieGuide(type, SelfieGuideStep.Second)
        else
            AmaniV2Destination.SelfieCapture(type)
    }

    /**
     * Entry destination for the currently prepared step (call right after
     * [prepareVersions]). This is the V2 port of v1's `HomeKYCViewModel.navigateScreen`,
     * preserving its two-branch shape so each document kind opens the right screen:
     *
     *  - **Single selectable document** (only one version, or only one not hidden):
     *    route straight into that document's capture screen via [directDestinationFor]
     *    (v1's single-version branch — Selfie → selfie, ID family → ID capture, …). This
     *    is why a selfie step never lands on the document-type chooser.
     *  - **Several selectable documents**: only the photo-ID family (ID / Passport /
     *    Driving licence / Visa / Physical contract) opens the [AmaniV2Destination.DocumentType]
     *    chooser (v1's multi-version branch); anything else still resolves directly.
     *
     * Returns `null` when there are no versions or the resolved document kind has no V2
     * screen yet (the caller then simply doesn't navigate).
     */
    fun startDestination(): AmaniV2Destination? {
        val all = CachingHomeKYC.versionsList.orEmpty()
        if (all.isEmpty()) {
            Timber.e("V2 capture flow: the prepared step has no versions, nothing to start")
            return null
        }
        val nonHidden = visibleVersions()
        // v1: single when `versionsList.size == 1 || non-hidden count == 1`.
        val isSingle = all.size == 1 || nonHidden.size == 1
        // The document the step is actually about (v1 reads documentId off the current
        // version); prefer the visible one so a hidden lead version can't mislead routing.
        val primary = nonHidden.firstOrNull() ?: all.first()
        Timber.i(
            "V2 capture flow: starting documentId=${primary.documentId} type=${primary.type} " +
                "versions=${all.size} visible=${nonHidden.size}"
        )
        if (isSingle) return directDestinationFor(primary)

        return when (primary.documentId) {
            AmaniDocumentTypes.IDENTIFICATION,
            AmaniDocumentTypes.PASSPORT,
            AmaniDocumentTypes.DRIVING_LICENSE,
            AmaniDocumentTypes.VISA,
            AmaniDocumentTypes.PHYSICAL_CONTRACT ->
                AmaniV2Destination.DocumentType

            // Selfie / signature / nfc never offer a document-type chooser (v1 keeps them
            // single-version); resolve straight to their capture screen.
            else -> directDestinationFor(primary)
        }
    }

    /**
     * Where to go after the user confirms a captured side. The core "skip the back side"
     * rule: a two-sided document (`version.steps.size > 1`) whose front was just confirmed
     * advances to the back-side guide (prototype screen 10, which then opens the camera);
     * anything else (single-sided, or the back already done) finishes the capture leg —
     * represented by `null`.
     *
     * When the leg is finished the caller then checks [isNfcEnabled] (+ device support): an
     * NFC-enabled ID routes to [AmaniV2Destination.NfcScan] before upload (v1
     * PreviewScreenViewModel → NFCScanFragment), otherwise the host pops to Home and uploads.
     */
    fun resolveAfterConfirm(version: Version, side: CaptureSide): AmaniV2Destination? {
        val isTwoSided = (version.steps?.size ?: 0) > 1
        return if (side == CaptureSide.Front && isTwoSided) {
            AmaniV2Destination.CaptureGuide(version.type ?: return null, CaptureSide.Back)
        } else {
            null
        }
    }

    /**
     * Whether this document enables NFC in config — the V2 port of v1's
     * `version.nfcAndroid ?: version.nfc` check. Device NFC availability is checked
     * separately (needs a Context) at the call site.
     */
    fun isNfcEnabled(version: Version): Boolean = version.nfcAndroid ?: version.nfc
}
