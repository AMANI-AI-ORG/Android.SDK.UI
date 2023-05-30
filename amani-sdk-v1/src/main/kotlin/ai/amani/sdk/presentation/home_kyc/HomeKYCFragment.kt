package ai.amani.sdk.presentation.home_kyc

import ai.amani.amani_sdk.BuildConfig
import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentHomeKycBinding
import ai.amani.sdk.Amani
import ai.amani.sdk.App
import ai.amani.sdk.di.AppContainer
import ai.amani.sdk.extentions.*
import ai.amani.sdk.model.*
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.presentation.home_kyc.adapter.KYCAdapter
import ai.amani.sdk.utils.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import datamanager.model.customer.Rule
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 5.09.2022
 */

class HomeKYCFragment : Fragment(), KYCAdapter.IKYCListener {

    private lateinit var binding: FragmentHomeKycBinding
    private var mAdapter: KYCAdapter? = null
    private lateinit var appContainer: AppContainer
    private val viewModel: HomeKYCViewModel by activityViewModels { HomeKYCViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_kyc, container, false)
        binding = FragmentHomeKycBinding.bind(view)
        getIntent()
        appContainer = (requireActivity().application as App).appContainer
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
                }
            }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.hideSelectButton{
            debugToast("There is an exception while hiding the select button via $it")
        }
    }

    private fun getIntent() {
        val extras = requireActivity().intent.extras
        extras?.let {
            // TODO: Migrate it
            val config = it.getParcelable<RegisterConfig>(AppConstant.REGISTER_CONFIG)
            viewModel.setRegisterConfigModel(
                registerConfig = config
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
                    showToast("Login failed due to httpsErrorCode: ${it.httpsErrorCode}")
                    binding.progressLoaderCentered.hide()
                    requireActivity().finish()
                }
            }
        }

        viewModel.logicEvent.observe(viewLifecycleOwner) {
            Timber.i("Logic event $it is triggered")
            when(it) {
               is HomeKYCLogicEvent.Refresh -> {
                   debugToast("Refresh triggered")
                   mAdapter?.updateDocumentList(it.documentList)
                }

               is HomeKYCLogicEvent.Finish.ProfileApproved -> {

                    Timber.d("Profile is APPROVED!")

                    findNavController().clearBackStack(R.id.homeKYCFragment)
                    val action =
                        HomeKYCFragmentDirections.actionHomeKYCFragmentToCongratulationsFragment(
                            ConfigModel(
                                viewModel.getVersion(),
                                viewModel.getAppConfig()!!.generalConfigs
                            )
                        )
                    findNavController().navigate(action)
                }

                is HomeKYCLogicEvent.Finish.LoginFailed -> {
                    Timber.d("Login is failed, httpErrorCode: ${it.httpErroCode}")
                    val returnIntent = Intent()
                    returnIntent.putExtra(AppConstant.KYC_RESULT,
                    KYCResult(
                        httpErrorCode = it.httpErroCode
                    ))
                    requireActivity().finish()
                    Timber.d("KYC activity is finished")
                }

                is HomeKYCLogicEvent.Finish.OnError -> {

                    Timber.d("Login is failed due to exception: ${it.exception}")

                    val returnIntent = Intent()
                    returnIntent.putExtra(AppConstant.KYC_RESULT,
                        KYCResult(
                            generalException = it.exception
                        ))
                    requireActivity().finish()
                    Timber.d("KYC activity is finished")
                }

                else -> {}
            }
        }
    }

    private fun setCustomUI() {
        
        renderRecyclerView()

        setFlag(viewModel.getDocList()!!)

        customizeToolBar(
            viewModel.getAppConfig()!!.generalConfigs.topBarBackground,
            viewModel.getAppConfig()!!.generalConfigs.topBarFontColor,
            viewModel.getAppConfig()!!.generalConfigs.topBarFontColor,
            viewModel.getAppConfig()!!.generalConfigs.mainTitleText
        )

        binding.imageViewPoweredByAmani.setColorFilter(Color.parseColor(viewModel.getAppConfig()!!.generalConfigs.appFontColor))

        binding.recyclerViewLayout.setBackgroundColor(Color.parseColor(viewModel.getAppConfig()!!.generalConfigs.appBackground))
        binding.recyclerViewLayout.show()
    }

    private fun renderRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            RecyclerView.VERTICAL,
            false
        )
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.isNestedScrollingEnabled = false

       // val list = viewModel.getDocList()!!.sortedWith(compareBy(ai.amani.sdk.model.customer.Rule::sortOrder, ai.amani.sdk.model.customer.Rule::sortOrder))

        mAdapter = KYCAdapter(
            viewModel.getDocList()!!,
            viewModel.getAppConfig()!!,
            this,
            requireContext()
        )
        binding.recyclerView.adapter = mAdapter
    }

    override fun onOnItemSelected(version: ai.amani.sdk.model.customer.Rule) {
        viewModel.navigateScreen(version!!) {
            when (it) {
                ScreenRoutes.SelfieCaptureScreen -> {
                    val action =
                        HomeKYCFragmentDirections.actionHomeKYCFragmentToSelfieCaptureFragment(
                            ConfigModel(
                                viewModel.getVersion(),
                                viewModel.getAppConfig()!!.generalConfigs
                            )
                        )

                    findNavController().navigate(action)
                }

                ScreenRoutes.IDFrontSideScreen -> {
                    findNavController().navigate(R.id.action_homeKYCFragment_to_IDCaptureFrontSideFrag)
                }

                ScreenRoutes.SelectDocumentTypeScreen -> {
                    val action =
                        HomeKYCFragmentDirections.actionHomeKYCFragmentToSelectDocumentTypeFragment(
                            SelectDocumentTypeModel(
                                viewModel.getVersionList()!!,
                                viewModel.getAppConfig()
                            )
                        )
                    findNavController().navigate(action)
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
                    findNavController().navigate(action)

                }

                ScreenRoutes.SignatureScreen -> {

                    val action = HomeKYCFragmentDirections.actionHomeKYCFragmentToSignatureFragment(
                        configModel = ConfigModel(
                            version = viewModel.getVersion(),
                            generalConfigs = viewModel.getAppConfig()!!.generalConfigs
                        )
                    )

                    findNavController().navigate(action)

                }

                ScreenRoutes.PhysicalContractScreen -> {

                    val action =
                        HomeKYCFragmentDirections.actionHomeKYCFragmentToPhysicalContractFragment(
                            ConfigModel(
                                viewModel.getVersion(),
                                viewModel.getAppConfig()!!.generalConfigs
                            )
                        )

                    findNavController().navigate(action)
                }

                else -> {}
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
}