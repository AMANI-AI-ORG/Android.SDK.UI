package ai.amani.sdk.presentation.common.document_picker

import ai.amani.amani_sdk.R
import android.content.res.Resources
import android.os.Parcelable
import datamanager.model.config.Version
import kotlinx.parcelize.Parcelize

/**
 * Texts of the source dialog of an `optional` document, shared by the V1 dialog and the V2
 * overlay so both flows say the same thing.
 *
 * The title and the description are the document's own config — the step's `captureTitle` and
 * the version's `basicInfoText`, the same fields the gallery information screen reads. The option
 * labels are the version's `uploadViaGalleryText`, `uploadViaPdfText` and `uploadViaCameraText`.
 * Each falls back to the SDK string (overridable in the integrator's resources) when the config
 * leaves it empty.
 */
@Parcelize
data class DocumentSourceChoiceTexts(
    val title: String,
    val description: String,
    val galleryText: String,
    val pdfText: String,
    val cameraText: String
) : Parcelable

/** Resolves the [DocumentSourceChoiceTexts] of [version], filling the gaps from [resources]. */
internal fun documentSourceChoiceTextsOf(
    version: Version?,
    resources: Resources
): DocumentSourceChoiceTexts = DocumentSourceChoiceTexts(
    title = version?.steps?.firstOrNull()?.captureTitle.orFallback(
        resources.getString(R.string.document_source_title)
    ),
    description = version?.basicInfoText.orFallback(
        resources.getString(R.string.document_source_description)
    ),
    galleryText = version?.uploadViaGalleryText.orFallback(
        resources.getString(R.string.document_source_gallery)
    ),
    pdfText = version?.uploadViaPdfText.orFallback(
        resources.getString(R.string.document_source_pdf)
    ),
    cameraText = version?.uploadViaCameraText.orFallback(
        resources.getString(R.string.document_source_camera)
    )
)

/** A config read from JSON can carry null or blank despite the non-null type. */
private fun String?.orFallback(fallback: String): String =
    this?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
