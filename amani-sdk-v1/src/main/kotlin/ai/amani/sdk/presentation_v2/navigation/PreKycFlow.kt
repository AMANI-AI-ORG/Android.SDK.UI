package ai.amani.sdk.presentation_v2.navigation

import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.utils.AppConstant
import ai.amani.sdk.utils.AppConstant.STATUS_APPROVED
import datamanager.model.config.StepConfig

/**
 * "Before KYC" step sequencing — the V2 port of v1's
 * `HomeKYCViewModel.hasStepsBeforeOrAfterKYCFlow`. v1 runs the identifier steps
 * ([AppConstant.STEPS_BEFORE_KYC_FLOW] — profile_info / phone_otp / email_otp / questionnaire)
 * that sit *before* the first KYC step and aren't approved yet, ahead of the KYC overview.
 *
 * V2 only ships the questionnaire screen so far, so this object exposes just what's needed to
 * trigger it with the same rule v1 uses; the remaining identifiers are detected the same way
 * and will slot in as their V2 screens land.
 */
internal object PreKycFlow {

    /**
     * Identifier steps completed in this session. v1 removes each step from its working list as
     * it finishes (`getNavDirection`); V2 can't mutate the cached config, so the chain tracks
     * completions here and skips them when picking the next step. Reset when the nav host is
     * (re)created via [initialBackStack].
     */
    private val completed = mutableSetOf<String>()

    fun reset() = completed.clear()

    fun markCompleted(identifier: String) {
        completed.add(identifier)
    }

    /**
     * The V2 screen for an identifier step, or null when V2 doesn't ship one yet. Steps with no
     * screen are skipped by the chain (they can't be completed in V2 anyway) — added here as
     * their screens land (phone_otp / email_otp).
     */
    fun destinationFor(identifier: String?): AmaniV2Destination? = when (identifier) {
        AppConstant.IDENTIFIER_PROFILE_INFO -> AmaniV2Destination.ProfileInfo
        AppConstant.IDENTIFIER_QUESTIONNAIRE -> AmaniV2Destination.Questionnaire
        AppConstant.IDENTIFIER_PHONE_OTP -> AmaniV2Destination.PhoneOtp
        AppConstant.IDENTIFIER_EMAIL_OTP -> AmaniV2Destination.EmailOtp
        else -> null
    }

    /** Next not-yet-completed *before*-KYC step with a V2 screen, or null when the chain is done. */
    fun nextBeforeKycStep(): AmaniV2Destination? = nextStep(pendingBeforeKycSteps())

    /** Next not-yet-completed *after*-KYC step with a V2 screen, or null when the chain is done. */
    fun nextAfterKycStep(): AmaniV2Destination? = nextStep(pendingAfterKycSteps())

    private fun nextStep(pending: List<StepConfig>): AmaniV2Destination? =
        pending.firstNotNullOfOrNull { step ->
            if (step.identifier in completed) null else destinationFor(step.identifier)
        }

    /**
     * The not-yet-approved [STEPS_BEFORE_KYC_FLOW][AppConstant.STEPS_BEFORE_KYC_FLOW] steps that
     * appear *before* the first KYC step, in config order — the v2 port of v1's before-KYC list.
     */
    fun pendingBeforeKycSteps(): List<StepConfig> = pendingIdentifierSteps(beforeKyc = true)

    /**
     * The not-yet-approved identifier steps that appear *after* the KYC steps — the v2 port of
     * v1's after-KYC list (`hasOnlyStepsAfterKYCFlow`). These run once every KYC step is
     * approved, before the flow completes.
     */
    fun pendingAfterKycSteps(): List<StepConfig> = pendingIdentifierSteps(beforeKyc = false)

    /**
     * Pending identifier ([STEPS_BEFORE_KYC_FLOW][AppConstant.STEPS_BEFORE_KYC_FLOW]) steps on one
     * side of the KYC block, deciding before/after by position relative to the first KYC step —
     * exactly v1's rule. Reads the same shared cache v1 does ([CachingHomeKYC]); empty until
     * config + customer detail have loaded.
     */
    private fun pendingIdentifierSteps(beforeKyc: Boolean): List<StepConfig> {
        val appConfig = CachingHomeKYC.appConfig ?: return emptyList()
        val rules = CachingHomeKYC.customerDetail?.rules ?: return emptyList()

        // Config steps this customer actually has (a step with a matching customer rule) — v1
        // filters the same way before deciding before/after KYC.
        val steps = appConfig.stepConfigs.orEmpty().filter { step ->
            rules.any { it.id == step.id }
        }

        // Index where the KYC steps begin (identifier blank or "kyc"); everything before it is
        // "before KYC", everything from it on is "after KYC". No KYC marker → all before-KYC.
        val firstKycIndex = steps
            .indexOfFirst { it.identifier == "" || it.identifier == "kyc" }
            .let { if (it == -1) steps.size else it }

        return steps.filterIndexed { index, step ->
            val onRequestedSide = if (beforeKyc) index < firstKycIndex else index >= firstKycIndex
            onRequestedSide &&
                AppConstant.STEPS_BEFORE_KYC_FLOW.contains(step.identifier) &&
                // Same "not approved yet" gate as v1 (per-step, matched by rule id).
                rules.firstOrNull { it.id == step.id }?.status != STATUS_APPROVED
        }
    }

    /**
     * Initial back stack for the nav host: normally just [root] (Home), but when a before-KYC
     * identifier step is pending it seeds `[root, firstStep]` so that step opens first (v1
     * redirects to it from Home) while Home stays underneath. Each step then advances to the
     * next pending one or pops to Home (see AmaniV2NavHost). Resets the completed-set here so a
     * fresh nav host starts a fresh chain; decided once at navigator creation.
     */
    fun initialBackStack(root: AmaniV2Destination): List<AmaniV2Destination> {
        if (root != AmaniV2Destination.HomeKYC) return listOf(root)
        reset()
        val first = nextBeforeKycStep() ?: return listOf(root)
        return listOf(root, first)
    }
}
