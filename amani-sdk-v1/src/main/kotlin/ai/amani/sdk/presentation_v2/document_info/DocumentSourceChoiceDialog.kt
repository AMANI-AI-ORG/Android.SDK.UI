package ai.amani.sdk.presentation_v2.document_info

import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.presentation.common.document_picker.DocumentSourceChoiceTexts
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.CappedCornerShape
import ai.amani.sdk.presentation_v2.theme.configCornerRadius
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Source dialog of a document whose `documentSource` is `optional` — the V2 counterpart of the
 * V1 `DocumentSourceDialog`, shown by the nav host in a dialog window over the current screen,
 * so the screen behind it is out of reach of touch and accessibility focus.
 *
 * The options are the stacked SDK buttons: the PDF sits in the middle as the primary button and
 * its label carries the recommendation, while the gallery and the camera are secondary buttons —
 * a PDF is the original document, so it is the upload the flow steers the user to. Colors and
 * the corner radius come from the config-driven palette.
 *
 * Tapping outside the card or the back button leaves without a choice ([onDismiss]).
 *
 * @param onChosen invoked with the concrete source the user picked
 */
@Composable
fun DocumentSourceChoiceDialog(
    texts: DocumentSourceChoiceTexts,
    onChosen: (DocumentSource) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette

    // Full width, so the card keeps the screen padding like the other V2 surfaces.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .padding(AmaniV2Dimens.screenPadding)
                .fillMaxWidth()
                .background(palette.surface, CappedCornerShape(configCornerRadius()))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                texts.title,
                style = AmaniV2Type.rowTitle.scaled(),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                texts.description,
                style = AmaniV2Type.bodySmall.scaled(),
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            // The SDK buttons, stacked like the alert dialog's: the PDF is the primary button in
            // the middle, the gallery and the camera the secondary ones around it.
            SecondaryButton(
                text = texts.galleryText,
                onClick = { onChosen(DocumentSource.Gallery) }
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = texts.pdfText,
                onClick = { onChosen(DocumentSource.PdfFile) }
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = texts.cameraText,
                onClick = { onChosen(DocumentSource.Camera) }
            )
        }
    }
}

@Preview(name = "Document source choice", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun DocumentSourceChoiceDialogPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        DocumentSourceChoiceDialog(
            texts = DocumentSourceChoiceTexts(
                title = "Proof of address",
                description = "Upload a utility bill issued in the last three months. The " +
                    "original PDF is the fastest way to get verified.",
                galleryText = "Upload via gallery",
                pdfText = "Upload via PDF (Recommended)",
                cameraText = "Upload via camera"
            ),
            onChosen = {},
            onDismiss = {}
        )
    }
}
