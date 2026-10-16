package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.utils.ProfileStatus
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.model.customer.CustomerDetailResult
import ai.amani.sdk.model.customer.Rule
import datamanager.model.config.ResGetConfig
import datamanager.model.config.Version

/**
 * @Author: zekiamani
 * @Date: 6.12.2022
 */
internal object CachingHomeKYC {

    // Private lock object for synchronizing access
    private val lock = Any()

    @Volatile private var _appConfig: ResGetConfig? = null
    @Volatile private var _customerDetail: CustomerDetailResult? = null
    @Volatile private var _version: Version? = null
    @Volatile private var _versionsList: MutableList<Version>? = null
    @Volatile private var _onlyKYCRules: ArrayList<Rule>? = null
    @Volatile private var _termsAcceptedOrDeclinedInSession: Boolean = false
    @Volatile private var _optionalStepsSkipped: Boolean = false

    // Thread-safe public accessors
    var appConfig: ResGetConfig?
        get() = synchronized(lock) { _appConfig }
        set(value) = synchronized(lock) { _appConfig = value }

    var customerDetail: CustomerDetailResult?
        get() = synchronized(lock) { _customerDetail }
        set(value) = synchronized(lock) { _customerDetail = value }

    var version: Version?
        get() = synchronized(lock) { _version }
        set(value) = synchronized(lock) { _version = value }

    var versionsList: MutableList<Version>?
        get() = synchronized(lock) { _versionsList }
        set(value) = synchronized(lock) { _versionsList = value }

    var onlyKYCRules: ArrayList<Rule>?
        get() = synchronized(lock) { _onlyKYCRules }
        set(value) = synchronized(lock) { _onlyKYCRules = value }

    var termsAcceptedOrDeclinedInSession: Boolean
        get() = synchronized(lock) { _termsAcceptedOrDeclinedInSession }
        set(value) = synchronized(lock) { _termsAcceptedOrDeclinedInSession = value }

    /**
     * The user ended the KYC with the skip button while optional steps remained. Kept for the
     * session so a return from an after-KYC screen finishes the KYC instead of showing Home again.
     */
    var optionalStepsSkipped: Boolean
        get() = synchronized(lock) { _optionalStepsSkipped }
        set(value) = synchronized(lock) { _optionalStepsSkipped = value }

    /** Freshest status of each step (socket / upload verdicts), on top of the cached rules. */
    private val _stepStatuses = HashMap<String, String>()

    /** Records a step's verdict; the v1 and v2 home screens both call it. */
    fun rememberStepStatus(stepId: String, status: String) = synchronized(lock) {
        _stepStatuses[stepId] = status
    }

    /**
     * What the SDK returns when the user closes it from the steps screen (back or toolbar back)
     * instead of finishing: APPROVED when every required KYC step is completed (approved or
     * pending review) and only optional steps are left, INCOMPLETE otherwise. Optional steps
     * are optional: leaving them undone does not make the KYC incomplete.
     */
    fun exitResult(): KYCResult = synchronized(lock) {
        val rules = _onlyKYCRules
        val requiredDone = !rules.isNullOrEmpty() &&
            OptionalSteps.requiredCompleted(rules) { _stepStatuses[it.id] ?: it.status }
        if (requiredDone) KYCResult(profileStatus = ProfileStatus.APPROVED) else KYCResult()
    }

    // Clears all cache safely
    fun clearCache() = synchronized(lock) {
        _appConfig = null
        _customerDetail = null
        _version = null
        _versionsList = null
        _onlyKYCRules = null
        _termsAcceptedOrDeclinedInSession = false
        _optionalStepsSkipped = false
        _stepStatuses.clear()
    }
}