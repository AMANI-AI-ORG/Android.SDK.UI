package ai.amani.sdk.model

/**
 * Where a generic (physical contract) document is acquired from.
 *
 * Mapped from the remote config field `documentSource` of a document version. Pure Kotlin: it
 * carries the decision, not the way the picker or the camera is opened.
 */
enum class DocumentSource {

    /** Classic capture screen: the document is photographed with the camera. */
    Camera,

    /** The document is picked as a PDF from the documents provider. */
    PdfFile,

    /** The document is picked as an image from the gallery, preferring the Screenshots folder. */
    Gallery;

    /** True when the document is picked from storage instead of being captured. */
    val isPicker: Boolean
        get() = this != Camera

    companion object {

        private const val VALUE_PDF_FILE = "pdffile"
        private const val VALUE_GALLERY = "gallery"

        /** Maps the remote config value to a source, defaulting to [Camera]. */
        fun from(value: String?): DocumentSource = when (value?.trim()?.lowercase()) {
            VALUE_PDF_FILE -> PdfFile
            VALUE_GALLERY -> Gallery
            else -> Camera
        }
    }
}
