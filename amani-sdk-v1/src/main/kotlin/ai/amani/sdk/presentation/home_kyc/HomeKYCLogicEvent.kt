package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.customer.Rule

/**
 * @Author: zekiamani
 * @Date: 3.12.2022
 */
sealed class HomeKYCLogicEvent {

    object Empty: HomeKYCLogicEvent()

    data class Refresh(val documentList: List<Rule>?) : HomeKYCLogicEvent()

    sealed class Finish {

        object ProfileApproved: HomeKYCLogicEvent()

        class OnError(val errorCode: Int = 0)  : HomeKYCLogicEvent()

        class OnException(val exception: Throwable): HomeKYCLogicEvent()
    }
}

