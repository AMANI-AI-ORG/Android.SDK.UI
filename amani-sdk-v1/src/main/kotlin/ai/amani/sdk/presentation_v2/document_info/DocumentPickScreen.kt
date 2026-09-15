package ai.amani.sdk.presentation_v2.document_info

import ai.amani.sdk.presentation.common.document_picker.MIME_PDF
import ai.amani.sdk.presentation.common.document_picker.OpenDocumentIn
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import timber.log.Timber

/**
 * No screen of its own: a `pdfFile` document goes straight to the documents provider, so this
 * destination only opens the picker and reports the outcome.
 *
 * A picked file goes to [onPicked] (the host uploads it like any other document and pops to
 * Home); leaving the picker empty goes to [onCancelled], which takes the user back where the
 * flow came from instead of stranding them on a blank screen.
 *
 * The background is painted so the moment between entering this destination and the picker
 * appearing does not flash a different color.
 */
@Composable
fun DocumentPickScreen(
    onPicked: (Uri) -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnCancelled by rememberUpdatedState(onCancelled)

    // The picker is opened once per visit, not again after a configuration change.
    val launched = rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(OpenDocumentIn()) { uri: Uri? ->
        if (uri == null) {
            Timber.d("V2 document pick: no document picked")
            currentOnCancelled()
            return@rememberLauncherForActivityResult
        }
        Timber.d("V2 document pick: document picked")
        currentOnPicked(uri)
    }

    LaunchedEffect(Unit) {
        if (launched.value) return@LaunchedEffect
        launched.value = true
        picker.launch(OpenDocumentIn.Input(mimeType = MIME_PDF))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    )
}
