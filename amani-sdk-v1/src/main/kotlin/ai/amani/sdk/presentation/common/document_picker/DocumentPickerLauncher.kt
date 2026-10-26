package ai.amani.sdk.presentation.common.document_picker

import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.model.DocumentSource
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Opens the storage picker that a [DocumentSource] asks for and reports the picked file back.
 *
 * Framework detail of the presentation layer: it only knows how to show a picker, never what the
 * picked file means. Register it in `onCreateView` or earlier — it registers activity result
 * launchers on the fragment.
 *
 * @param fragment host of the launchers
 * @param onUnavailable the device has no picker for the source at all; the host falls back to
 * the camera capture screen (see [noPickerMessage])
 * @param onPicked picked file, or `null` when the user leaves the picker without choosing one
 */
internal class DocumentPickerLauncher(
    fragment: Fragment,
    private val onUnavailable: (source: DocumentSource) -> Unit,
    private val onPicked: (uri: Uri?) -> Unit
) {

    private val launchers = PickerLaunchers(
        gallery = fragment.registerForActivityResult(PickFromGallery()) { onPicked.invoke(it) },
        documents = fragment.registerForActivityResult(OpenDocumentIn()) { onPicked.invoke(it) },
        content = fragment.registerForActivityResult(ActivityResultContracts.GetContent()) {
            onPicked.invoke(it)
        }
    )

    /**
     * Opens the first picker of [source]'s chain the device has (see [PickerLaunchers.launch]),
     * or reports [onUnavailable] when it has none. [DocumentSource.Camera] has no picker and
     * [DocumentSource.Optional] is resolved by the user first, so both are ignored.
     */
    fun launch(source: DocumentSource) {
        if (!launchers.launch(source)) onUnavailable.invoke(source)
    }
}

/**
 * Tells the user that no app can pick their document, with the error code support looks up.
 * Shown on the activity, so it stays up over the capture screen the host falls back to.
 */
internal fun Fragment.showNoPickerMessage() =
    showSnackbar(resources.noPickerMessage(), Snackbar.LENGTH_LONG)

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
