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

        /**
         * The user ended the KYC with the skip-optional-steps button. Shown like [ProfileApproved],
         * but the steps screen stays below it so back returns there instead of closing the SDK.
         */
        object ProfileApprovedBySkip: HomeKYCLogicEvent()

        class OnError(val errorCode: Int = 0)  : HomeKYCLogicEvent()

        class OnException(val exception: Throwable): HomeKYCLogicEvent()
    }
}

