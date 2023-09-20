package ai.amani.sdk.presentation.home_kyc

/**
 * @Author: zekiamani
 * @Date: 6.09.2022
 */
sealed class HomeKYCState {

    object Loading : HomeKYCState()

    object Loaded: HomeKYCState()

    data class Error(var httpsErrorCode: Int = 0) : HomeKYCState()

}


