package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCPreviewData.RULE_ID_DOCUMENT
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCPreviewData.RULE_ID_SELFIE
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCPreviewData.rule
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import ai.amani.sdk.utils.AppConstant.STATUS_NOT_UPLOADED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** The V2 home skip button: shown with the config text once only optional steps remain. */
@RunWith(JUnit4::class)
class HomeKYCMapperOptionalTest {

    private val requiredDoneOptionalLeft = listOf(
        rule(RULE_ID_DOCUMENT, "ID", STATUS_APPROVED, 0),
        rule(RULE_ID_SELFIE, "Selfie", STATUS_NOT_UPLOADED, 1, optional = true)
    )

    private fun configWithSkipText(text: String?) = HomeKYCPreviewData.config().let {
        it.copy(generalConfigs = it.generalConfigs!!.copy(buttonTextOptionalSteps = text))
    }

    @Test
    fun `the skip button carries the config text`() {
        val state = HomeKYCMapper.toUiState(requiredDoneOptionalLeft, configWithSkipText("Finish now"))
        assertEquals("Finish now", state.skipOptionalLabel)
    }

    @Test
    fun `without a skip text the button reads the continue text`() {
        val config = configWithSkipText(null)
        val state = HomeKYCMapper.toUiState(requiredDoneOptionalLeft, config)
        assertEquals(config.generalConfigs!!.continueText, state.skipOptionalLabel)
    }

    @Test
    fun `no skip button while a required step is open`() {
        val rules = listOf(
            rule(RULE_ID_DOCUMENT, "ID", STATUS_NOT_UPLOADED, 0),
            rule(RULE_ID_SELFIE, "Selfie", STATUS_NOT_UPLOADED, 1, optional = true)
        )
        assertNull(HomeKYCMapper.toUiState(rules, configWithSkipText("Finish now")).skipOptionalLabel)
    }

    @Test
    fun `a fresher verdict and an upload in flight are taken into account`() {
        val rules = listOf(
            rule(RULE_ID_DOCUMENT, "ID", STATUS_NOT_UPLOADED, 0),
            rule(RULE_ID_SELFIE, "Selfie", STATUS_NOT_UPLOADED, 1, optional = true)
        )
        val config = configWithSkipText("Finish now")
        val approvedBySocket = mapOf(RULE_ID_DOCUMENT to STATUS_APPROVED)

        assertEquals("Finish now", HomeKYCMapper.toUiState(rules, config, statusOverrides = approvedBySocket).skipOptionalLabel)
        assertNull(
            HomeKYCMapper.toUiState(
                rules, config, statusOverrides = approvedBySocket, processingRuleIds = setOf(RULE_ID_SELFIE)
            ).skipOptionalLabel
        )
    }
}
