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
 *    │       (shown only  │              continue           │ [Upload via PDF] primary  │
 *    │        when an app │                  │              │ [Upload via camera]       │
 *    │        can pick a  │                  │              └─┬──────┬──────┬───────┬───┘
 *    │        PDF)        │                  │             Gallery  PDF   Camera  back /
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
 *    │      │ PDF chain            │  │ Image chain (a gallery choice skips  │
 *    │      │ (PickerLaunchers)    │  │ the information screen: the dialog   │
 *    │      │                      │  │ already explains the document)       │
 *    │      │ 1. Documents         │  │                                      │
 *    │      │    provider,         │  │ 1. Gallery app: ACTION_PICK on the   │
 *    │      │    ACTION_OPEN_      │  │    MediaStore images (a chooser when │
 *    │      │    DOCUMENT          │  │    several gallery apps exist)       │
 *    │      │ 2. Any GET_CONTENT   │  │ 2. Documents provider, ACTION_OPEN_  │
 *    │      │    app               │  │    DOCUMENT, any image, opened in    │
 *    │      │                      │  │    Pictures/Screenshots or else      │
 *    │      │ application/pdf      │  │    DCIM/Screenshots when one exists  │
 *    │      │                      │  │ 3. Any GET_CONTENT app, any image    │
 *    │      └──┬───────┬────────┬──┘  └──┬─────────┬─────────────────────┬──┘
 *    │       file  cancelled   none     file     none                cancelled
 *    │         │       │      opens       │      opens                    │
 *    │         │       │        │         │        │                      │
 *    │         │       │        └─────────┼────────┴──────────┐           │
 *    │         │       │                  │                   ▼           │
 *    │         │       │                  │   ┌─────────────────────────┐ │
 *    │         │       │                  │   │ Camera fallback         │ │
 *    │         │       │                  │   │ capture screen replaces │ │
 *    │         │       │                  │   │ the picker's screen,    │ │
 *    │         │       │                  │   │ snackbar with error     │ │
 *    │         │       │                  │   │ code DS-01; from the    │ │
 *    │         │       │                  │   │ capture screen's own    │ │
 *    │         │       │                  │   │ button: it hides, the   │ │
 *    │         │       │                  │   │ capture stays           │ │
 *    │         │       │                  │   └────────────┬────────────┘ │
 *    │         │       ▼                  │                │              ▼
 *    │         │    V1: stays on the screen that opened the picker
 *    │         │    V2: info / capture screen stays; a direct pick
 *    │         │        (DocumentPick) pops back to where it started
 *    │         │                          │                │
 *    ▼         ▼                          ▼                ▼
 *  DataFromCamera              DataFromGallery(uri)   (as Camera)
 *    │                                    │
 *    └────────────────┬───────────────────┘
 *                     ▼
 *     KYC home uploads the document, the step shows its processing state
 * ```
 *
 * No picker needs a runtime permission: the app that answers grants read access to the one file
 * the user picked. Each picker of a chain is tried rather than resolved up front; the SDK
 * manifest's `<queries>` only serves the check that decides whether the capture screen offers
 * its PDF button. The integrating app adds nothing.
 */
enum class DocumentSource {

    /** Classic capture screen: the document is photographed with the camera. */
    Camera,

    /**
     * The document is picked as a PDF from the documents provider, else from any `GET_CONTENT`
     * app; with neither, it is photographed instead.
     */
    PdfFile,

    /**
     * The document is picked as an image from the device's gallery app, else from the documents
     * provider (preferring the Screenshots folder), else from any `GET_CONTENT` app; with none of
     * them, it is photographed instead.
     */
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
