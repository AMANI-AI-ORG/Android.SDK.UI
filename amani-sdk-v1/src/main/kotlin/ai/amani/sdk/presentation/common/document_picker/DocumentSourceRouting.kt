package ai.amani.sdk.presentation.common.document_picker

import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import datamanager.model.config.Version

/**
 * Route a generic (physical contract) document takes, decided by its `documentSource` config.
 *
 * A camera document opens the classic capture screen, a gallery document an information screen
 * that opens the gallery itself, and a PDF document the documents picker straight away. Pure
 * function so both the KYC home and the document selection screens make the same decision.
 */
internal fun physicalContractRouteOf(version: Version?): ScreenRoutes =
    when (val source = DocumentSource.from(version?.documentSource)) {
        DocumentSource.Camera -> ScreenRoutes.PhysicalContractScreen
        DocumentSource.Gallery -> ScreenRoutes.DocumentInfoScreen
        DocumentSource.PdfFile -> ScreenRoutes.DocumentPickerScreen(source)
    }
