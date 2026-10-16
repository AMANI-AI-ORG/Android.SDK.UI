package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCPreviewData.rule
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import ai.amani.sdk.utils.AppConstant.STATUS_NOT_UPLOADED
import ai.amani.sdk.utils.AppConstant.STATUS_PENDING_REVIEW
import ai.amani.sdk.utils.AppConstant.STATUS_PROCESSING
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** When the home screen offers to end the KYC with optional steps left. */
@RunWith(JUnit4::class)
class OptionalStepsTest {

    private fun canSkip(vararg rules: Rule, processing: Set<String> = emptySet()) =
        OptionalSteps.canSkipRemaining(rules.toList(), { it.status }, { it.id in processing })

    @Test
    fun `required steps done and an optional one left offers the skip`() {
        assertTrue(
            canSkip(
                rule("id", "ID", STATUS_APPROVED, 0),
                rule("selfie", "Selfie", STATUS_PENDING_REVIEW, 1),
                rule("address", "Address", STATUS_NOT_UPLOADED, 2, optional = true)
            )
        )
    }

    @Test
    fun `a required step not done yet offers no skip`() {
        assertFalse(
            canSkip(
                rule("id", "ID", STATUS_PROCESSING, 0),
                rule("address", "Address", STATUS_NOT_UPLOADED, 1, optional = true)
            )
        )
        assertFalse(
            canSkip(
                rule("id", "ID", STATUS_NOT_UPLOADED, 0),
                rule("address", "Address", STATUS_NOT_UPLOADED, 1, optional = true)
            )
        )
    }

    @Test
    fun `without optional steps there is nothing to skip`() {
        assertFalse(canSkip(rule("id", "ID", STATUS_APPROVED, 0), rule("selfie", "Selfie", STATUS_NOT_UPLOADED, 1)))
    }

    @Test
    fun `with every step done the automatic finish takes over`() {
        assertFalse(
            canSkip(
                rule("id", "ID", STATUS_APPROVED, 0),
                rule("address", "Address", STATUS_APPROVED, 1, optional = true)
            )
        )
    }

    @Test
    fun `an upload in flight hides the skip`() {
        assertFalse(
            canSkip(
                rule("id", "ID", STATUS_APPROVED, 0),
                rule("address", "Address", STATUS_NOT_UPLOADED, 1, optional = true),
                processing = setOf("address")
            )
        )
    }

    @Test
    fun `a step without the flag is required`() {
        assertFalse(
            canSkip(
                rule("id", "ID", STATUS_APPROVED, 0),
                rule("selfie", "Selfie", STATUS_NOT_UPLOADED, 1, optional = null),
                rule("address", "Address", STATUS_NOT_UPLOADED, 2, optional = true)
            )
        )
    }

    @Test
    fun `required steps done is told apart from optional ones`() {
        assertTrue(
            OptionalSteps.requiredCompleted(
                listOf(rule("id", "ID", STATUS_APPROVED, 0), rule("address", "Address", STATUS_NOT_UPLOADED, 1, optional = true))
            ) { it.status }
        )
    }
}
