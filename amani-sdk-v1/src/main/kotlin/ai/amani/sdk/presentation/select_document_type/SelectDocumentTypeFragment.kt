package ai.amani.sdk.presentation.select_document_type

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentSelectDocumentTypeBinding
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.presentation.common.document_picker.DocumentPickerLauncher
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.presentation.select_document_type.adapter.DocumentAdapter
import ai.amani.sdk.utils.ColorConstant
import ai.amani.sdk.extentions.setToolBarTitle
import ai.amani.sdk.presentation.home_kyc.HomeKYCFragmentDirections
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import datamanager.model.config.ResGetConfig
import datamanager.model.config.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 12.09.2022
 */
class SelectDocumentTypeFragment : Fragment(), DocumentAdapter.IDocumentListener {

    private lateinit var binding: FragmentSelectDocumentTypeBinding
    private val args: SelectDocumentTypeFragmentArgs by navArgs()
    private val viewModel: SelectDocumentTypeViewModel by viewModels()

    /** Document whose picker is open, kept to report its upload back to the KYC home. */
    private var pickedVersion: Version? = null

    /**
     * Picker for the documents whose `documentSource` is not the camera. Registered eagerly
     * because activity result launchers cannot be registered once the fragment is started.
     */
    private val documentPickerLauncher = DocumentPickerLauncher(this) { uri ->
        onDocumentPicked(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_document_type, container, false)
        binding = FragmentSelectDocumentTypeBinding.bind(view)
        setCustomUI(
            args.dataModel.versionList,
            args.dataModel.generalConfigs
        )
        return view
    }

    override fun onOnItemSelected(version: Version?) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.navigateScreen(
                version!!
            ) {
                when (it) {
                    ScreenRoutes.IDFrontSideScreen -> {
                        val action =
                            SelectDocumentTypeFragmentDirections.actionSelectDocumentTypeFragmentToIDCaptureFrontSideFrag(
                                ConfigModel(
                                    version = version,
                                    generalConfigs =  args.dataModel.generalConfigs!!.generalConfigs,
                                    featureConfig = args.dataModel.featureConfig
                                )
                            )
                        findNavController().navigateSafely(action)
                    }

                    is ScreenRoutes.DocumentPickerScreen -> {
                        // The document is picked from storage, so the capture screen is skipped.
                        pickedVersion = version
                        documentPickerLauncher.launch(it.source)
                    }

                    else -> {
                        val action =
                            SelectDocumentTypeFragmentDirections.actionSelectDocumentTypeFragmentToPhysicalContractFragment(
                                ConfigModel(
                                    version = version,
                                    generalConfigs = args.dataModel.generalConfigs!!.generalConfigs
                                )
                            )

                        findNavController().navigateSafely(action)
                    }
                }
            }
        }
    }

    /**
     * Hands the picked file to the KYC home for the upload, the same way the physical contract
     * screen does after a capture. Leaving the picker empty keeps the selection screen open.
     */
    private fun onDocumentPicked(uri: android.net.Uri?) {
        val version = pickedVersion
        pickedVersion = null

        if (uri == null || version == null) {
            Timber.d("No document is picked from storage")
            return
        }

        findNavController().getBackStackEntry(R.id.homeKYCFragment)
            .savedStateHandle[AmaniDocumentTypes.type] = HomeKYCResultModel(
            docID = version.documentId,
            docType = version.type,
            genericDocumentFlow = GenericDocumentFlow.DataFromGallery(arrayListOf(uri))
        )

        findNavController().clearBackStack(R.id.homeKYCFragment)
        findNavController().popBackStack(R.id.homeKYCFragment, false)
    }

    private fun setCustomUI(
        versionList: List<Version?>?,
        appConfig: ResGetConfig?
    ) {
        if (appConfig == null || versionList.isNullOrEmpty()) return
        val color: String = if (appConfig.generalConfigs!!.appFontColor != null
        ) appConfig.generalConfigs?.appFontColor!! else ColorConstant.COLOR_BLACK

        val matchingStepConfig =
            appConfig.stepConfigs.find { it.id == args.dataModel.currentVersionID }

        setToolBarTitle(
            matchingStepConfig?.documentSelectionTitle,
            appConfig.generalConfigs!!.appFontColor
        )

        binding.text.setTextProperty(matchingStepConfig?.documentSelectionDescription, color)
        binding.parentLayout.setBackgroundColor(
            Color.parseColor(
                appConfig.generalConfigs?.appBackground
            )
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.isNestedScrollingEnabled = false
        val modifiedVersionList = versionList.filter {
            it?.isHidden == false || it?.isHidden == null
        }
        binding.recyclerView.adapter = DocumentAdapter(
            modifiedVersionList,
            this,
            args.dataModel.generalConfigs!!.generalConfigs
        )
    }
}