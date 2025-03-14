package ai.amani.sdk.presentation.home_kyc

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentHomeKycBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.debugToast
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.logUploadResult
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.parcelable
import ai.amani.sdk.extentions.show
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.HomeKYCResultModel
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.model.NFCScanScreenModel
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.model.SelectDocumentTypeModel
import ai.amani.sdk.presentation.AmaniMainActivity
import ai.amani.sdk.presentation.common.NavigationCommands
import ai.amani.sdk.presentation.home_kyc.adapter.KYCAdapter
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 5.09.2022
 */

class HomeKYCFragment : Fragment(), KYCAdapter.IKYCListener {

    private var _binding: FragmentHomeKycBinding? = null
    private val binding get() = _binding!!
    private var mAdapter: KYCAdapter? = null
    private val viewModel: HomeKYCViewModel by activityViewModels { HomeKYCViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_kyc, container, false)
        _binding = FragmentHomeKycBinding.bind(view)
        getIntent()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loginSDK(activity = requireActivity())

        observe()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<HomeKYCResultModel>("type")
            ?.observe(viewLifecycleOwner) {

                findNavController().currentBackStackEntry?.savedStateHandle?.remove<HomeKYCResultModel>("type")

                when (it.docID) {
                    AmaniDocumentTypes.IDENTIFICATION, AmaniDocumentTypes.PASSPORT, AmaniDocumentTypes.DRIVING_LICENSE -> {

                        //Uploading the ID as alone, single.
                        //Do not forget to configure it back as true if you want to make upload request ID plus NFC together
                        Amani.sharedInstance().IDCapture().withNFC(false)

                        viewModel.uploadID(
                            requireActivity(),
                            it.docType,
                            onCompleted = { uploadResultModel ->

                                logUploadResult(uploadResultModel, it.docType)

                            }
                        )
                    }

                    AmaniDocumentTypes.SELFIE -> {

                        viewModel.uploadSelfie(
                            requireActivity(),
                            it.docType,
                            it.selfieType,
                            onCompleted = { uploadResultModel ->

                                logUploadResult(uploadResultModel, it.docType)

                            }
                        )
                    }

                    AmaniDocumentTypes.NFC -> {

                        if (it.nfcOnly) {
                            // This section is providing OnlyNFC section
                            Timber.e(it.docType + ": uploading only NFC without ID")

                            viewModel.uploadNFCOnly(
                                requireActivity(),
                                it.docType,
                                onCompleted = { uploadResultModel ->

                                    logUploadResult(uploadResultModel, it.docType)

                                }
                            )

                        } else {
                            // This section is providing NFC + ID together
                            Timber.e(it.docType + ": uploading NFC && ID together")

                            // Enabling withNFC, will be handle to upload NFC and IDCapture data in one req
                            Amani.sharedInstance().IDCapture().withNFC(true)

                            viewModel.uploadID(
                                requireActivity(),
                                it.docType,
                                onCompleted = { uploadResultModel ->

                                    logUploadResult(uploadResultModel, it.docType)

                                }
                            )
                        }
                    }

                    AmaniDocumentTypes.SIGNATURE -> {
                        viewModel.uploadSignature { signatureUploadResult ->
                            logUploadResult(signatureUploadResult, AmaniDocumentTypes.SIGNATURE)
                        }
                    }

                    AmaniDocumentTypes.PHYSICAL_CONTRACT -> {
                        viewModel.uploadDocument(
                            activity = requireActivity(),
                            docType = it.docType,
                            genericDocumentFlow = it.genericDocumentFlow,
                            onCompleted = { documentUploadResult ->
                                logUploadResult(documentUploadResult, AmaniDocumentTypes.PHYSICAL_CONTRACT)
                            }
                        )
                    }

                    else -> {
                        viewModel.uploadDocument(
                            activity = requireActivity(),
                            docType = it.docType,
                            genericDocumentFlow = it.genericDocumentFlow,
                            onCompleted = { documentUploadResult ->
                                logUploadResult(documentUploadResult, AmaniDocumentTypes.PHYSICAL_CONTRACT)
                            }
                        )
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            AmaniMainActivity.hideSelectButton()
            viewModel.listenAmaniEvents()
        }
    }

    private fun getIntent() {
        val extras = requireActivity().intent.extras
        extras?.let {
            val config = it.parcelable<RegisterConfig>(AppConstant.REGISTER_CONFIG)
            val featureConfig = it.parcelable<FeatureConfig>(AppConstant.FEATURE_CONFIG)
            viewModel.setConfigModels(
                registerConfig = config,
                featureConfig = featureConfig
            )
        }
    }

    private fun observe() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                HomeKYCState.Loading -> binding.progressLoaderCentered.show()
                HomeKYCState.Loaded -> {
                    binding.progressLoaderCentered.hide()
                    setCustomUI()
                }
                is HomeKYCState.Error -> {
                    binding.progressLoaderCentered.hide()
                }
            }
        }

        viewModel.logicEvent.observe(viewLifecycleOwner) {
            Timber.i("Logic event $it is triggered")
            when(it) {
                is HomeKYCLogicEvent.Refresh -> {
                    debugToast("Refresh triggered")
                    viewLifecycleOwner.lifecycleScope.launch {
                        mAdapter?.updateDocumentList(it.documentList)
                    }
                }

                is HomeKYCLogicEvent.Finish.ProfileApproved -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        Timber.d("Profile is APPROVED")
                        findNavController().clearBackStack(R.id.homeKYCFragment)
                        val action =
                            HomeKYCFragmentDirections.actionHomeKYCFragmentToCongratulationsFragment(
                                ConfigModel(
                                    viewModel.getVersion(),
                                    viewModel.getAppConfig()!!.generalConfigs
                                )
                            )
                        findNavController().navigateSafely(action)
                    }
                }

                is HomeKYCLogicEvent.Finish.OnError -> {
                    Timber.d("Login is failed, httpErrorCode: ${it.errorCode}")
                    val returnIntent = Intent()
                    returnIntent.putExtra(AppConstant.KYC_RESULT,
                        KYCResult(
                            errorCode = it.errorCode
                        ))
                    requireActivity().setResult(Activity.RESULT_OK, returnIntent)
                    requireActivity().finish()
                    Timber.d("KYC activity is finished")
                }

                is HomeKYCLogicEvent.Finish.OnException -> {

                    Timber.d("Login is failed due to exception: ${it.exception}")

                    val returnIntent = Intent()
                    returnIntent.putExtra(AppConstant.KYC_RESULT,
                        KYCResult(
                            generalException = it.exception
                        ))
                    requireActivity().setResult(Activity.RESULT_OK, returnIntent)
                    requireActivity().finish()
                    Timber.d("KYC activity is finished")
                }

                else -> {}
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.navigateTo.collect{
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (it is NavigationCommands.NavigateDirections) {
                        findNavController().navigateSafely(it.direction)
                    }
                }
            }
        }
    }

    private fun setCustomUI() {
        
        renderRecyclerView()

        setFlag(viewModel.getDocList()!!)

        customizeToolBar(
            viewModel.getAppConfig()!!.generalConfigs?.topBarBackground,
            viewModel.getAppConfig()!!.generalConfigs?.topBarFontColor,
            viewModel.getAppConfig()!!.generalConfigs?.topBarFontColor,
            viewModel.getAppConfig()!!.generalConfigs?.mainTitleText
        )

        binding.imageViewPoweredByAmani.setColorFilter(Color.parseColor("#909090"))

        binding.uploadDocumentDescription.let {
            it.text = viewModel.getAppConfig()?.generalConfigs?.mainDescriptionText
            it.setTextColor(Color.parseColor(
                viewModel.getAppConfig()!!.generalConfigs?.appFontColor
            ))
        }

        binding.parentLayout.setBackgroundColor(Color.parseColor(viewModel.getAppConfig()!!.generalConfigs?.appBackground))
        binding.parentLayout.show()
    }

    private fun renderRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            RecyclerView.VERTICAL,
            false
        )
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.isNestedScrollingEnabled = false

        mAdapter = KYCAdapter(
            viewModel.getDocList()!!,
            viewModel.getAppConfig()!!,
            this,
            requireContext()
        )
        binding.recyclerView.adapter = mAdapter
    }

    override fun onOnItemSelected(version: ai.amani.sdk.model.customer.Rule, adapterPosition: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.navigateScreen(version!!, adapterPosition) {
                when (it) {
                    ScreenRoutes.SelfieCaptureScreen -> {
                        val action =
                            HomeKYCFragmentDirections.actionHomeKYCFragmentToSelfieCaptureFragment(
                                ConfigModel(
                                    viewModel.getVersion(),
                                    viewModel.getAppConfig()!!.generalConfigs
                                )
                            )

                        findNavController().navigateSafely(action)
                    }

                    ScreenRoutes.IDFrontSideScreen -> {
                        val action = HomeKYCFragmentDirections.actionHomeKYCFragmentToIDCaptureFrontSideFrag(
                            dataModel = ConfigModel(
                                viewModel.getVersion(),
                                viewModel.getAppConfig()!!.generalConfigs
                            )
                        )
                        findNavController().navigateSafely(action)
                    }

                    ScreenRoutes.SelectDocumentTypeScreen -> {
                        val action =
                            HomeKYCFragmentDirections.actionHomeKYCFragmentToSelectDocumentTypeFragment(
                                SelectDocumentTypeModel(
                                    versionList =  viewModel.getVersionList()!!,
                                    generalConfigs = viewModel.getAppConfig(),
                                    currentVersionID =  version.id!!,
                                    featureConfig = viewModel.featureConfigModel()
                                )
                            )
                        findNavController().navigateSafely(action)
                    }

                    ScreenRoutes.NFCScanScreen -> {

                        val action =
                            HomeKYCFragmentDirections.actionHomeKYCFragmentToNFCScanFragment(
                                NFCScanScreenModel(
                                    ConfigModel(
                                        viewModel.getVersion(),
                                        viewModel.getAppConfig()!!.generalConfigs
                                    ),

                                    viewModel.getMRZModel()!!,
                                    nfcOnly = true
                                )
                            )
                        findNavController().navigateSafely(action)

                    }

                    ScreenRoutes.SignatureScreen -> {

                        val action = HomeKYCFragmentDirections.actionHomeKYCFragmentToSignatureFragment(
                            configModel = ConfigModel(
                                version = viewModel.getVersion(),
                                generalConfigs = viewModel.getAppConfig()!!.generalConfigs
                            )
                        )

                        findNavController().navigateSafely(action)

                    }

                    ScreenRoutes.PhysicalContractScreen -> {

                        pdfPickerLauncher?.launch("application/pdf")
                    }

                    else -> {}
                }
            }
        }
    }

    //TODO Setting flag and all steps completed case will be handled by viewModel
    private fun setFlag(docList: List<ai.amani.sdk.model.customer.Rule>) {
        var count = 0
        var flags = 0

        for (r in docList) {
            if ((r.status == AppConstant.STATUS_APPROVED || r.status == AppConstant.STATUS_PENDING_REVIEW) && r.phase == 0) {
                flags++
            }
            if (r.status == AppConstant.STATUS_APPROVED || r.status == AppConstant.STATUS_PENDING_REVIEW) {
                count++
            }
        }
        if (count == docList.size) {

        }
        mAdapter?.setFlags(flags)
    }

    private var pdfPickerLauncher: ActivityResultLauncher<String>? = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (it.scheme == "content") {
                requireContext().contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val pdfData = requireActivity().contentResolver.openInputStream(uri)?.readBytes()
                    if (pdfData == null) {
                        Toast.makeText(requireContext(), "PDF could not take", Toast.LENGTH_LONG).show()
                        return@let
                    } else {
                        val listOfUri = arrayListOf(uri)

                        viewModel.uploadDocument(
                            activity = requireActivity(),
                            genericDocumentFlow = GenericDocumentFlow.DataFromGallery(listOfUri),
                            onCompleted = { documentUploadResult ->
                                logUploadResult(documentUploadResult, AmaniDocumentTypes.PHYSICAL_CONTRACT)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
