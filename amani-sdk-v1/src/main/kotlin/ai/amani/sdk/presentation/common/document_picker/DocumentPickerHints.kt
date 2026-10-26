package ai.amani.sdk.presentation.common.document_picker

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import timber.log.Timber
import java.io.File

/**
 * Folder the documents provider's image picker should open in, or `null` when there is nothing
 * to suggest.
 *
 * A gallery document is nearly always a screenshot (a bank app's IBAN screen, a chat), so the
 * picker starts in the Screenshots folder when it exists — `Pictures/Screenshots` on most
 * devices, `DCIM/Screenshots` on Samsung and some Xiaomi ones — and falls back to the plain
 * image picker otherwise. The folder is only a hint: a picker that cannot honour it opens its
 * own default.
 *
 * Shared by the fragment (V1) and the Compose (V2) pickers so both flows behave identically.
 */
internal fun screenshotsFolderUriOrNull(): Uri? {
    val parent = SCREENSHOTS_PARENTS.firstOrNull { parent ->
        File(Environment.getExternalStoragePublicDirectory(parent), SCREENSHOTS_FOLDER_NAME)
            .exists()
    } ?: run {
        Timber.d("Screenshots folder is not available, opening the default image picker")
        return null
    }
    return DocumentsContract.buildDocumentUri(
        EXTERNAL_STORAGE_AUTHORITY,
        "$EXTERNAL_STORAGE_PRIMARY$parent/$SCREENSHOTS_FOLDER_NAME"
    )
}

/** MIME type of a picked gallery document. */
internal const val MIME_IMAGE = "image/*"

/** MIME type of a picked PDF document. */
internal const val MIME_PDF = "application/pdf"

private const val SCREENSHOTS_FOLDER_NAME = "Screenshots"

/** Where devices keep the Screenshots folder, in the order it is looked for. */
private val SCREENSHOTS_PARENTS = listOf(Environment.DIRECTORY_PICTURES, Environment.DIRECTORY_DCIM)
private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val EXTERNAL_STORAGE_PRIMARY = "primary:"
