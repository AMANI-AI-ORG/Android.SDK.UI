package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation_v2.home_kyc.HomeKYCPreviewData.rule
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import ai.amani.sdk.utils.AppConstant.STATUS_NOT_UPLOADED
import ai.amani.sdk.utils.AppConstant.STATUS_PENDING_REVIEW
import ai.amani.sdk.utils.AppConstant.STATUS_PROCESSING
import ai.amani.sdk.utils.ProfileStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** What the SDK returns when the user closes it from the steps screen. */
@RunWith(JUnit4::class)
class ExitResultTest {

    @After
    fun clear() = CachingHomeKYC.clearCache()

    private fun exitWith(vararg rules: Rule): String {
        CachingHomeKYC.onlyKYCRules = ArrayList(rules.toList())
        return CachingHomeKYC.exitResult().profileStatus
    }

    @Test
    fun `only optional steps left returns approved`() {
        assertEquals(
            ProfileStatus.APPROVED,
            exitWith(
                rule("id", "ID", STATUS_APPROVED, 0),
                rule("selfie", "Selfie", STATUS_PENDING_REVIEW, 1),
                rule("address", "Address", STATUS_NOT_UPLOADED, 2, optional = true)
            )
        )
    }

    @Test
    fun `a required step left returns incomplete`() {
        assertEquals(
            ProfileStatus.INCOMPLETE,
            exitWith(
                rule("id", "ID", STATUS_PROCESSING, 0),
                rule("address", "Address", STATUS_NOT_UPLOADED, 1, optional = true)
            )
        )
    }

    @Test
    fun `a fresher verdict counts over the cached status`() {
        CachingHomeKYC.rememberStepStatus("id", STATUS_APPROVED)
        assertEquals(
            ProfileStatus.APPROVED,
            exitWith(
                rule("id", "ID", STATUS_NOT_UPLOADED, 0),
                rule("address", "Address", STATUS_NOT_UPLOADED, 1, optional = true)
            )
        )
    }

    @Test
    fun `a step without the optional flag is required`() {
        assertEquals(
            ProfileStatus.INCOMPLETE,
            exitWith(
                rule("id", "ID", STATUS_APPROVED, 0),
                rule("address", "Address", STATUS_NOT_UPLOADED, 1)
            )
        )
    }

    @Test
    fun `no steps loaded returns incomplete`() {
        assertEquals(ProfileStatus.INCOMPLETE, CachingHomeKYC.exitResult().profileStatus)
    }
}
