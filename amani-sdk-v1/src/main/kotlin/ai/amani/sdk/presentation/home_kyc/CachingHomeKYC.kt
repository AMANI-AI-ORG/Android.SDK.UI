package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.customer.CustomerDetailResult
import datamanager.model.config.ResGetConfig
import datamanager.model.config.Version

/**
 * @Author: zekiamani
 * @Date: 6.12.2022
 */
object CachingHomeKYC {

    var appConfig: ResGetConfig? = null

    var customerDetail: CustomerDetailResult? = null

    var version: Version? = null

    var versionsList : MutableList<Version>? = null
}