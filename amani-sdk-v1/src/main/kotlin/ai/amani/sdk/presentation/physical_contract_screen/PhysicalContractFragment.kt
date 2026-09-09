package ai.amani.sdk.presentation.physical_contract_screen

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentPhysicalContractBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.popBackStackSafely
import ai.amani.sdk.extentions.replaceChildFragmentWithoutBackStack
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.modules.document.DocBuilder
import ai.amani.sdk.modules.document.interfaces.IDocumentCallBack
import ai.amani.sdk.presentation.AmaniMainActivity
import ai.amani.sdk.presentation.common.document_picker.DocumentPickerLauncher
import ai.amani.sdk.utils.AmaniDocumentTypes
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * Capture screen of a generic document.
 *
 * Only reached by the documents whose `documentSource` is the camera — the picker sources are
 * handled where the flow is routed, without opening this screen. The select button still offers
 * a PDF from storage as an alternative to the capture.
 *
 * @Author: zekiamani
 * @Date: 1.03.2023
 */
class PhysicalContractFragment: Fragment() {

    private lateinit var binding: FragmentPhysicalContractBinding
    private var args = navArgs<PhysicalContractFragmentArgs>()

    /**
     * PDF alternative behind the select button. Registered eagerly because activity result
     * launchers cannot be registered once the fragment is started.
     */
    private val documentPickerLauncher = DocumentPickerLauncher(this) { uri ->
        onPdfPicked(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_physical_contract, container, false)
        binding = FragmentPhysicalContractBinding.bind(view)
        return view

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolBar()

        initPhysicalContract()

        clickEvents()

    }

    private fun initPhysicalContract() {

        // Preparing DocumentBuilder for User Interface && Some Logic Operations
        val docBuilder = DocBuilder(
            "Tekrar Dene",  // Text label
            "Devam Et",  // Text label
            ai.amani.R.color.color_white,  // Text color
            ai.amani.R.color.color_white,
            ai.amani.R.color.color_white,  // Button color
            1
        ) // Indicates the number of documents.

        val docFragment =
            Amani.sharedInstance().Document().start(
                args.value.dataModel.version!!.type,
                docBuilder,
                binding.childOfPhysicalContract,
                object : IDocumentCallBack {
                    override fun cb(
                        listOfDocumentAbsolutePath: ArrayList<String>?,
                        isSuccess: Boolean
                    ) {
                        Timber.d("Document fragment callback triggered isSuccess: $isSuccess")
                        if (isSuccess) {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                                findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
                                    HomeKYCResultModel(
                                        docID = args.value.dataModel.version!!.documentId!!,
                                        docType = args.value.dataModel.version!!.type,
                                    )

                                findNavController().clearBackStack(R.id.homeKYCFragment)
                                findNavController().popBackStack(R.id.homeKYCFragment, false)

                                AmaniMainActivity.hideSelectButton()
                            }
                        }
                    }
                })

        docFragment?.let {
            replaceChildFragmentWithoutBackStack(
                R.id.child_of_physical_contract ,
                it
            )
        }?:run {
            showSnackbar("Configuration error, Physical Contract Screen could not launch")
            popBackStackSafely()
        }
    }

    private fun toolBar() {
        setToolBarTitle(
            args.value.dataModel.version!!.steps[0].captureTitle,
            args.value.dataModel.generalConfigs!!.appFontColor
        )
    }

    private fun clickEvents() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            AmaniMainActivity.addSelectButtonListener {
                documentPickerLauncher.launch(DocumentSource.PdfFile)
            }
        }
    }

    /** Hands the picked PDF to the KYC home for the upload, the capture screen is left behind. */
    private fun onPdfPicked(uri: Uri?) {
        if (uri == null) {
            Timber.d("No PDF is picked for the physical contract")
            return
        }

        AmaniMainActivity.hideSelectButton()

        findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
            HomeKYCResultModel(
                docID = args.value.dataModel.version!!.documentId!!,
                docType = args.value.dataModel.version!!.type,
                genericDocumentFlow = GenericDocumentFlow.DataFromGallery(arrayListOf(uri))
            )

        findNavController().clearBackStack(R.id.homeKYCFragment)
        findNavController().popBackStack(R.id.homeKYCFragment, false)
    }

    override fun onPause() {
        super.onPause()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            AmaniMainActivity.hideSelectButton()
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            AmaniMainActivity.showSelectButton()
        }
    }
}
