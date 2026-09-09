package ai.amani.sdk.presentation.common.document_picker

import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import datamanager.model.config.Version

/**
 * Route a generic (physical contract) document takes, decided by its `documentSource` config.
 *
 * A camera document opens the classic capture screen; every other source skips that screen and
 * goes straight to the matching storage picker. Pure function so both the KYC home and the
 * document selection screens make the same decision.
 */
internal fun physicalContractRouteOf(version: Version?): ScreenRoutes {
    val source = DocumentSource.from(version?.documentSource)
    return if (source.isPicker) {
        ScreenRoutes.DocumentPickerScreen(source)
    } else {
        ScreenRoutes.PhysicalContractScreen
    }
}
