package ai.amani.sdk.presentation_v2.address_verify

import ai.amani.sdk.Amani
import ai.amani.sdk.modules.document.DocBuilder
import ai.amani.sdk.modules.document.interfaces.IDocumentCallBack
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.SecondaryButton
import ai.amani.sdk.presentation_v2.id_capture.findFragmentActivity
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.scaled
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import timber.log.Timber

/**
 * State backing [AddressVerifyScreen]. Every string is config-driven with a fallback
 * (see [AddressVerifyMapper]); the DocBuilder texts feed the shared capture fragment's
 * own retry/confirm buttons.
 */
data class AddressVerifyUiState(
    val headerTitle: String,
    val instruction: String,
    val uploadPdfButtonText: String,
    val tryAgainText: String,
    val confirmText: String
)

/**
 * V2 verify-address (utility bill / physical contract, "IB") screen — the Compose
 * counterpart of v1's PhysicalContractFragment with the same business flow: the shared
 * AmaniAi document-capture fragment is hosted below the header (its own retry/confirm
 * buttons come from [DocBuilder]); alternatively the user picks a PDF from storage
 * (v1's toolbar select button → GetContent("application/pdf")).
 *
 *  - Camera capture confirmed → [onCaptured] (v1 IDocumentCallBack success → Home uploads
 *    with GenericDocumentFlow.DataFromCamera).
 *  - PDF picked → [onPdfPicked] (v1 → Home uploads with DataFromGallery(uri)).
 *
 * In `@Preview`/inspection there is no Activity to host a Fragment, so a framed
 * placeholder stands in for the capture area.
 */
@Composable
fun AddressVerifyScreen(
    state: AddressVerifyUiState,
    versionType: String,
    onCaptured: () -> Unit,
    onPdfPicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val currentOnPdfPicked by rememberUpdatedState(onPdfPicked)

    // v1 pickPdfFileFromStorage(): GetContent limited to PDFs.
    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Timber.d("V2 address: PDF picked from storage")
            currentOnPdfPicked(it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            Text(
                state.instruction,
                style = AmaniV2Type.bodySmall.scaled(),
                color = palette.inkMuted
            )
            Spacer(Modifier.height(12.dp))
            // Capture card: the shared AmaniAi document camera fills it (dark viewport),
            // clipped to the V2 card radius like the other capture screens.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AmaniV2Dimens.cardRadius.scaled()))
                    .clipToBounds()
            ) {
                if (LocalInspectionMode.current) {
                    AddressCapturePlaceholder(Modifier.fillMaxSize())
                } else {
                    AddressDocumentHost(
                        state = state,
                        versionType = versionType,
                        onCaptured = onCaptured,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
            // v1's toolbar "select" button, brought into the screen: upload a PDF instead
            // of photographing the document.
            SecondaryButton(
                text = state.uploadPdfButtonText,
                leadingIcon = Icons.Outlined.UploadFile,
                onClick = { pdfPicker.launch("application/pdf") }
            )
        }
    }
}

/**
 * Hosts the shared AmaniAi document-capture Fragment inside Compose (same AndroidView +
 * FragmentContainerView pattern as the other V2 hosts). Mirrors v1
 * PhysicalContractFragment.initPhysicalContract: `Document().start(type, DocBuilder,
 * container, IDocumentCallBack)` with a single document; on the success callback control is
 * handed back via [onCaptured]. The Fragment is committed/removed with the composable's
 * lifecycle.
 */
@Composable
private fun AddressDocumentHost(
    state: AddressVerifyUiState,
    versionType: String,
    onCaptured: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current.findFragmentActivity()
    val containerId = rememberSaveable { View.generateViewId() }
    val currentOnCaptured by rememberUpdatedState(onCaptured)

    AndroidView(
        modifier = modifier,
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } }
    )

    DisposableEffect(activity, versionType) {
        val fm = activity?.supportFragmentManager
        var docFragment: Fragment? = null

        if (fm != null && activity != null) {
            // v1 DocBuilder: retry/confirm labels (config texts instead of v1's hardcoded
            // Turkish), white text/button colors, single document.
            val docBuilder = DocBuilder(
                state.tryAgainText,
                state.confirmText,
                ai.amani.R.color.color_white,
                ai.amani.R.color.color_white,
                ai.amani.R.color.color_white,
                DOCUMENT_COUNT
            )
            val container = activity.findViewById<FrameLayout>(containerId)

            docFragment = Amani.sharedInstance().Document().start(
                versionType,
                docBuilder,
                container,
                object : IDocumentCallBack {
                    override fun cb(listOfDocumentAbsolutePath: ArrayList<String>?, isSuccess: Boolean) {
                        Timber.d("V2 address: document callback isSuccess=$isSuccess")
                        if (isSuccess) {
                            // Capture confirmed (v1 navigates Home to upload DataFromCamera).
                            activity.runOnUiThread { currentOnCaptured() }
                        }
                    }
                }
            )

            docFragment?.let { frag ->
                fm.beginTransaction()
                    .replace(containerId, frag)
                    .commitAllowingStateLoss()
            } ?: Timber.e("V2 address: SDK returned no document fragment")
        }

        onDispose {
            val manager = activity?.supportFragmentManager
            val frag = docFragment
            if (manager != null && frag != null && !manager.isStateSaved) {
                manager.beginTransaction().remove(frag).commitAllowingStateLoss()
            }
        }
    }
}

/** Number of documents the capture fragment collects (v1 passes 1). */
private const val DOCUMENT_COUNT = 1

/** Static stand-in for the capture area, shown only in previews/inspection. */
@Composable
private fun AddressCapturePlaceholder(modifier: Modifier = Modifier) {
    val palette = AmaniV2Theme.palette
    Box(
        modifier = modifier.background(palette.backgroundWarm),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = palette.inkLight,
                modifier = Modifier.height(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Document camera",
                style = AmaniV2Type.caption,
                color = palette.inkLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(name = "AddressVerify", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun AddressVerifyScreenPreview() {
    AmaniV2Theme {
        AddressVerifyScreen(
            state = AddressVerifyUiState(
                headerTitle = "Verify address",
                instruction = "Photograph your utility bill, or upload it as a PDF.",
                uploadPdfButtonText = "Upload PDF instead",
                tryAgainText = "Try again",
                confirmText = "Continue"
            ),
            versionType = "TUR_IB_0",
            onCaptured = {},
            onPdfPicked = {}
        )
    }
}
