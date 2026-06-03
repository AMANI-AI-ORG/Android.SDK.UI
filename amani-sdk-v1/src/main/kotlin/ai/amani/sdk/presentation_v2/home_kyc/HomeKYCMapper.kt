package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation_v2.components.DotStep
import ai.amani.sdk.presentation_v2.components.StepError
import ai.amani.sdk.presentation_v2.components.StepRowStatus
import ai.amani.sdk.presentation_v2.components.StepStatus
import ai.amani.sdk.presentation_v2.components.VerificationStep
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.amaniV2PaletteFromHex
import ai.amani.sdk.utils.AppConstant
import datamanager.model.config.ResGetConfig

/**
 * Pure, side-effect-free mapping from the shared SDK data layer (server [ResGetConfig]
 * GeneralConfigs + the customer KYC [Rule] list) into the V2 presentation models
 * ([AmaniV2Palette] + [HomeKYCUiState]).
 *
 * Kept separate from [HomeKYCViewModel] so the translation is unit-testable and the
 * view model stays focused on orchestration. Both colors and strings are config-driven
 * with static fallbacks: any missing/blank/malformed value falls back to the HTML
 * defaults baked into [AmaniV2Palette] / the literals below.
 */
internal object HomeKYCMapper {

    /** Statuses that count as "finished" for stepper display purposes. */
    private val DONE_STATUSES = setOf(
        AppConstant.STATUS_APPROVED,
        AppConstant.STATUS_PENDING_REVIEW,
        AppConstant.STATUS_PROCESSING
    )

    /** Statuses that need the user's attention again. */
    private val REJECTED_STATUSES = setOf(
        AppConstant.STATUS_REJECTED,
        AppConstant.STATUS_AUTOMATICALLY_REJECTED
    )

    /**
     * Builds the brand palette from the server GeneralConfigs hex colors. This is the
     * single entry point for config colors into the V2 theme; everything downstream
     * reads from the returned palette.
     */
    fun resolvePalette(config: ResGetConfig?): AmaniV2Palette {
        val g = config?.generalConfigs ?: return AmaniV2Palette()
        return amaniV2PaletteFromHex(
            accent = g.primaryButtonBackgroundColor,
            ink = g.appFontColor,
            background = g.appBackground
        )
    }

    /**
     * Maps the KYC [rules] (already filtered + sorted) plus config strings into the
     * stateless [HomeKYCUiState]. The first step that is neither done nor approved
     * becomes the "active" step (Current dot / Active row); later unfinished steps are
     * Locked, matching the v1 sequential-unlock behaviour.
     */
    fun toUiState(rules: List<Rule>, config: ResGetConfig?): HomeKYCUiState {
        val g = config?.generalConfigs
        val activeIndex = rules.indexOfFirst { !isDone(it.status) }

        val dots = rules.map { rule ->
            DotStep(
                // Per-step label is server content (the KYC rule title), not a
                // GeneralConfigs field — kept as-is, blank when the rule has none.
                label = rule.title ?: "",
                status = dotStatus(rule, rules.indexOf(rule) == activeIndex)
            )
        }

        val steps = rules.mapIndexed { index, rule ->
            VerificationStep(
                index = index + 1,
                // Server content (rule title) — kept.
                title = rule.title ?: "",
                // Step status label ("Up next" / "Locked" / …) had no GeneralConfigs
                // equivalent, so it is dropped (null = the row renders no subtitle).
                subtitle = null,
                status = rowStatus(rule, index == activeIndex),
                error = errorFor(rule)
            )
        }

        return HomeKYCUiState(
            // Config-driven: GeneralConfigs.mainTitleText (v1 toolbar title), fallback when blank/null.
            headerTitle = g?.mainTitleText.orFallback("Verification"),
            dots = dots,
            // The large page heading had no dedicated GeneralConfigs field (mainTitleText
            // already drives the toolbar), so it is dropped (null = not rendered).
            title = null,
            // Config-driven: GeneralConfigs.mainDescriptionText (v1 description), fallback when blank/null.
            subtitle = g?.mainDescriptionText.orFallback("Complete the steps below to finish verification."),
            steps = steps,
            // Config-driven: GeneralConfigs.continueText, fallback when blank/null.
            primaryButtonText = g?.continueText.orFallback("Continue")
        )
    }

    private fun isDone(status: String?): Boolean = status in DONE_STATUSES

    private fun dotStatus(rule: Rule, isActive: Boolean): StepStatus = when {
        rule.status in DONE_STATUSES -> StepStatus.Completed
        rule.status in REJECTED_STATUSES -> StepStatus.Rejected
        isActive -> StepStatus.Current
        else -> StepStatus.Pending
    }

    private fun rowStatus(rule: Rule, isActive: Boolean): StepRowStatus = when {
        rule.status in DONE_STATUSES -> StepRowStatus.Done
        rule.status in REJECTED_STATUSES -> StepRowStatus.Rejected
        isActive -> StepRowStatus.Active
        else -> StepRowStatus.Locked
    }

    /**
     * Surfaces the first server error message on a rejected step as an inline block.
     * The error *title* had no GeneralConfigs equivalent so it is dropped (null); only
     * the server-provided [datamanager error message] is shown.
     */
    private fun errorFor(rule: Rule): StepError? {
        if (rule.status !in REJECTED_STATUSES) return null
        val message = rule.errors?.firstOrNull()?.errorMessage?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return StepError(title = null, message = message)
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
