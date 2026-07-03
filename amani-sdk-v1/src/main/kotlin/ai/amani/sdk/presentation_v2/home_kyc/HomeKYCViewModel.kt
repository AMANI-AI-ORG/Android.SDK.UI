package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.Amani
import ai.amani.sdk.data.repository.config.ConfigRepositoryImp
import ai.amani.sdk.data.repository.customer.CustomerDetailRepoImp
import ai.amani.sdk.data.repository.id_capture.IDCaptureRepoImp
import ai.amani.sdk.data.repository.login.LoginRepoImp
import ai.amani.sdk.data.repository.document.DocumentRepoImp
import ai.amani.sdk.data.repository.selfie_capture.SelfieCaptureRepoImp
import ai.amani.sdk.data.repository.signature.SignatureRepoImp
import ai.amani.sdk.presentation.physical_contract_screen.GenericDocumentFlow
import ai.amani.sdk.presentation.selfie.SelfieType
import ai.amani.sdk.presentation_v2.selfie_capture.SelfieTypeResolver
import ai.amani.sdk.utils.AmaniDocumentTypes
import ai.amani.sdk.extentions.sort
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import ai.amani.sdk.model.customer.CustomerDetailResult
import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.utils.AmaniUIErrorConstants
import ai.amani.sdk.utils.AppConstant
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import datamanager.model.config.Version
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Compose-friendly view model for the V2 HomeKYC overview.
 *
 * Why a *new* view model rather than reusing v1's [ai.amani.sdk.presentation.home_kyc.HomeKYCViewModel]:
 * the v1 one extends `BaseViewModel` (NavDirections / Fragment-NavController coupling)
 * and emits `LiveData`, neither of which fits a unidirectional Compose flow. So this is
 * the only piece we re-author — everything underneath is *reused*: the same
 * [LoginRepoImp] → [ConfigRepositoryImp] → [CustomerDetailRepoImp] orchestration, the
 * shared [CachingHomeKYC] cache, the [sort] extension, and the doc-list filtering logic
 * mirrored from v1. The result is exposed as a single [StateFlow] of [HomeKYCState]
 * plus a one-shot [HomeKYCEffect] channel.
 *
 * Login needs an [Activity] (the core SDK's `initAmani`), so [start] takes one; the
 * reference is used only for the duration of the call and never retained on the field.
 */
class HomeKYCViewModel(
    private val loginRepository: LoginRepoImp,
    private val configRepository: ConfigRepositoryImp,
    private val customerDetailRepository: CustomerDetailRepoImp,
    private val idCaptureRepository: IDCaptureRepoImp,
    private val selfieCaptureRepository: SelfieCaptureRepoImp,
    private val signatureRepository: SignatureRepoImp,
    private val documentRepository: DocumentRepoImp
) : ViewModel() {

    private val _state = MutableStateFlow<HomeKYCState>(HomeKYCState.Loading)
    val state: StateFlow<HomeKYCState> = _state.asStateFlow()

    private val _effects = Channel<HomeKYCEffect>(Channel.BUFFERED)
    val effects: Flow<HomeKYCEffect> = _effects.receiveAsFlow()

    private var registerConfig: RegisterConfig? = null
    private var featureConfig: FeatureConfig = FeatureConfig()
    private var started = false

    // ── Live overlays on the immutable cached rules ─────────────────────────────────
    // The cached [Rule] objects have no status setter, so updates from the upload flow
    // (the AmaniEvent socket, a failed upload) are layered on as overlays and applied by
    // [HomeKYCMapper.toUiState] instead of mutating the rules in place.
    private val statusOverrides = mutableMapOf<String, String>()
    private val errorOverrides = mutableMapOf<String, String>()
    /** Rule id currently uploading / awaiting the verdict — renders the row spinner. */
    private var processingRuleId: String? = null

    private val REJECTED_STATUSES = setOf(
        AppConstant.STATUS_REJECTED,
        AppConstant.STATUS_AUTOMATICALLY_REJECTED
    )

    /**
     * Statuses whose server message is surfaced inline under the step — rejections plus
     * PENDING_REVIEW (manual-review note), matching v1's KYCAdapter and
     * [HomeKYCMapper.ERROR_BEARING_STATUSES].
     */
    private val ERROR_BEARING_STATUSES = setOf(
        AppConstant.STATUS_REJECTED,
        AppConstant.STATUS_AUTOMATICALLY_REJECTED,
        AppConstant.STATUS_PENDING_REVIEW
    )

    init {
        listenAmaniEvents()
    }

    /**
     * Kicks off the load: login → app config → customer detail → map to [HomeKYCState.Ready].
     * Idempotent — safe to call again across recompositions/config changes; the second
     * call is ignored once a load is in flight or done.
     */
    fun start(activity: Activity, registerConfig: RegisterConfig?, featureConfig: FeatureConfig?) {
        if (started) return
        started = true

        featureConfig?.let { this.featureConfig = it }
        this.registerConfig = registerConfig

        if (registerConfig?.token.isNullOrEmpty()) {
            emitError(AmaniUIErrorConstants.REGISTER_CONFIG_NULL)
            return
        }

        login(activity, registerConfig!!)
    }

    /** Re-runs the load after a transient failure (host "try again"). */
    fun retry(activity: Activity) {
        started = false
        _state.value = HomeKYCState.Loading
        start(activity, registerConfig, featureConfig)
    }

    /** A step row / primary button was tapped — defer the concrete routing to wiring. */
    fun onStepSelected(rule: Rule) {
        sendEffect(HomeKYCEffect.StartStep(rule))
    }

    /**
     * The step the home primary button should start right now, or `null` when nothing is
     * actionable (the active step is uploading / awaiting its verdict and the next is still
     * locked behind it). Resolved with the live overlays so navigation matches exactly the
     * Active row the user sees: it never re-opens a just-approved step and never jumps a
     * step still locked while another processes. The capture entry (see the nav host) uses
     * this instead of the cache's stale `rule.status`.
     */
    fun resolveActiveRule(): Rule? {
        val rules = CachingHomeKYC.onlyKYCRules ?: return null
        return HomeKYCMapper.resolveActiveRule(
            rules = rules,
            config = CachingHomeKYC.appConfig,
            statusOverrides = statusOverrides,
            processingRuleId = processingRuleId
        )
    }

    /**
     * Finishes the capture leg for [version] after the user confirmed the last side.
     * Mirrors v1's `PreviewScreenViewModel.navigateScreen` → `HomeKYCViewModel.uploadID`
     * hand-off: when NFC is *not* active the captured document is uploaded straight away
     * and the matching home step shows a loading spinner; the verdict then arrives over
     * the AmaniEvent socket (see [listenAmaniEvents]) — APPROVED unlocks the next step,
     * a rejection shows the server error inline and keeps the next step locked.
     *
     * The host calls this and then pops back to Home, so the user watches the step
     * upload from the overview screen.
     *
     * @param activity the host activity (the core upload + NFC check both need it)
     * @param version  the just-confirmed document version
     */
    fun uploadStep(activity: FragmentActivity, version: Version) {
        val docType = version.type ?: run {
            Timber.e("V2 upload: version.type is null, cannot upload")
            return
        }

        // NFC-enabled IDs never reach here: the nav host routes them to the NFC screen first,
        // and the outcome comes back through [finishNfcLeg]. So this path is the plain
        // (non-NFC) upload — mirror v1's `IDCapture().withNFC(false)` before an ID-only upload.
        val ruleId = ruleIdFor(version)
        // Clear any stale rejection on this step and show the spinner before uploading.
        ruleId?.let { errorOverrides.remove(it) }
        processingRuleId = ruleId
        refreshReady()

        val onComplete: (ai.amani.sdk.model.UploadResultModel) -> Unit = { result ->
            handleUploadResult(result, docType)
        }

        when (version.documentId) {
            // Selfies upload through the selfie SDK path (not IDCapture). The variant is
            // resolved the same way the capture screen mounted it, so the upload always
            // matches what was captured. Mirrors v1 HomeKYCViewModel.uploadSelfie dispatch.
            AmaniDocumentTypes.SELFIE -> when (SelfieTypeResolver.resolve(version)) {
                SelfieType.Auto -> selfieCaptureRepository.uploadAutoSelfie(activity, docType, {}, onComplete)
                SelfieType.Manual -> selfieCaptureRepository.uploadManualSelfie(activity, docType, {}, onComplete)
                // Pose estimation (V1 and V2) share the same upload endpoint.
                else -> selfieCaptureRepository.uploadSelfiePoseEstimation(activity, docType, {}, onComplete)
            }

            // Sign contract: the signatures the pad collected upload through the shared
            // signature repository. Mirrors v1 HomeKYCViewModel.uploadSignature dispatch.
            AmaniDocumentTypes.SIGNATURE -> signatureRepository.uploadSignature(onComplete)

            else -> {
                Amani.sharedInstance().IDCapture().withNFC(false)
                idCaptureRepository.upload(activity, docType, {}, onComplete)
            }
        }
    }

    /**
     * Finishes the verify-address leg for [version]. Mirrors v1 HomeKYCFragment's
     * PHYSICAL_CONTRACT dispatch → `uploadDocument`: the photographed document
     * ([GenericDocumentFlow.DataFromCamera]) or the picked PDF
     * ([GenericDocumentFlow.DataFromGallery]) uploads through the shared
     * [DocumentRepoImp]; the step shows its spinner and the verdict arrives over the
     * AmaniEvent socket (see [listenAmaniEvents]).
     */
    fun uploadAddressStep(
        activity: FragmentActivity,
        version: Version,
        flow: GenericDocumentFlow
    ) {
        val docType = version.type ?: run {
            Timber.e("V2 address upload: version.type is null, cannot upload")
            return
        }
        val ruleId = ruleIdFor(version)
        ruleId?.let { errorOverrides.remove(it) }
        processingRuleId = ruleId
        refreshReady()

        documentRepository.upload(
            activity = activity,
            docType = docType,
            onStart = {},
            onComplete = { result -> handleUploadResult(result, docType) },
            genericDocumentFlow = flow
        )
    }

    /**
     * Finishes the NFC leg for [version] (from the V2 NFC screen). Mirrors v1
     * HomeKYCFragment's post-NFC dispatch: [success] true → set `IDCapture().withNFC(true)`
     * and upload so the ID *and* NFC data go up together; false (out of attempts) →
     * `withNFC(false)` and upload the ID only. Either way the step shows its spinner and the
     * verdict then arrives over the AmaniEvent socket (see [listenAmaniEvents]).
     */
    fun finishNfcLeg(activity: FragmentActivity, version: Version, success: Boolean) {
        val docType = version.type ?: run {
            Timber.e("V2 NFC finish: version.type is null, cannot upload")
            return
        }
        val ruleId = ruleIdFor(version)
        ruleId?.let { errorOverrides.remove(it) }
        processingRuleId = ruleId
        refreshReady()

        Amani.sharedInstance().IDCapture().withNFC(success)
        idCaptureRepository.upload(activity, docType, {}) { result ->
            handleUploadResult(result, docType)
        }
    }

    /**
     * Upload completion callback for every step kind. We deliberately do **not** act on
     * [ai.amani.sdk.model.UploadResultModel.isSuccess]: a successful upload only means the
     * document reached the server, not that it was approved — and approval (not delivery)
     * is what the step's state must reflect. So the step keeps its processing loader after
     * upload regardless of this result; only the AmaniEvent socket verdict (see
     * [listenAmaniEvents]) clears [processingRuleId] and swaps the spinner for the real
     * status (Done / Rejected / Pending review). An APPROVED verdict then unlocks the next
     * step. A failure here is logged only; the loader stays until that socket status comes.
     */
    private fun handleUploadResult(
        result: ai.amani.sdk.model.UploadResultModel,
        docType: String
    ) {
        if (result.isSuccess) {
            Timber.i("V2 upload accepted for docType=$docType, awaiting AmaniEvent verdict")
        } else {
            Timber.e(
                "V2 upload reported failure docType=$docType code=${result.onError} " +
                    "err=${result.throwable}; keeping loader until AmaniEvent verdict"
            )
        }
    }

    private fun login(activity: Activity, config: RegisterConfig) {
        loginRepository.login(
            activity = activity,
            tcNumber = config.tcNumber,
            token = config.token!!,
            lang = config.language,
            location = config.location,
            onStart = { _state.value = HomeKYCState.Loading },
            onCompleted = { result ->
                if (result.isSuccess) {
                    fetchAppConfig()
                } else {
                    val code = result.error ?: 0
                    Timber.e("V2 HomeKYC login failed, code: $code")
                    emitError(code)
                }
            }
        )
    }

    private fun fetchAppConfig() {
        configRepository.getAppConfig(
            onStart = { Timber.d("V2 HomeKYC: fetching app config") },
            onError = { code ->
                Timber.e("V2 HomeKYC: app config error $code")
                emitError(AmaniUIErrorConstants.REMOTE_CONFIG_FETCH_ERROR)
            },
            onComplete = { config ->
                CachingHomeKYC.appConfig = config
                fetchCustomerDetail()
            }
        )
    }

    private fun fetchCustomerDetail() {
        customerDetailRepository.getCustomerDetail(
            onStart = { Timber.d("V2 HomeKYC: fetching customer detail") },
            onError = { throwable ->
                Timber.e("V2 HomeKYC: customer detail error $throwable")
                emitError(AmaniUIErrorConstants.CUSTOMER_DETAIL_FETCH_ERROR)
            },
            onComplete = { customerDetail ->
                CachingHomeKYC.customerDetail = customerDetail
                reduce(customerDetail)
            }
        )
    }

    /**
     * Turns the freshly fetched data into the final state. If every KYC step is already
     * approved the flow is complete; otherwise we map the doc list into the overview.
     *
     * NOTE(wiring): v1 also branches into before/after-KYC identifier flows
     * (profile_info / phone_otp / email_otp / questionnaire) here. Those screens don't
     * exist in V2 yet, so that routing is deferred to the wiring phase.
     */
    private fun reduce(customerDetail: CustomerDetailResult?) {
        if (areAllKycStepsApproved(customerDetail)) {
            sendEffect(HomeKYCEffect.ProfileApproved)
            return
        }

        val docList = buildDocList()
        if (docList.isNullOrEmpty()) {
            emitError(AmaniUIErrorConstants.CORRUPTED_DOC_LIST)
            return
        }

        refreshReady()
    }

    /**
     * Re-emits [HomeKYCState.Ready] from the cached doc list with the current overlays
     * applied. Called on the initial load and on every live update (upload start /
     * failure, AmaniEvent socket result).
     */
    private fun refreshReady() {
        val docList = CachingHomeKYC.onlyKYCRules ?: return
        _state.value = HomeKYCState.Ready(
            palette = HomeKYCMapper.resolvePalette(CachingHomeKYC.appConfig),
            content = HomeKYCMapper.toUiState(
                rules = docList,
                config = CachingHomeKYC.appConfig,
                statusOverrides = statusOverrides,
                errorOverrides = errorOverrides,
                processingRuleId = processingRuleId
            )
        )
    }

    /**
     * Registers the AmaniEvent listener that drives the home overview after an upload.
     * Mirrors v1 `HomeKYCViewModel.listenAmaniEvents`/`stepsResult`: each socket result
     * for a KYC step updates that step's status overlay (and rejection message), stops
     * its spinner, and refreshes the UI. APPROVED moves the row to Done and unlocks the
     * next step (the mapper recomputes the active step); a rejection shows the inline
     * error and leaves later steps locked. When every KYC step is approved the flow
     * completes via [HomeKYCEffect.ProfileApproved].
     */
    private fun listenAmaniEvents() {
        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack {
            override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
                Timber.e("V2 AmaniEvent error type=$type")
            }

            override fun profileStatus(profileStatus: ProfileStatus) {
                Timber.d("V2 AmaniEvent profile status received")
            }

            override fun stepsResult(stepsResult: StepsResult?) {
                val results = stepsResult?.result ?: return

                // Store the freshest status + rejection message per step as overlays — even
                // if this socket push arrives before the doc list is built on first open.
                // The old HomeKYCViewModel likewise keeps the latest socket result; if we
                // bailed out when the list wasn't ready yet (the previous behaviour) the very
                // first push — which carries the current approved/rejected/not-uploaded state
                // on a fresh open — was dropped and the step states/errors never showed.
                var changed = false
                results.forEach { res ->
                    val id = res.id ?: return@forEach
                    res.status?.let { status ->
                        statusOverrides[id] = status
                        changed = true
                        when (status) {
                            // Rejections AND pending-review verdicts carry a server message
                            // to surface inline — same statuses v1's KYCAdapter shows it for.
                            in ERROR_BEARING_STATUSES -> {
                                res.errors?.firstOrNull()?.errorMessage?.toString()
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { errorOverrides[id] = it }
                            }
                            STATUS_APPROVED -> errorOverrides.remove(id)
                        }
                    }
                    // A verdict for the uploading step stops its spinner.
                    if (id == processingRuleId) processingRuleId = null
                }
                if (!changed) return

                // Re-render only once the overview's doc list exists; until then the stored
                // overrides simply wait for the initial reduce() → refreshReady() to apply them.
                val docList = CachingHomeKYC.onlyKYCRules ?: return
                refreshReady()

                val allApproved = docList.all {
                    (statusOverrides[it.id] ?: it.status) == STATUS_APPROVED
                }
                if (allApproved) sendEffect(HomeKYCEffect.ProfileApproved)
            }
        })
    }

    /** Resolves the KYC rule id matching [version] (same step sort order). */
    private fun ruleIdFor(version: Version): String? {
        val docList = CachingHomeKYC.onlyKYCRules
        return docList?.firstOrNull { it.sortOrder == version.stepId }?.id
            ?: docList?.firstOrNull { (statusOverrides[it.id] ?: it.status) != STATUS_APPROVED }?.id
    }

    /**
     * Filters the customer rules down to the KYC-flow rules, caching the result in the
     * shared [CachingHomeKYC.onlyKYCRules]. Mirrors v1 `HomeKYCViewModel.getDocList()`.
     */
    private fun buildDocList(): List<Rule>? {
        CachingHomeKYC.onlyKYCRules?.let { return it }
        return try {
            val rules = (CachingHomeKYC.customerDetail?.rules as? ArrayList<Rule>) ?: return null
            val sorted = rules.sort()
            val kycStepConfigs = CachingHomeKYC.appConfig?.stepConfigs?.filter { step ->
                sorted.any { rule -> rule.id == step.id && step.identifier == "kyc" } ||
                    step.identifier == ""
            }.orEmpty()
            val kycRules = rules.filter { rule -> kycStepConfigs.any { it.id == rule.id } }
            ArrayList(kycRules).also { CachingHomeKYC.onlyKYCRules = it }
        } catch (e: Exception) {
            Timber.e(e, "V2 HomeKYC: failed to build doc list")
            null
        }
    }

    /** True when every KYC-identifier rule is APPROVED. Mirrors v1 `checkKYCStepsAreApproved`. */
    private fun areAllKycStepsApproved(customerDetail: CustomerDetailResult?): Boolean {
        var total = 0
        var approved = 0
        customerDetail?.rules?.forEach { rule ->
            if (rule.identifier == "kyc" || rule.identifier == "") {
                total += 1
                if (rule.status == STATUS_APPROVED) approved += 1
            }
        }
        return total > 0 && total == approved
    }

    private fun emitError(errorCode: Int) {
        _state.value = HomeKYCState.Failed(errorCode)
        sendEffect(HomeKYCEffect.Error(errorCode))
    }

    private fun sendEffect(effect: HomeKYCEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                HomeKYCViewModel(
                    LoginRepoImp(),
                    ConfigRepositoryImp(),
                    CustomerDetailRepoImp(),
                    IDCaptureRepoImp(),
                    SelfieCaptureRepoImp(),
                    SignatureRepoImp(),
                    DocumentRepoImp()
                ) as T
        }
    }
}
