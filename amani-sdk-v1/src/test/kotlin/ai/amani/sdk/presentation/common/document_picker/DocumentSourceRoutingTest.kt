package ai.amani.sdk.presentation.common.document_picker

import ai.amani.sdk.model.DocumentSource
import ai.amani.sdk.presentation.home_kyc.ScreenRoutes
import ai.amani.sdk.presentation_v2.navigation.AmaniV2Destination
import ai.amani.sdk.presentation_v2.navigation.AmaniV2Navigator
import ai.amani.sdk.presentation_v2.navigation.CaptureFlow
import datamanager.model.config.Version
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** How the `documentSource` config, and the user's choice for an optional one, is routed. */
@RunWith(JUnit4::class)
class DocumentSourceRoutingTest {

    private fun version(source: String) = Version().apply {
        documentSource = source
        type = "TUR_IB_0"
    }

    @Test
    fun `config values map to their source, unknown ones to the camera`() {
        assertEquals(DocumentSource.Optional, DocumentSource.from(" Optional "))
        assertEquals(DocumentSource.PdfFile, DocumentSource.from("pdfFile"))
        assertEquals(DocumentSource.Gallery, DocumentSource.from("gallery"))
        assertEquals(DocumentSource.Camera, DocumentSource.from("camera"))
        assertEquals(DocumentSource.Camera, DocumentSource.from(null))
        assertEquals(DocumentSource.Camera, DocumentSource.from("unknown"))
    }

    @Test
    fun `optional is no picker`() {
        assertFalse(DocumentSource.Optional.isPicker)
    }

    @Test
    fun `v1 optional document opens the source dialog`() {
        assertEquals(ScreenRoutes.DocumentSourceDialog, physicalContractRouteOf(version("optional")))
    }

    @Test
    fun `v1 chosen source opens a picker or the capture screen`() {
        assertEquals(
            ScreenRoutes.DocumentPickerScreen(DocumentSource.PdfFile),
            chosenSourceRouteOf(DocumentSource.PdfFile)
        )
        assertEquals(
            ScreenRoutes.DocumentPickerScreen(DocumentSource.Gallery),
            chosenSourceRouteOf(DocumentSource.Gallery)
        )
        assertEquals(ScreenRoutes.PhysicalContractScreen, chosenSourceRouteOf(DocumentSource.Camera))
        assertNull(chosenSourceRouteOf(DocumentSource.Optional))
    }

    @Test
    fun `v2 optional document opens the source choice`() {
        assertEquals(
            AmaniV2Destination.DocumentSourceChoice("TUR_IB_0"),
            CaptureFlow.directDestinationFor(version("optional").apply { documentId = "IB" })
        )
    }

    @Test
    fun `v2 chosen source opens a picker or the capture screen`() {
        assertEquals(
            AmaniV2Destination.DocumentPick("TUR_IB_0", DocumentSource.PdfFile),
            CaptureFlow.chosenSourceDestination("TUR_IB_0", DocumentSource.PdfFile)
        )
        assertEquals(
            AmaniV2Destination.DocumentPick("TUR_IB_0", DocumentSource.Gallery),
            CaptureFlow.chosenSourceDestination("TUR_IB_0", DocumentSource.Gallery)
        )
        assertEquals(
            AmaniV2Destination.AddressVerify("TUR_IB_0"),
            CaptureFlow.chosenSourceDestination("TUR_IB_0", DocumentSource.Camera)
        )
        assertNull(CaptureFlow.chosenSourceDestination("TUR_IB_0", DocumentSource.Optional))
    }

    @Test
    fun `v2 navigator holds the source choice as a dialog instead of pushing it`() {
        val navigator = AmaniV2Navigator(listOf(AmaniV2Destination.HomeKYC))
        val choice = AmaniV2Destination.DocumentSourceChoice("TUR_IB_0")

        navigator.navigateTo(choice)
        assertEquals(AmaniV2Destination.HomeKYC, navigator.current)
        assertEquals(1, navigator.depth)
        assertEquals(choice, navigator.sourceChoice)

        navigator.dismissSourceChoice()
        assertNull(navigator.sourceChoice)
    }
}
