package ai.amani.sdk.model

/**
 * Where a generic (physical contract) document is acquired from.
 *
 * Mapped from the remote config field `documentSource` of a document version. Pure Kotlin: it
 * carries the decision, not the way the picker or the camera is opened.
 *
 * ## Flow
 *
 * Only generic documents get here: every document id except ID, passport, driving licence, visa,
 * selfie, signature, speech and NFC (V1 `physicalContractRouteOf`, V2
 * `CaptureFlow.documentSourceDestination`). The flow is the same whether the step starts from the
 * KYC home or from the document type chooser; V1 and V2 differ only in the screens' names.
 *
 * ```
 *               version.documentSource (remote config, trimmed, case-insensitive)
 *                                     │
 *                          DocumentSource.from(value)
 *                                     │
 *      ┌──────────────────┬───────────┴───────┬───────────────────────────┐
 *  "camera", null,    "pdfFile"           "gallery"                  "optional"
 *   blank, unknown        │                   │                           │
 *      │                  ▼                   ▼                           ▼
 *      ▼               PdfFile             Gallery                    Optional
 *    Camera               │                   │                           │
 *      │                  │                   ▼                           ▼
 *      ▼                  │        ┌────────────────────┐   ┌───────────────────────────┐
 *  ┌───────────────────┐  │        │ Information screen │   │ Source dialog             │
 *  │ Capture screen    │  │        │ V1 DocumentInfo-   │   │ V1 DocumentSourceDialog   │
 *  │ V1 PhysicalCon-   │  │        │    Fragment        │   │ V2 DocumentSource-        │
 *  │    tractFragment  │  │        │ V2 DocumentInfo-   │   │    ChoiceDialog           │
 *  │ V2 AddressVerify  │  │        │    Screen          │   │ title: captureTitle       │
 *  └─┬─────────────┬───┘  │        │ texts: basicInfo-  │   │ text:  basicInfoText      │
 *    │             │      │        │  Text, capture-    │   │ (SDK strings as fallback) │
 *  photo      "select"    │        │  Description       │   │                           │
 *  taken      PDF button  │        └─────────┬──────────┘   │ [Upload via gallery]      │
 *    │             │      │              continue           │ [Upload via PDF] primary  │
 *    │             │      │                  │              │ [Upload via camera]       │
 *    │             │      │                  │              └─┬──────┬──────┬───────┬───┘
 *    │             │      │                  │             Gallery  PDF   Camera  back /
 *    │             │      │                  │                │      │      │    outside
 *    │             │      │                  │                │      │      │       │
 *    │             │      │                  │                │      │      │       ▼
 *    │             │      │                  │                │      │      │  stays on the
 *    │             │      │                  │                │      │      │  current screen
 *    │             │      │                  │                │      │      │
 *    │             │      │                  │                │      │      └──► Capture screen
 *    │             │      │                  │                │      │          (as Camera)
 *    │             ▼      ▼                  ▼                ▼      ▼
 *    │      ┌──────────────────────┐  ┌──────────────────────────────────────┐
 *    │      │ PDF picker           │  │ Image picker (gallery choice skips   │
 *    │      │ ACTION_OPEN_DOCUMENT │  │ the information screen: the dialog   │
 *    │      │ application/pdf      │  │ already explains the document)       │
 *    │      │                      │  │ ACTION_OPEN_DOCUMENT, any image,     │
 *    │      │                      │  │ opened in Pictures/Screenshots when  │
 *    │      │                      │  │ that folder exists                   │
 *    │      └───┬──────────────┬───┘  └───┬─────────────────────────────┬────┘
 *    │        file        cancelled      file                       cancelled
 *    │          │              │          │                             │
 *    │          │              ▼          │                             ▼
 *    │          │       V1: stays on the screen that opened the picker
 *    │          │       V2: info / capture screen stays; a direct pick
 *    │          │           (DocumentPick) pops back to where it started
 *    │          │                         │
 *    ▼          ▼                         ▼
 *  DataFromCamera              DataFromGallery(uri)
 *    │                                    │
 *    └────────────────┬───────────────────┘
 *                     ▼
 *     KYC home uploads the document, the step shows its processing state
 * ```
 *
 * Neither picker needs a runtime permission or a manifest entry: the documents provider grants
 * read access to the one file the user picked.
 */
enum class DocumentSource {

    /** Classic capture screen: the document is photographed with the camera. */
    Camera,

    /** The document is picked as a PDF from the documents provider. */
    PdfFile,

    /** The document is picked as an image from the gallery, preferring the Screenshots folder. */
    Gallery,

    /**
     * The user decides: a dialog offers [Gallery], [PdfFile] and [Camera], with the PDF upload
     * promoted. Only ever a config value — the user's choice is one of the concrete sources.
     */
    Optional;

    /** True when the document is picked from storage instead of being captured. */
    val isPicker: Boolean
        get() = this == PdfFile || this == Gallery

    companion object {

        private const val VALUE_PDF_FILE = "pdffile"
        private const val VALUE_GALLERY = "gallery"
        private const val VALUE_OPTIONAL = "optional"

        /** Maps the remote config value to a source, defaulting to [Camera]. */
        fun from(value: String?): DocumentSource = when (value?.trim()?.lowercase()) {
            VALUE_PDF_FILE -> PdfFile
            VALUE_GALLERY -> Gallery
            VALUE_OPTIONAL -> Optional
            else -> Camera
        }
    }
}
