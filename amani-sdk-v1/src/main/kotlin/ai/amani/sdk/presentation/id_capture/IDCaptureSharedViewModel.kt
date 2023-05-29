package ai.amani.sdk.presentation.id_capture

import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import androidx.lifecycle.ViewModel
import datamanager.model.config.Version

/**
 * @Author: zekiamani
 * @Date: 13.09.2022
 */
class IDCaptureSharedViewModel: ViewModel() {

    fun navigateScreen(
        version: Version,
        route: (route: ScreenRoutes) -> Unit
    ) {
        version.steps.apply {
            if (!this.isNullOrEmpty()) {
                if (this.size > 1) {
                    //Navigate for back side
                    route.invoke(ScreenRoutes.PreviewScreen)
                } else {
                    //Navigate for KYCHomeScreen
                    route.invoke(ScreenRoutes.HomeKYCScreen)
                }
            }
        }
    }
}