package ai.amani.sdk.presentation_v2.navigation

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
     * Document capture — V2 counterpart of IDCaptureFront/BackSideFrag. [side] selects
     * which face is captured; [versionType] keys the chosen [datamanager.model.config.Version].
     */
    @Parcelize
    data class Capture(
        val versionType: String,
        val side: CaptureSide
    ) : AmaniV2Destination

    /**
     * Captured-image confirmation — V2 counterpart of PreviewScreenFragment. [imagePath]
     * is the just-captured frame persisted by the camera host, shown for review. Confirming
     * here applies the v1 side logic: a two-sided document (`version.steps.size > 1`)
     * advances to the back-side [Capture]; a single-sided one finishes without ever
     * opening a back-side capture screen.
     */
    @Parcelize
    data class CaptureConfirm(
        val versionType: String,
        val side: CaptureSide,
        val imagePath: String
    ) : AmaniV2Destination
}

/** Which face of a document is being captured. */
@Parcelize
enum class CaptureSide : Parcelable { Front, Back }
