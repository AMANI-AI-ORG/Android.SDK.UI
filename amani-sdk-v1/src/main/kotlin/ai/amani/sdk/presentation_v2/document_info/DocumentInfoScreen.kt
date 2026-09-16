package ai.amani.sdk.presentation_v2.document_info

import ai.amani.sdk.presentation.common.document_picker.MIME_IMAGE
import ai.amani.sdk.presentation.common.document_picker.OpenDocumentIn
import ai.amani.sdk.presentation.common.document_picker.screenshotsFolderUriOrNull
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import timber.log.Timber

/**
 * V2 information screen shown before a `gallery` document is picked — the Compose counterpart
 * of the V1 information fragment, with the same business flow: the config explains what to
 * upload, and the single action opens the gallery.
 *
 * The picked file is handed back through [onPicked]; the host uploads it exactly like a
 * photographed document (`GenericDocumentFlow.DataFromGallery`) and pops to Home. Leaving the
 * picker without choosing anything keeps the user on this screen, so the explanation stays
 * readable and the action can be retried.
 */
@Composable
fun DocumentInfoScreen(
    state: DocumentInfoUiState,
    onPicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val currentOnPicked by rememberUpdatedState(onPicked)

    val galleryPicker = rememberLauncherForActivityResult(OpenDocumentIn()) { uri: Uri? ->
        if (uri == null) {
            Timber.d("V2 document info: no document picked from the gallery")
            return@rememberLauncherForActivityResult
        }
        Timber.d("V2 document info: document picked from the gallery")
        currentOnPicked(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)

        // The explanation is centered between the header and the action while it fits, and
        // scrolls inside its own area when the config text is long.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.headline,
                style = AmaniV2Type.heading.scaled(),
                color = palette.ink,
                textAlign = TextAlign.Center
            )

            if (state.details.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                // Card surface, like the other V2 screens group their secondary content.
                Text(
                    text = state.details,
                    style = AmaniV2Type.body.scaled(),
                    color = palette.inkMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AmaniV2Dimens.cardRadius.scaled()))
                        .background(palette.backgroundWarm)
                        .padding(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Keep the button clear of the system navigation bar.
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(top = 16.dp, bottom = 20.dp)
        ) {
            PrimaryButton(
                text = state.buttonText,
                leadingIcon = Icons.Outlined.PhotoLibrary,
                onClick = {
                    val initialUri = screenshotsFolderUriOrNull()
                    Timber.i("V2 document info: opening the gallery, initialUri=$initialUri")
                    runCatching {
                        galleryPicker.launch(
                            OpenDocumentIn.Input(
                                mimeType = MIME_IMAGE,
                                initialUri = initialUri
                            )
                        )
                    }.onFailure { Timber.e(it, "V2 document info: the gallery could not be opened") }
                }
            )
        }
    }
}

@Preview(name = "Document info — gallery", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DocumentInfoScreenPreview() {
    DocumentInfoScreen(
        state = DocumentInfoUiState(
            headerTitle = "IBAN Document",
            headline = "You can send your IBAN in two ways: a bank app screenshot or a WhatsApp chat.",
            details = "METHOD 1 — WhatsApp\n1. Share your IBAN to WhatsApp.\n2. Write your name right below it.\n3. Screenshot both messages.",
            buttonText = "Upload Document"
        ),
        onPicked = {}
    )
}

/** A document configured without details: the card disappears and the headline stands alone. */
@Preview(name = "Document info — headline only", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DocumentInfoHeadlineOnlyPreview() {
    DocumentInfoScreen(
        state = DocumentInfoUiState(
            headerTitle = "IBAN Document",
            headline = "Upload a screenshot that shows your IBAN and your name together.",
            details = "",
            buttonText = "Upload Document"
        ),
        onPicked = {}
    )
}
