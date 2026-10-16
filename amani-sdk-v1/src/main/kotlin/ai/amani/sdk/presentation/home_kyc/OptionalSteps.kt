package ai.amani.sdk.presentation.home_kyc

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.utils.AppConstant

/**
 * When the user may end the KYC before finishing its optional steps (`Rule.optional == true`).
 * Shared by the V1 and V2 home screens, so both show the skip button under the same rule.
 */
internal object OptionalSteps {

    /** A step counts as completed with these statuses, as for the automatic finish. */
    private val COMPLETED = setOf(AppConstant.STATUS_APPROVED, AppConstant.STATUS_PENDING_REVIEW)

    /** True when every required step is completed; optional steps are not looked at. */
    fun requiredCompleted(rules: List<Rule>, statusOf: (Rule) -> String?): Boolean =
        rules.filter { it.optional != true }.all { statusOf(it) in COMPLETED }

    /**
     * True when every required step is completed, at least one optional step is not, and no
     * step is uploading (the KYC must not end under an upload in flight). A step without the
     * flag (`optional == null`) is required, so profiles from older backends never see the skip.
     * When every step is completed this is false: the automatic finish takes over.
     *
     * @param statusOf the step's current status (the cached status with any fresher verdict on top)
     * @param processing whether the step's upload is in flight or awaiting its verdict
     */
    fun canSkipRemaining(
        rules: List<Rule>,
        statusOf: (Rule) -> String?,
        processing: (Rule) -> Boolean
    ): Boolean {
        if (rules.any(processing)) return false
        return requiredCompleted(rules, statusOf) &&
            rules.any { it.optional == true && statusOf(it) !in COMPLETED }
    }
}
