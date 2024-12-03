package ai.amani.sdk.presentation.home_kyc

import ai.amani.amani_sdk.R
import ai.amani.sdk.Amani
import ai.amani.sdk.data.mapper.StepsResultModelMapper
import ai.amani.sdk.data.repository.config.ConfigRepositoryImp
import ai.amani.sdk.data.repository.customer.CustomerDetailRepoImp
import ai.amani.sdk.data.repository.document.DocumentRepoImp
import ai.amani.sdk.data.repository.id_capture.IDCaptureRepoImp
import ai.amani.sdk.data.repository.login.LoginRepoImp
import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.data.repository.selfie_capture.SelfieCaptureRepoImp
import ai.amani.sdk.data.repository.signature.SignatureRepoImp
import ai.amani.sdk.extentions.getStepConfig
import ai.amani.sdk.extentions.sort
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.*
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.model.customer.CustomerDetailResult
import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation.common.BaseViewModel
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
import timber.log.Timber

/**
 * @Author: zekiamani
 * @Date: 5.09.2022
 */


open class HomeKYCViewModel(
    private val loginRepository: LoginRepoImp,
    private val configRepository: ConfigRepositoryImp,
    private val customerDetailRepoImp: CustomerDetailRepoImp,
    private val selfieCaptureRepoImp: SelfieCaptureRepoImp,
    private val idCaptureRepoImp: IDCaptureRepoImp,
    private val nfcRepository: NFCRepositoryImp,
    private val signatureRepository: SignatureRepoImp,
    private val documentRepository: DocumentRepoImp
) : BaseViewModel() {

    /*Responsible for UI State Management */
    private var _uiState: MutableLiveData<HomeKYCState> = MutableLiveData(HomeKYCState.Loading)
    val uiState: LiveData<HomeKYCState> = _uiState

    /*Responsible for Logic Events */
    private var _logicEvent: MutableLiveData<HomeKYCLogicEvent> =
        MutableLiveData(HomeKYCLogicEvent.Empty)
    val logicEvent: LiveData<HomeKYCLogicEvent> = _logicEvent

    private var selectedStepIDNumber = ""

    private var profileInfoModel : ProfileInfoModel = ProfileInfoModel()
    private var featureConfig : FeatureConfig = FeatureConfig()

    fun getAppConfig(): ResGetConfig? = CachingHomeKYC.appConfig

    fun getDocList(): ArrayList<Rule> {
        CachingHomeKYC.onlyKYCRules?.let {
            return it
        }
       val list = CachingHomeKYC.customerDetail!!.rules as ArrayList<Rule>?
       val sortedList =  list!!.sort()
        val result = CachingHomeKYC.appConfig?.stepConfigs?.filter { element1 ->
            sortedList.any {
                element2 -> element2.id == element1.id && element1.identifier == "kyc"
                    || element1.identifier == ""
            }
        }

      val kycRules =  list.filter { rule ->
            result!!.any{
                it.id == rule.id
            }
        }

        CachingHomeKYC.onlyKYCRules = ArrayList(kycRules)
        return CachingHomeKYC.onlyKYCRules!!
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
            token = getProfileInfoModel().registerConfig!!.token!!,
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
                        _logicEvent.postValue(HomeKYCLogicEvent.Finish.LoginFailed(error))
                    } ?: run {
                        Timber.e("Login is failed, HttpsErrorCode: null")

                        _uiState.value = HomeKYCState.Error()
                        _logicEvent.postValue(HomeKYCLogicEvent.Finish.LoginFailed(0))
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
                Timber.e("Fetch customer detail error: $it")
            },

            onComplete = {customerDetail ->
                CachingHomeKYC.customerDetail = customerDetail
                hasStepsBeforeOrAfterKYCFlow(
                    CachingHomeKYC.appConfig!!,
                    stepsBeforeKYC = { steps ->
                        //There is steps before KYC, checking the first step then navigate for that
                        navigateBeforeOrAfterKYCFlowScreens(steps)

                    },
                    stepsAfterKYC = { afterKycSteps ->
                        //If there is no steps before KYC continue with KYC process
                        if (checkKYCStepsAreApproved(customerDetail)) {
                            navigateBeforeOrAfterKYCFlowScreens(afterKycSteps)
                        } else _uiState.value = HomeKYCState.Loaded
                    },

                    no = {
                        if (checkKYCStepsAreApproved(customerDetail)){
                            _logicEvent.postValue(HomeKYCLogicEvent.Finish.ProfileApproved)
                        } else {
                            _uiState.value = HomeKYCState.Loaded
                        }
                    }
                )
            }
        )
    }

    /** Checks the fetched up to date customer data to process it. If customer status is already
     * APPROVED, postValue to [HomeKYCLogicEvent.Finish.ProfileApproved] as [_logicEvent]
     * @param customerDetail: Current customer data that fetched
     */
    private fun checkKYCStepsAreApproved(customerDetail: CustomerDetailResult?): Boolean {
        var size = 0
        var approvedStep = 0
        customerDetail?.rules?.forEach {
            if (it.identifier == "kyc" || it.identifier == "") {
                size += 1
                if (it.status == STATUS_APPROVED) approvedStep += 1
            }
        }

        return size == approvedStep
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

    fun navigateScreen(
        rule: Rule,
        adapterPosition: Int,
        route: (route: ScreenRoutes) -> Unit
    ) {
        selectedStepIDNumber = getDocList()[adapterPosition].id!!
        //Creating VersionList
        try {
            setVersionList(rule)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

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

                    AmaniDocumentTypes.PASSPORT, AmaniDocumentTypes.IDENTIFICATION,
                    AmaniDocumentTypes.DRIVING_LICENSE, AmaniDocumentTypes.VISA -> {
                        route.invoke(ScreenRoutes.IDFrontSideScreen)
                    }

                    AmaniDocumentTypes.NFC -> {
                        if (profileInfoModel.mrzModel != null) {
                            route.invoke(ScreenRoutes.NFCScanScreen)
                        } else {
                            // MRZ must be provided for every user from client side to start NFCOnly
                            // process properly.
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

                    AmaniDocumentTypes.PASSPORT, AmaniDocumentTypes.IDENTIFICATION,
                    AmaniDocumentTypes.DRIVING_LICENSE, AmaniDocumentTypes.VISA,
                    AmaniDocumentTypes.PHYSICAL_CONTRACT -> {
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

                    AmaniDocumentTypes.PHYSICAL_CONTRACT -> {
                        route.invoke(ScreenRoutes.PhysicalContractScreen)
                    }
                }
            }
        }
    }

    /** Sets Version List accordingly selected rule
     * @param rule : Selected rule from adapter
     **/
    @Throws
    private fun setVersionList(rule : Rule){
        CachingHomeKYC.versionsList = mutableListOf()
        val stepConfig: StepConfig = CachingHomeKYC.appConfig!!.getStepConfig(rule.sortOrder!!)
        val documentLists: List<DocumentList?> =
            stepConfig.mDocuments!!
        for (documentList in documentLists) {
            for (version in documentList!!.versions!!) {
                version.documentId = documentList.id.toString()
                version.stepId = rule.sortOrder!!
            }
            //Creating version list accordingly sort order from remote config
            //Sort order is a row number of current document
            CachingHomeKYC.versionsList!!.addAll(documentList.versions!!)
        }
    }

    /** Sets loader status for current step
     *  that show the button loader */
    private fun setLoaderStatus() {
        repeat(getDocList()!!.size) {
            if (getDocList()!![it]!!.id == selectedStepIDNumber) {
                CachingHomeKYC.onlyKYCRules!![it].isShowLoader = true
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

    fun featureConfigModel(): FeatureConfig = featureConfig

    fun setConfigModels(
        registerConfig: RegisterConfig?,
        featureConfig: FeatureConfig?
    ){
        if (registerConfig == null) {
            _logicEvent.postValue(HomeKYCLogicEvent.Finish.OnError(NullPointerException()))
            return
        }

        featureConfig?.let {
            this.featureConfig = it
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

    fun listenAmaniEvents() {

        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack{
            override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
                Timber.e("Amani SDK error type: $type Amani error: " +
                        "${error?.firstNotNullOf { it?.errorCode }}")
            }

            override fun profileStatus(profileStatus: ProfileStatus) {
                Timber.d("Profile status received")
            }

            override fun stepsResult(stepsResult: StepsResult?) {
                Timber.d("Steps result is received")

                try {
                    repeat(stepsResult!!.result.size) {
                        Timber.d("Step result ID: ${stepsResult.result[it].sortOrder} ," +
                                " Status: ${stepsResult.result[it].status}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val steps = stepsResult!!.result.filter { result ->
                    getDocList().any { rule ->
                        rule.id == result.id
                    }
                }

                //Refreshing the UI with fresh steps
                _logicEvent.postValue(HomeKYCLogicEvent.Refresh(StepsResultModelMapper.map(
                    stepsResult = StepsResult(ArrayList(steps))
                )))

                //Copy original list to make mutable list
                val kycArrayList = getDocList().map { it.status }
                    .toMutableList()

                //Update mutable list rule status accordingly recent socket result
                getDocList().forEachIndexed{ index ,rule ->
                    steps.forEach {
                        if (rule.id == it.id) {
                            kycArrayList[index] = it.status
                        }
                    }
                }

                //Checking the KYC steps approved ot not
                var approvedSteps = 0
                kycArrayList.forEach {
                    // All KYC steps should be approved
                    if(it == STATUS_APPROVED) approvedSteps += 1
                }

                if (approvedSteps >= kycArrayList.size) {
                    //Check after kyc steps is existing to navigate for those steps
                    hasOnlyStepsAfterKYCFlow(
                        appConfig = CachingHomeKYC.appConfig!!,
                        steps = {
                            navigateBeforeOrAfterKYCFlowScreens(steps = it)
                        },
                        no = {
                            _logicEvent.postValue(HomeKYCLogicEvent.Finish.ProfileApproved)
                        }
                    )
                }
            }
        })
    }

    init {
        listenAmaniEvents()
    }

    private fun hasOnlyStepsAfterKYCFlow(
        appConfig: ResGetConfig,
        steps: (steps: ArrayList<StepConfig>) -> Unit = {},
        no: () -> Unit
    ) {
        hasStepsBeforeOrAfterKYCFlow(
            appConfig = appConfig,
            stepsBeforeKYC = {
                //We are only taking care of steps before at KYC in that time
                no.invoke()
            },
            stepsAfterKYC = {
                steps.invoke(it)
            },
            no = {
                no.invoke()
            }
        )
    }

    private fun hasStepsBeforeOrAfterKYCFlow(
        appConfig: ResGetConfig,
        stepsBeforeKYC: (steps: ArrayList<StepConfig>) -> Unit = {},
        stepsAfterKYC: (steps: ArrayList<StepConfig>) -> Unit = {},
        no: () -> Unit
    ) {
        val identifierScreenListBeforeKYCFlow = arrayListOf<StepConfig>()
        val identifierScreenListAfterKYCFlow = arrayListOf<StepConfig>()

        //All steps in mutable array list to mutate it
        var steps = ArrayList(appConfig.stepConfigs)

        steps = ArrayList(steps.filter { step->
            CachingHomeKYC.customerDetail!!.rules!!.any { rule ->
                step.id == rule.id
            }
        })

        //The first index of where the KYC steps begins
        var firstIndexOfKYC = 9999
        steps.forEachIndexed { index, step ->
            //Identifier is kyc or empty its the first index of KYC
            if (step.identifier == "" || step.identifier == "kyc") {
                if (firstIndexOfKYC == 9999) firstIndexOfKYC = index
            }
        }

        // Remove elements with value equal to kyc or its empty
        val iterator = steps.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element.identifier == "" || element.identifier == "kyc") {
                iterator.remove()
            }
        }

        val identifierRuleList = CachingHomeKYC.customerDetail!!.rules?.filter { customerDetailRule ->
            steps.any { stepConfigRule ->
                customerDetailRule.id == stepConfigRule.id
            }
        }

        steps.forEachIndexed{ index, step ->
            AppConstant.STEPS_BEFORE_KYC_FLOW.forEach {
                if (step.identifier == it) {
                   if (identifierRuleList!![index].status != STATUS_APPROVED) {
                      if (index < firstIndexOfKYC){
                          identifierScreenListBeforeKYCFlow.add(step)
                      } else{
                          identifierScreenListAfterKYCFlow.add(step)
                      }
                   }
                }
            }
        }

        //Invoking steps before KYC flow if its not empty
        if (identifierScreenListBeforeKYCFlow.isNotEmpty()) {
            stepsBeforeKYC.invoke(identifierScreenListBeforeKYCFlow)
        }

        //Invoking steps after KYC flow if its not empty
        if (identifierScreenListAfterKYCFlow.isNotEmpty()) {
            stepsAfterKYC.invoke(identifierScreenListAfterKYCFlow)
        }

        //If both list is empty there is not extra/additional steps other then KYC flow
        if (identifierScreenListBeforeKYCFlow.isEmpty()
            && identifierScreenListAfterKYCFlow.isEmpty()){
            no.invoke()
        }
    }

    private fun navigateBeforeOrAfterKYCFlowScreens(steps: ArrayList<StepConfig>) {
        when(steps.first().identifier) {
            AppConstant.IDENTIFIER_PROFILE_INFO -> {
                navigateTo(HomeKYCFragmentDirections.actionHomeKYCFragmentToProfileInfoFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = CachingHomeKYC.appConfig!!.generalConfigs!!
                    )
                ))
            }

            AppConstant.IDENTIFIER_PHONE_OTP -> {
                navigateTo(HomeKYCFragmentDirections.actionHomeKYCFragmentToVerifyPhoneFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = CachingHomeKYC.appConfig!!.generalConfigs!!
                    )
                ))
            }

            AppConstant.IDENTIFIER_EMAIL_OTP -> {
                navigateTo(HomeKYCFragmentDirections.actionHomeKYCFragmentToVerifyEmailFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = CachingHomeKYC.appConfig!!.generalConfigs!!
                    )
                ))
            }

            AppConstant.IDENTIFIER_QUESTIONNAIRE -> {
                navigateTo(HomeKYCFragmentDirections.actionHomeKYCFragmentToQuestionnaireFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = CachingHomeKYC.appConfig!!.generalConfigs!!
                    )
                ))
            }
        }
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
}