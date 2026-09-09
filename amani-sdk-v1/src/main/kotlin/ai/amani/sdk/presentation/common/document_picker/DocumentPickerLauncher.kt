package ai.amani.sdk.presentation.common.document_picker

import ai.amani.sdk.model.DocumentSource
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import timber.log.Timber
import java.io.File

/**
 * Opens the storage picker that a [DocumentSource] asks for and reports the picked file back.
 *
 * Framework detail of the presentation layer: it only knows how to show a picker, never what the
 * picked file means. Register it in `onCreateView` or earlier — it registers activity result
 * launchers on the fragment.
 *
 * @param fragment host of the launchers
 * @param onPicked picked file, or `null` when the user leaves the picker without choosing one
 */
internal class DocumentPickerLauncher(
    fragment: Fragment,
    private val onPicked: (uri: Uri?) -> Unit
) {

    private val pickPdf = fragment.registerForActivityResult(OpenDocumentIn()) { uri ->
        onPicked.invoke(uri)
    }

    private val pickImage = fragment.registerForActivityResult(OpenDocumentIn()) { uri ->
        onPicked.invoke(uri)
    }

    /**
     * Opens the picker of [source]. [DocumentSource.Camera] has no picker, so it is ignored.
     *
     * The gallery opens on the Screenshots folder when that folder exists and falls back to the
     * plain image picker otherwise. The folder is only a hint: a picker that cannot honour it
     * opens its own default location.
     */
    fun launch(source: DocumentSource) {
        when (source) {
            DocumentSource.Camera -> Timber.d("Camera source has no picker to launch")

            DocumentSource.PdfFile -> pickPdf.launch(
                OpenDocumentIn.Input(mimeType = MIME_PDF)
            )

            DocumentSource.Gallery -> pickImage.launch(
                OpenDocumentIn.Input(
                    mimeType = MIME_IMAGE,
                    initialUri = screenshotsFolderUriOrNull()
                )
            )
        }
    }

    private fun screenshotsFolderUriOrNull(): Uri? {
        val screenshotsFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            SCREENSHOTS_FOLDER_NAME
        )
        if (!screenshotsFolder.exists()) {
            Timber.d("Screenshots folder is not available, opening the default gallery")
            return null
        }
        return DocumentsContract.buildDocumentUri(
            EXTERNAL_STORAGE_AUTHORITY,
            "$EXTERNAL_STORAGE_PRIMARY${Environment.DIRECTORY_PICTURES}/$SCREENSHOTS_FOLDER_NAME"
        )
    }

    private companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_IMAGE = "image/*"
        const val SCREENSHOTS_FOLDER_NAME = "Screenshots"
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        const val EXTERNAL_STORAGE_PRIMARY = "primary:"
    }
}

/**
 * `ACTION_OPEN_DOCUMENT` contract that can also suggest the folder the picker opens in, which
 * `ActivityResultContracts.OpenDocument` does not expose.
 */
internal class OpenDocumentIn : ActivityResultContract<OpenDocumentIn.Input, Uri?>() {

    data class Input(val mimeType: String, val initialUri: Uri? = null)

    override fun createIntent(context: Context, input: Input): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.mimeType
            input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
