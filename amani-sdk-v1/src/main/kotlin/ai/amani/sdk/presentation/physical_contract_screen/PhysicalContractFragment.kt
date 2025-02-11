package ai.amani.sdk.presentation.physical_contract_screen

import ai.amani.BuildConfig
import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentPhysicalContractBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.debugToast
import ai.amani.sdk.extentions.replaceChildFragmentWithoutBackStack
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.modules.document.DocBuilder
import ai.amani.sdk.modules.document.interfaces.IDocumentCallBack
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.utils.AmaniDocumentTypes
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber


/**
 * @Author: zekiamani
 * @Date: 1.03.2023
 */
class PhysicalContractFragment: Fragment() {

    private lateinit var binding: FragmentPhysicalContractBinding
    private var args = navArgs<PhysicalContractFragmentArgs>()

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
                            findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
                                HomeKYCResultModel(
                                    docID = args.value.dataModel.version!!.documentId!!,
                                    docType = args.value.dataModel.version!!.type,
                                    )

                            findNavController().clearBackStack(R.id.homeKYCFragment)
                            findNavController().popBackStack(R.id.homeKYCFragment, false)

                            MainActivity.hideSelectButton()

                        } else {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(requireContext(), "Generic document result fale", Toast.LENGTH_LONG).show()
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
            findNavController().popBackStack()
        }
    }

    private fun toolBar() {
        setToolBarTitle(
            args.value.dataModel.version!!.steps[0].captureTitle,
            args.value.dataModel.generalConfigs!!.appFontColor
        )
    }

    private fun clickEvents() {
        MainActivity.addSelectButtonListener {
            pickPdfFileFromStorage()
        }
    }

    private fun pickPdfFileFromStorage() {
        getContent?.launch("application/pdf")
    }

    private var getContent: ActivityResultLauncher<String>? = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Timber.d("PDF file is taken from gallery")
            if (it.scheme == "content") {
                requireContext().contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val pdfData = requireActivity().contentResolver.openInputStream(uri)?.readBytes()
                    if (pdfData == null) {
                        Toast.makeText(requireContext(), "PDF could not take", Toast.LENGTH_LONG).show()
                        return@let
                    }
                    val listOfUri = arrayListOf(uri)

                    MainActivity.hideSelectButton()

                    findNavController().getBackStackEntry(R.id.homeKYCFragment).savedStateHandle[AmaniDocumentTypes.type] =
                        HomeKYCResultModel(
                            docID = args.value.dataModel.version!!.documentId!!,
                            docType = args.value.dataModel.version!!.type,
                            genericDocumentFlow = GenericDocumentFlow.DataFromGallery(listOfUri)
                        )

                    findNavController().clearBackStack(R.id.homeKYCFragment)
                    findNavController().popBackStack(R.id.homeKYCFragment, false)

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        MainActivity.hideSelectButton()
    }

    override fun onResume() {
        super.onResume()
        MainActivity.showSelectButton()
    }
}