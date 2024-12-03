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

    var appConfig: ResGetConfig? = null

    var customerDetail: CustomerDetailResult? = null

    var version: Version? = null

    var versionsList : MutableList<Version>? = null

    var onlyKYCRules: ArrayList<Rule>? = null

    fun clearCache() {
        appConfig = null
        customerDetail = null
        version = null
        versionsList = null
        onlyKYCRules = null
    }
}