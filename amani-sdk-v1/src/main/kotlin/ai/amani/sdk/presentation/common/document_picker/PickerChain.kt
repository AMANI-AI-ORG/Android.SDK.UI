package ai.amani.sdk.presentation.common.document_picker

import ai.amani.amani_sdk.R
import ai.amani.sdk.model.DocumentSource
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import timber.log.Timber

/**
 * Error code shown, and logged, when no app on the device can pick a document and the flow falls
 * back to the camera capture screen.
 */
internal const val ERROR_CODE_NO_PICKER = "DS-01"

/** Message of the camera fallback taken when no app on the device can pick a document. */
internal fun Resources.noPickerMessage(): String =
    getString(R.string.document_picker_unavailable, ERROR_CODE_NO_PICKER)

/**
 * `ACTION_PICK` on the images collection: opens the device's gallery app (or lets the user choose
 * one when several are installed). The gallery app grants read access to the picked image only,
 * so no storage permission is needed.
 */
internal class PickFromGallery : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK).setDataAndType(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MIME_IMAGE
        )

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/**
 * The pickers a host registers once — on a fragment (V1) or in Compose (V2) — all reporting to
 * the same callback, so the host never knows which one answered.
 */
internal class PickerLaunchers(
    private val gallery: ActivityResultLauncher<Unit>,
    private val documents: ActivityResultLauncher<OpenDocumentIn.Input>,
    private val content: ActivityResultLauncher<String>
) {

    /**
     * Opens the first picker of [source]'s chain the device has:
     *
     * - [DocumentSource.Gallery]: the gallery app → the documents provider, on the Screenshots
     *   folder when it exists and on the images otherwise → any `GET_CONTENT` app.
     * - [DocumentSource.PdfFile]: the documents provider → any `GET_CONTENT` app.
     *
     * Each picker is tried rather than resolved up front, so a device that hides one from the
     * package manager still gets it.
     *
     * @return false when the device has no picker of the chain, so the host falls back to the
     * camera; true otherwise, including for the sources that have no picker at all
     */
    fun launch(source: DocumentSource): Boolean {
        val chain: List<Pair<String, () -> Unit>> = when (source) {
            DocumentSource.Gallery -> listOf(
                "gallery app" to { gallery.launch(Unit) },
                "documents provider" to {
                    documents.launch(
                        OpenDocumentIn.Input(
                            mimeType = MIME_IMAGE,
                            initialUri = screenshotsFolderUriOrNull()
                        )
                    )
                },
                "content picker" to { content.launch(MIME_IMAGE) }
            )

            DocumentSource.PdfFile -> listOf(
                "documents provider" to {
                    documents.launch(OpenDocumentIn.Input(mimeType = MIME_PDF))
                },
                "content picker" to { content.launch(MIME_PDF) }
            )

            DocumentSource.Camera,
            DocumentSource.Optional -> {
                Timber.e("$source has no picker to launch")
                return true
            }
        }

        for ((name, open) in chain) {
            try {
                open()
                Timber.i("$source: the $name is opened")
                return true
            } catch (e: ActivityNotFoundException) {
                Timber.w("$source: no $name on the device")
            }
        }
        Timber.e("$source: no picker on the device, error code $ERROR_CODE_NO_PICKER")
        return false
    }
}

/**
 * True when an app on the device can pick a file of [mimeType], through the documents provider
 * or `GET_CONTENT`. Relies on the `<queries>` of the SDK manifest to see those apps.
 */
internal fun Context.hasPickerFor(mimeType: String): Boolean =
    listOf(Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_GET_CONTENT).any { action ->
        Intent(action)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mimeType)
            .resolveActivity(packageManager) != null
    }
