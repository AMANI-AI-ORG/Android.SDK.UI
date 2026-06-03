package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.data.repository.config.ConfigRepositoryImp
import ai.amani.sdk.data.repository.customer.CustomerDetailRepoImp
import ai.amani.sdk.data.repository.login.LoginRepoImp
import ai.amani.sdk.extentions.sort
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.model.customer.CustomerDetailResult
import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.utils.AmaniUIErrorConstants
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
    private val customerDetailRepository: CustomerDetailRepoImp
) : ViewModel() {

    private val _state = MutableStateFlow<HomeKYCState>(HomeKYCState.Loading)
    val state: StateFlow<HomeKYCState> = _state.asStateFlow()

    private val _effects = Channel<HomeKYCEffect>(Channel.BUFFERED)
    val effects: Flow<HomeKYCEffect> = _effects.receiveAsFlow()

    private var registerConfig: RegisterConfig? = null
    private var featureConfig: FeatureConfig = FeatureConfig()
    private var started = false

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

        _state.value = HomeKYCState.Ready(
            palette = HomeKYCMapper.resolvePalette(CachingHomeKYC.appConfig),
            content = HomeKYCMapper.toUiState(docList, CachingHomeKYC.appConfig)
        )
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
                    CustomerDetailRepoImp()
                ) as T
        }
    }
}
