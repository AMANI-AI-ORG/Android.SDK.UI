package ai.amani.sdk.presentation.document_info

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentDocumentInfoBinding
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.presentation.binding.setPropertyConfirmButton
import ai.amani.sdk.presentation.common.document_picker.DocumentPickerLauncher
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.ColorConstant
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber

/**
 * Screen shown before a gallery document is picked: the informative texts in the middle of the
 * screen and one button that opens the gallery.
 *
 * Every text comes from the remote config of the document version — the headline from
 * `basicInfoText`, the details from the step's `captureDescription`, the button from
 * `captureDocumentText` and the toolbar from the step's `captureTitle`.
 */
class DocumentInfoFragment : Fragment() {

    private lateinit var binding: FragmentDocumentInfoBinding
    private val args: DocumentInfoFragmentArgs by navArgs()

    /** Gallery picker of this screen; registered eagerly, as activity results require. */
    private val documentPickerLauncher = DocumentPickerLauncher(this) { uri ->
        onDocumentPicked(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_document_info, container, false)
        binding = FragmentDocumentInfoBinding.bind(view)
        binding.dataModel = args.dataModel
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTexts()

        binding.continueBtn.setOnClickListener {
            documentPickerLauncher.launch(DocumentSource.Gallery)
        }
    }

    private fun setTexts() {
        val version = args.dataModel.version
        val generalConfigs = args.dataModel.generalConfigs

        setToolBarTitle(
            version?.steps?.firstOrNull()?.captureTitle,
            generalConfigs?.appFontColor
        )

        val fontColor = generalConfigs?.appFontColor ?: ColorConstant.COLOR_BLACK

        binding.informationText.setTextProperty(
            version?.basicInfoText,
            fontColor
        )

        binding.informationDescription.setTextProperty(
            version?.steps?.firstOrNull()?.captureDescription?.trim(),
            fontColor
        )

        // The details sit on a faint tint of the font color, so the card reads on a light and on
        // a dark app background alike without a second color in the config.
        binding.informationDescription.backgroundTintList = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(Color.parseColor(fontColor), CARD_BACKGROUND_ALPHA)
        )

        // Nothing to show for a document configured without details.
        binding.informationDescription.isVisible =
            !version?.steps?.firstOrNull()?.captureDescription.isNullOrBlank()

        // Colors, corner radius and background come from the shared confirm-button styling, the
        // same one the `buttonConfirm` binding adapter applies elsewhere. It is called from here
        // instead of the layout because it also writes the general `confirmText`, which would
        // overwrite this screen's own label.
        generalConfigs?.let { binding.continueBtn.setPropertyConfirmButton(it) }

        // GeneralConfigs has no generic upload label (`cropAndUploadText` is the crop screen's
        // and `v2UploadPdfButtonText` is the V2 PDF one), so a document without
        // `captureDocumentText` falls back to the general continue label.
        val buttonText = version?.captureDocumentText
            ?.takeIf { it.isNotBlank() }
            ?: generalConfigs?.continueText

        binding.continueBtn.setTextProperty(
            buttonText,
            generalConfigs?.primaryButtonTextColor ?: ColorConstant.COLOR_BLACK
        )
    }

    /** Hands the picked file to the KYC home for the upload; an empty pick keeps this screen. */
    private fun onDocumentPicked(uri: Uri?) {
        if (uri == null) {
            Timber.d("No document is picked from the gallery")
            return
        }

        val version = args.dataModel.version ?: return

        findNavController().getBackStackEntry(R.id.homeKYCFragment)
            .savedStateHandle[AmaniDocumentTypes.type] = HomeKYCResultModel(
            docID = version.documentId,
            docType = version.type,
            genericDocumentFlow = GenericDocumentFlow.DataFromGallery(arrayListOf(uri))
        )

        findNavController().clearBackStack(R.id.homeKYCFragment)
        findNavController().popBackStack(R.id.homeKYCFragment, false)
    }

    private companion object {
        /** ~8% of the font color: visible as a surface, never as a second text color. */
        const val CARD_BACKGROUND_ALPHA = 20
    }
}
