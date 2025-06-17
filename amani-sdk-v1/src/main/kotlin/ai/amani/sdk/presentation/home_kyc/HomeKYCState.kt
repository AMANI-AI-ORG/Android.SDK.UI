package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.customer.Rule

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
sealed class HomeKYCState {

    object Loading : HomeKYCState()

    data class Loaded(var docList: ArrayList<Rule>): HomeKYCState()

    data class Error(var httpsErrorCode: Int = 0) : HomeKYCState()

}


