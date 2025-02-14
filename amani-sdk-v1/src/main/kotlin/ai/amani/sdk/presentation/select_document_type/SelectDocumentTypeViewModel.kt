package ai.amani.sdk.presentation.select_document_type

import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.utils.AmaniDocumentTypes
import androidx.lifecycle.ViewModel
import datamanager.model.config.Version

/**
 * @Author: zekiamani
 * @Date: 13.09.2022
 */

class SelectDocumentTypeViewModel: ViewModel() {

    fun navigateScreen(
        version: Version,
        route: (route: ScreenRoutes) -> Unit
    ) {
        when(version.documentId) {
            AmaniDocumentTypes.IDENTIFICATION ,
            AmaniDocumentTypes.PASSPORT,
            AmaniDocumentTypes.DRIVING_LICENSE-> {
                route.invoke(ScreenRoutes.IDFrontSideScreen)
            }

            else -> {
                route.invoke(ScreenRoutes.PhysicalContractScreen)
            }
        }
    }
}