package ai.amani.sdk.presentation_v2.navigation

import ai.amani.sdk.model.MRZModel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Type-safe destinations for the V2 (Compose) KYC flow.
 *
 * This is a deliberately library-free navigation model: no navigation-compose
 * dependency (which would drag the navigation group to 2.9.x and force
 * compileSdk 35 + AGP 8.6). Destinations are [Parcelable] so [AmaniV2Navigator]
 * can persist the whole back stack across configuration changes and process death.
 *
 * As V2 screens land, add a destination here and a branch in [AmaniV2NavHost].
 * Destinations that carry arguments are `data class`es with Parcelable fields — we
 * keep those args minimal (a `versionType` key + side), and the richer SDK data
 * ([datamanager.model.config.Version]) is resolved from [CaptureFlow] / the shared
 * cache, since `Version` is not a Parcelable we control.
 */
sealed interface AmaniV2Destination : Parcelable {

    /** KYC overview / stepper — V2 counterpart of the v1 HomeKYCFragment. */
    @Parcelize
    data object HomeKYC : AmaniV2Destination

    /** Document type selection — V2 counterpart of the v1 SelectDocumentTypeFragment. */
    @Parcelize
    data object DocumentType : AmaniV2Destination

    /**
     * Pre-capture guide — V2 counterpart of the legacy "Upload Front/Back Side" Lottie
     * screens (IDCaptureFront/BackSideFrag played `xx_id_front` / `xx_id_back` before the
     * camera). Redesigned per prototype screens 7 & 10: an animated illustration plus a
     * quality checklist, shown *before* [Capture] for each side so the user knows how to
     * frame the document. "Open camera" advances to [Capture] for the same [side];
     * [versionType] keys the chosen [datamanager.model.config.Version].
     */
    @Parcelize
    data class CaptureGuide(
        val versionType: String,
        val side: CaptureSide
    ) : AmaniV2Destination

    /**
     * Document capture — V2 counterpart of IDCaptureFront/BackSideFrag. [side] selects
     * which face is captured; [versionType] keys the chosen [datamanager.model.config.Version].
     */
    @Parcelize
    data class Capture(
        val versionType: String,
        val side: CaptureSide
    ) : AmaniV2Destination

    /**
     * Captured-image confirmation — V2 counterpart of PreviewScreenFragment. The
     * just-captured frame is handed over in memory (see
     * [ai.amani.sdk.presentation_v2.preview_screen.CapturedFrame]), keeping the camera's
     * orientation and this destination Parcelable-small. Confirming here applies the v1
     * side logic: a two-sided document (`version.steps.size > 1`) advances to the back-side
     * [Capture]; a single-sided one finishes without ever opening a back-side capture screen.
     */
    @Parcelize
    data class CaptureConfirm(
        val versionType: String,
        val side: CaptureSide
    ) : AmaniV2Destination

    /**
     * Pre-selfie guide — V2 counterpart of the legacy selfie instruction animations
     * (SelfieCaptureFragment played `animation_first_selfie_instruction` /
     * `animation_second_selfie_instruction` before mounting the camera). Which animation(s)
     * appear follows v1's per-type logic (see [CaptureFlow]):
     *  - Auto / Manual → one guide ([SelfieGuideStep.First]) → camera
     *  - Pose estimation → two guides ([SelfieGuideStep.First] then [SelfieGuideStep.Second]) → camera
     *  - Pose estimation V2 → no guide (straight to [SelfieCapture])
     *
     * "Open camera" advances to the next [SelfieGuideStep] or to [SelfieCapture].
     * [versionType] keys the chosen [datamanager.model.config.Version].
     */
    @Parcelize
    data class SelfieGuide(
        val versionType: String,
        val step: SelfieGuideStep
    ) : AmaniV2Destination

    /**
     * Selfie capture — V2 counterpart of SelfieCaptureFragment. The live AmaniAi selfie
     * camera mounts beneath the header; the concrete variant (auto / manual / pose
     * estimation) is resolved from the [datamanager.model.config.Version] via
     * [ai.amani.sdk.presentation_v2.selfie_capture.SelfieTypeResolver]. [versionType] keys
     * the chosen version.
     */
    @Parcelize
    data class SelfieCapture(
        val versionType: String
    ) : AmaniV2Destination

    /**
     * Captured-selfie confirmation — the selfie counterpart of [CaptureConfirm] (selfies
     * are single-sided, so there is no `side`; the frame is handed over in memory the same
     * way). Confirming finishes the leg: the host uploads through the selfie SDK path and
     * pops to Home, where the step shows its processing spinner.
     */
    @Parcelize
    data class SelfieConfirm(
        val versionType: String
    ) : AmaniV2Destination

    /**
     * NFC chip scan — V2 counterpart of NFCScanFragment. Reached after the final side of an
     * NFC-enabled ID is confirmed *and* its MRZ has been read (on the confirm screen, mirroring
     * v1 PreviewScreenViewModel → NFCScanFragment). [versionType] keys the chosen version;
     * [mrz] is the MRZ already read from the captured ID, so the NFC screen opens ready to scan.
     * [nfcOnly] mirrors v1's NFCScanScreenModel flag: false = NFC + ID uploaded together,
     * true = NFC without an ID leg.
     */
    @Parcelize
    data class NfcScan(
        val versionType: String,
        val mrz: MRZModel,
        val nfcOnly: Boolean = false
    ) : AmaniV2Destination

    /**
     * Sign contract (digital signature) — V2 counterpart of SignatureFragment. The shared
     * AmaniAi signature pad is hosted in the screen; once the required signature count is
     * taken the host uploads through the shared signature repository and pops to Home
     * (v1's navigate-home-then-upload hand-off). [versionType] keys the chosen version.
     */
    @Parcelize
    data class Signature(
        val versionType: String
    ) : AmaniV2Destination

    /**
     * Verify address (utility bill / physical contract, "IB") — V2 counterpart of
     * PhysicalContractFragment. The shared AmaniAi document-capture fragment is hosted in
     * the screen (or the user picks a PDF from storage); either way the host uploads through
     * the shared document repository and pops to Home (v1's navigate-home-then-upload
     * hand-off). [versionType] keys the chosen version.
     */
    @Parcelize
    data class AddressVerify(
        val versionType: String
    ) : AmaniV2Destination

    /**
     * Speech verification (document id "ST", type like `XXX_ST_0`) — hosts the OPTIONAL,
     * standalone AmaniSpeechVerifier module (a `compileOnly` dependency, not bundled). The
     * user reads a passphrase aloud; on success the host uploads the recorded session through
     * `SpeechVerifier.upload` and pops to Home (the same navigate-home-then-upload hand-off as
     * the other legs). [versionType] keys the chosen version and is passed as the module's
     * document type. Reachable only when the module is present on the classpath (the nav host
     * checks and otherwise throws a descriptive error).
     */
    @Parcelize
    data class SpeechVerify(
        val versionType: String
    ) : AmaniV2Destination
}

/** Which face of a document is being captured. */
@Parcelize
enum class CaptureSide : Parcelable { Front, Back }

/**
 * Which pre-selfie instruction animation a [AmaniV2Destination.SelfieGuide] shows —
 * [First] = `animation_first_selfie_instruction`, [Second] = the follow-up
 * `animation_second_selfie_instruction` used only for the pose-estimation flow (v1).
 */
@Parcelize
enum class SelfieGuideStep : Parcelable { First, Second }
