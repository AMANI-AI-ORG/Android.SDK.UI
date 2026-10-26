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
 * the version's `basicInfoText`, the same fields the gallery information screen reads — and fall
 * back to the SDK strings. The option labels have no config field, so they are SDK strings that
 * an integrator can override in its own resources; the PDF label carries the recommendation.
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
    // TODO: static until the remote config carries the option labels, then read them from the
    //  config like the title and the description.
    galleryText = resources.getString(R.string.document_source_gallery),
    pdfText = resources.getString(R.string.document_source_pdf),
    cameraText = resources.getString(R.string.document_source_camera)
)

/** A config read from JSON can carry null or blank despite the non-null type. */
private fun String?.orFallback(fallback: String): String =
    this?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
