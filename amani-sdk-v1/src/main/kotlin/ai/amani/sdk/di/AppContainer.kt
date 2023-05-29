package ai.amani.sdk.di

import ai.amani.sdk.data.repository.config.ConfigRepositoryImp
import ai.amani.sdk.data.repository.customer.CustomerDetailRepoImp
import ai.amani.sdk.data.repository.id_capture.IDCaptureRepoImp
import ai.amani.sdk.data.repository.login.LoginRepoImp
import ai.amani.sdk.data.repository.selfie_capture.SelfieCaptureRepoImp

/**
 * @Author: zekiamani
 * @Date: 8.09.2022
 */

class AppContainer {

    val loginRepository = LoginRepoImp()
    val configRepository = ConfigRepositoryImp()
    val customerDetailRepository = CustomerDetailRepoImp()
    val idCaptureRepository = IDCaptureRepoImp()
    val selfieCaptureRepository = SelfieCaptureRepoImp()

}