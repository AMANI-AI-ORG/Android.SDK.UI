package ai.amani.sdk.presentation.home_kyc

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

    // Clears all cache safely
    fun clearCache() = synchronized(lock) {
        _appConfig = null
        _customerDetail = null
        _version = null
        _versionsList = null
        _onlyKYCRules = null
    }
}
