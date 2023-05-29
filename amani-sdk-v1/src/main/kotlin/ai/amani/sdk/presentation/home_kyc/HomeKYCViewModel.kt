package ai.amani.sdk.presentation.home_kyc

import ai.amani.amani_sdk.R
import ai.amani.sdk.Amani
import ai.amani.sdk.data.mapper.StepsResultModelMapper
import ai.amani.sdk.data.repository.config.ConfigRepositoryImp
import ai.amani.sdk.data.repository.customer.CustomerDetailRepoImp
import ai.amani.sdk.data.repository.document.DocumentRepoImp
import ai.amani.sdk.data.repository.document.DocumentRepository
import ai.amani.sdk.data.repository.id_capture.IDCaptureRepoImp
import ai.amani.sdk.data.repository.login.LoginRepoImp
import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.data.repository.selfie_capture.SelfieCaptureRepoImp
import ai.amani.sdk.data.repository.signature.SignatureRepoImp
import ai.amani.sdk.extentions.getStepConfig
import ai.amani.sdk.extentions.sort
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.*
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.model.amani_events.upload_result.UploadResult
import ai.amani.sdk.model.customer.CustomerDetailResult
import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.modules.document.FileWithType
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import ai.amani.sdk.presentation.selfie.SelfieType
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.utils.AppConstant
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import datamanager.model.config.DocumentList
import datamanager.model.config.ResGetConfig
import datamanager.model.config.StepConfig
import datamanager.model.config.Version
import datamanager.model.customer.Errors
import timber.log.Timber
import java.lang.Exception
import java.lang.NullPointerException

/**
 * @Author: zekiamani
 * @Date: 5.09.2022
 */


open class HomeKYCViewModel constructor(
    private val loginRepository: LoginRepoImp,
    private val configRepository: ConfigRepositoryImp,
    private val customerDetailRepoImp: CustomerDetailRepoImp,
    private val selfieCaptureRepoImp: SelfieCaptureRepoImp,
    private val idCaptureRepoImp: IDCaptureRepoImp,
    private val nfcRepository: NFCRepositoryImp,
    private val signatureRepository: SignatureRepoImp,
    private val documentRepository: DocumentRepoImp
) : ViewModel() {

    /*Responsible for UI State Management */
    private var _uiState: MutableLiveData<HomeKYCState> = MutableLiveData(HomeKYCState.Loading)
    val uiState: LiveData<HomeKYCState> = _uiState

    /*Responsible for Logic Events */
    private var _logicEvent: MutableLiveData<HomeKYCLogicEvent> = MutableLiveData(HomeKYCLogicEvent.Empty)
    val logicEvent: LiveData<HomeKYCLogicEvent> = _logicEvent


    private var selectedStepNumber = 0

    private var profileInfoModel : ProfileInfoModel = ProfileInfoModel()


    fun getAppConfig(): ResGetConfig? = CachingHomeKYC.appConfig

    fun getCustomerDetail(): CustomerDetailResult? = CachingHomeKYC.customerDetail

    fun getDocList(): ArrayList<Rule> {
       val list = CachingHomeKYC.customerDetail!!.rules as ArrayList<Rule>?
        return list!!.sort()
    }

    fun getVersion(): Version? = CachingHomeKYC.version

    fun getVersionList(): MutableList<Version>? = CachingHomeKYC.versionsList

    fun loginSDK(
        activity: Activity
    ) {

        if (profileInfoModel.registerConfig == null) {
            _logicEvent.postValue(HomeKYCLogicEvent.Finish.OnError(NullPointerException()))
            return
        }

        if (_uiState.value == HomeKYCState.Loaded) {
            Timber.d("Login is already done")
            return
        }

        loginRepository.login(
            activity = activity,
            tcNumber = getProfileInfoModel().registerConfig!!.tcNumber,
            token = getProfileInfoModel().registerConfig!!.token,
            lang = getProfileInfoModel().registerConfig!!.language,
            location = getProfileInfoModel().registerConfig!!.location,
            onStart = {
                _uiState.value = HomeKYCState.Loading
            },

            onCompleted = {
                if (it.isSuccess) {
                    getApplicationConfig()
                } else {
                    it.error?.let { error ->
                        Timber.e("Login is failed, HttpsErrorCode: $error")

                        _uiState.value = HomeKYCState.Error(error)
                    } ?: run {
                        Timber.e("Login is failed, HttpsErrorCode: null")

                        _uiState.value = HomeKYCState.Error()
                    }
                }
            }
        )
    }

    /** Gets the application config from saved data */
    private fun getApplicationConfig() {
        configRepository.getAppConfig(
            onStart = {
                Timber.d("Get app config is started")
            },

            onError = {
                Timber.e("Get app config is failed due to error code:$it")
            },

            onComplete = {
                Timber.i("Get app config is succeed")
                CachingHomeKYC.appConfig = it
                fetchCustomerDetail()
            }
        )
    }

    /** Fetches the current up to date data from API */
    private fun fetchCustomerDetail() {
        customerDetailRepoImp.getCustomerDetail(
            onStart = {
            },

            onError = {
            },

            onComplete = {
                checkCustomerStatus(it)
                CachingHomeKYC.customerDetail = it
                _uiState.value = HomeKYCState.Loaded
            }
        )
    }

    /** Checks the fetched up to date customer data to process it. If customer status is already
     * APPROVED, postValue to [HomeKYCLogicEvent.Finish.ProfileApproved] as [_logicEvent]
     * @param customerDetail: Current customer data that fetched
     */
    private fun checkCustomerStatus(customerDetail: CustomerDetailResult?) {
        if (customerDetail == null) return
        if (customerDetail.status == null) return
        if (customerDetail.status.equals(STATUS_APPROVED, ignoreCase = true)){
            _logicEvent.postValue(HomeKYCLogicEvent.Finish.ProfileApproved)
        }
    }

    /** Provides the upload according to SelfieType
     *  @param activity: Activity pointer of current fragment
     *  @param docType: Type of the current document as a string value
     *  @param selfieType: SelfieType sealed class that represents the current SelfieType according
     *  to remote config data.
     *  0 -> Auto Selfie
     *  -1 -> Manual Selfie
     *  else -> PoseEstimation
     *  @param onCompleted : Unit callback when process completed
     */
    fun uploadSelfie(
        activity: FragmentActivity,
        docType: String,
        selfieType: SelfieType,
        onCompleted: (uploadRes: UploadResultModel) -> Unit
    ) {

        when(selfieType) {

            is SelfieType.PoseEstimation -> {
                selfieCaptureRepoImp.uploadSelfiePoseEstimation(
                    activity,
                    docType,
                    onStart = {
                        setLoaderStatus()
                    },
                    onCompleted
                )

            }

            is SelfieType.Auto -> {
                selfieCaptureRepoImp.uploadAutoSelfie(
                    activity,
                    docType,
                    onStart = {
                        setLoaderStatus()
                    },
                    onCompleted
                )
            }

            is SelfieType.Manual -> {
                selfieCaptureRepoImp.uploadManualSelfie(
                    activity,
                    docType,
                    onStart = {
                        setLoaderStatus()
                    },
                    onCompleted
                )
            }

            else -> {}
        }
    }

    /** Upload only ID
     *  @param activity: Activity pointer of current fragment
     *  @param docType: Current document type as a String
     *  @param onCompleted: Unit callback when process is done
     */
    fun uploadID(
        activity: FragmentActivity,
        docType: String,
        onCompleted: (uploadRes: UploadResultModel) -> Unit
    ) {
        idCaptureRepoImp.upload(
            activity,
            docType,
            onStart = {
                setLoaderStatus()
            },
            onCompleted
        )
    }

    /** Upload only NFC
     *  @param activity: Activity pointer of current fragment
     *  @param docType: Current document type as a String
     *  @param onCompleted: Unit callback when process is done
     */
    fun uploadNFCOnly(
        activity: FragmentActivity,
        type: String,
        onCompleted: (uploadRes: UploadResultModel) -> Unit
    ) {
        setLoaderStatus()
        nfcRepository.upload(
            activity,
            type,
            onCompleted
        )
    }

    /** Upload digital signature that taken from SignatureFragment
     * @param onCompleted : Upload is completed
     */
    fun uploadSignature(
        onCompleted: (uploadRes: UploadResultModel) -> Unit
    ) {
        setLoaderStatus()
        signatureRepository.uploadSignature(
            onCompleted
        )
    }

    /** Fetches all current rules from API to check current user state
     * @param uploadRes: Response of the uplaod
     * @param onComplete: Unit callback when process is done
     */
    fun fetchRules(
       // uploadRes: UploadResultModel,
        onComplete: (result: CustomerDetailResult) -> Unit
    ) {
        customerDetailRepoImp.getCustomerDetail(
            onStart = {
            },
            onComplete = {
                onComplete.invoke(it)
                clearLogicState()
            },
            onError = {
            }
        )
    }

    /**
     * Uploads the GenericDocument data
     */
    fun uploadDocument(
        activity: FragmentActivity,
        genericDocumentFlow: GenericDocumentFlow = GenericDocumentFlow.DataFromCamera ,
        docType: String,
        onCompleted: (uploadRes: UploadResultModel) -> Unit
    ) {
        setLoaderStatus()

        documentRepository.upload(
            activity = activity,
            docType = docType,
            onStart = {
                //
            },
            onComplete = onCompleted,
            genericDocumentFlow = genericDocumentFlow
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {

                return HomeKYCViewModel(
                    LoginRepoImp(),
                    ConfigRepositoryImp(),
                    CustomerDetailRepoImp(),
                    SelfieCaptureRepoImp(),
                    IDCaptureRepoImp(),
                    NFCRepositoryImp(),
                    SignatureRepoImp(),
                    DocumentRepoImp()
                ) as T
            }
        }
    }

    fun navigateScreen(
        rule: Rule,
        route: (route: ScreenRoutes) -> Unit
    ) {
        //Creating VersionList
        setVersionList(rule)

        //Setting current selecting version from VersionList
        setCurrentVersion(
            CachingHomeKYC.versionsList
        )

        if (CachingHomeKYC.versionsList.isNullOrEmpty()) {
            _uiState.value = HomeKYCState.Error(R.string.error_upload_source)
            return
        }

        val documentID = CachingHomeKYC.version!!.documentId
        if (rule.status != AppConstant.STATUS_PROCESSING
            && rule.status != STATUS_APPROVED
        ) {
            if (CachingHomeKYC.versionsList!!.size == 1) {
                when (documentID) {
                    AmaniDocumentTypes.SELFIE -> {
                        route.invoke(ScreenRoutes.SelfieCaptureScreen)
                    }

                    AmaniDocumentTypes.PASSPORT-> {
                        route.invoke(ScreenRoutes.IDFrontSideScreen)
                    }

                    AmaniDocumentTypes.NFC -> {
                        if (profileInfoModel.mrzModel != null) {
                            route.invoke(ScreenRoutes.NFCScanScreen)
                        } else {
                            // MRZ must be provided for every user from client side to start NFCOnly
                            // process properly. In this section no need to show pop up to user for now
                            Timber.e("MRZModel field is empty to start process of NFCOnly")
                        }
                    }

                    AmaniDocumentTypes.SIGNATURE -> {
                        route.invoke(ScreenRoutes.SignatureScreen)
                    }

                    AmaniDocumentTypes.PHYSICAL_CONTRACT -> {
                        route.invoke(ScreenRoutes.PhysicalContractScreen)
                    }
                }
            } else {
                when (documentID) {

                    AmaniDocumentTypes.PASSPORT -> {
                        route.invoke(ScreenRoutes.SelectDocumentTypeScreen)
                    }

                    AmaniDocumentTypes.IDENTIFICATION -> {
                        route.invoke(ScreenRoutes.SelectDocumentTypeScreen)
                    }

                    AmaniDocumentTypes.DRIVING_LICENSE -> {
                        route.invoke(ScreenRoutes.SelectDocumentTypeScreen)
                    }

                    AmaniDocumentTypes.NFC -> {
                        if (profileInfoModel.mrzModel != null) {
                            route.invoke(ScreenRoutes.NFCScanScreen)
                        } else {
                            // MRZ must be provided for every user from client side to start NFCOnly
                            // process properly. In this section no need to show pop up to user for now
                            Timber.e("MRZModel field is empty to start process of NFCOnly")
                        }
                    }
                }
            }
        }
    }

    /** Sets Version List accordingly selected rule
     * @param rule : Selected rule from adapter
     **/
    private fun setVersionList(rule : Rule) {
        CachingHomeKYC.versionsList = mutableListOf()
        selectedStepNumber = rule.sortOrder!!
        val stepConfig: StepConfig = CachingHomeKYC.appConfig!!.getStepConfig(rule.sortOrder!!)
        val documentLists: List<DocumentList> =
            stepConfig.getmDocuments()
        for (documentList in documentLists) {
            for (version in documentList.versions) {
                version.setDocumentId(documentList.id)
                version.setStepId(rule.sortOrder!!)
            }
            //Creating version list accordingly sort order from remote config
            //Sort order is a row number of current document
            CachingHomeKYC.versionsList!!.addAll(documentList.versions)

        }
    }

    /** Sets loader status for current step
     *  that show the button loader */
    private fun setLoaderStatus() {
        repeat(getDocList()!!.size) {
            if (getDocList()!![it]!!.sortOrder == selectedStepNumber) {
                getDocList()!![it]!!.isShowLoader = true
            }
        }
    }

    /** Setting the current version accordingly selected rule
     * @param versionList:  created versionList that contains all version
     * @param selectedRule: Currently selected rule, when user select
     * a document it will be created where onItemSelected section on KYCAdapter.
     */
    private fun setCurrentVersion(
        versionList : MutableList<Version>?
    ) {
        // Setting the current version
        CachingHomeKYC.version = versionList!![0]
    }

    fun getMRZModel() :MRZModel? = profileInfoModel.mrzModel

    fun getProfileInfoModel() :ProfileInfoModel = profileInfoModel

    fun setRegisterConfigModel(
        registerConfig: RegisterConfig?
    ){
        if (registerConfig == null) {
            _logicEvent.postValue(HomeKYCLogicEvent.Finish.OnError(NullPointerException()))
            return
        }
        if (!registerConfig.birthDate.isNullOrEmpty() &&
                !registerConfig.expireDate.isNullOrEmpty() &&
                !registerConfig.documentNumber.isNullOrEmpty()) {
            profileInfoModel.mrzModel = MRZModel(
                registerConfig.birthDate,
                registerConfig.expireDate,
                registerConfig.documentNumber
            )
        }

        profileInfoModel.registerConfig = registerConfig
    }

    private fun listenAmaniEvents() {

        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack{

            override fun profileStatus(profileStatus: ProfileStatus) {
                Timber.d("Profile status received")
                if (profileStatus.message!!.status.equals(STATUS_APPROVED, ignoreCase = true)){
                    _logicEvent.postValue(HomeKYCLogicEvent.Finish.ProfileApproved)
                }
            }

            override fun stepsResult(stepsResult: StepsResult?) {
                Timber.d("Steps result is received")

                try {
                    repeat(stepsResult!!.result.size) {
                        Timber.d("Step result ID: ${stepsResult.result[it].sortOrder} , Status: ${stepsResult.result[it].status}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _logicEvent.postValue(HomeKYCLogicEvent.Refresh(StepsResultModelMapper.map(
                    stepsResult = stepsResult
                )))
            }
        })
    }

    init {
        listenAmaniEvents()
    }

    fun clearLogicState() {
        _logicEvent.postValue(HomeKYCLogicEvent.Empty)
    }
 }