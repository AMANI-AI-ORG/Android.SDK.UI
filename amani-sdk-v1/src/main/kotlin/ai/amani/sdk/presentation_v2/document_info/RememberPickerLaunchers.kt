package ai.amani.sdk.presentation_v2.document_info

import ai.amani.sdk.presentation.common.document_picker.OpenDocumentIn
import ai.amani.sdk.presentation.common.document_picker.PickFromGallery
import ai.amani.sdk.presentation.common.document_picker.PickerLaunchers
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The Compose counterpart of the V1 `DocumentPickerLauncher` registration: every picker of the
 * chain reports to [onResult], so the screen never knows which one answered.
 */
@Composable
internal fun rememberPickerLaunchers(onResult: (Uri?) -> Unit): PickerLaunchers {
    val gallery = rememberLauncherForActivityResult(PickFromGallery(), onResult)
    val documents = rememberLauncherForActivityResult(OpenDocumentIn(), onResult)
    val content = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), onResult)
    return remember(gallery, documents, content) { PickerLaunchers(gallery, documents, content) }
}
