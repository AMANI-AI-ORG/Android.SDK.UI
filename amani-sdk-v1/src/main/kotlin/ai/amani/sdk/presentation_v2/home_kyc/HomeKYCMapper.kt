package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation_v2.components.DotStep
import ai.amani.sdk.presentation_v2.components.StepError
import ai.amani.sdk.presentation_v2.components.StepRowStatus
import ai.amani.sdk.presentation_v2.components.StepStatus
import ai.amani.sdk.presentation_v2.components.VerificationStep
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import ai.amani.sdk.presentation_v2.theme.amaniV2PaletteFromHex
import ai.amani.sdk.presentation_v2.theme.toAmaniColorOrNull
import ai.amani.sdk.utils.AppConstant
import androidx.compose.ui.graphics.Color
import datamanager.model.config.ResGetConfig
import datamanager.model.config.StepConfig

/**
 * Pure, side-effect-free mapping from the shared SDK data layer (server [ResGetConfig]
 * GeneralConfigs + the customer KYC [Rule] list) into the V2 presentation models
 * ([AmaniV2Palette] + [HomeKYCUiState]).
 *
 * Kept separate from [HomeKYCViewModel] so the translation is unit-testable and the
 * view model stays focused on orchestration. Both colors and strings are config-driven
 * with static fallbacks: any missing/blank/malformed value falls back to the
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
     * Statuses whose server message is surfaced inline under the step. Mirrors v1's
     * KYCAdapter, which shows the `errors[0].errorMessage` subtitle not only for
     * REJECTED / AUTOMATICALLY_REJECTED but also for PENDING_REVIEW (a manual-review note).
     * The earlier V2 mapper gated this on [REJECTED_STATUSES] only, so PENDING_REVIEW
     * messages (and any rejection that arrives as pending review) were silently dropped.
     */
    private val ERROR_BEARING_STATUSES = setOf(
        AppConstant.STATUS_REJECTED,
        AppConstant.STATUS_AUTOMATICALLY_REJECTED,
        AppConstant.STATUS_PENDING_REVIEW
    )

    /**
     * Master switch for the static error fallback. When `true`, a rejected step that the
     * server returned *no* message for still shows the inline error block, using
     * [STATIC_ERROR_FALLBACK] so the card explains itself. Flip to
     * `false` to render the inline error only when the backend actually sends a message.
     */
    private const val STATIC_ERROR_FALLBACK_ENABLED = true

    // TODO: config-driven
    private const val STATIC_STEP_DURATION = "~30 sec"

    /**
     * Generic rejection message shown when [STATIC_ERROR_FALLBACK_ENABLED] is on and the
     * server provided no per-step message. Pending-review steps are *not* given this
     * fallback (they carry no rejection text); only true rejections fall back to it.
     */
    // TODO: config-driven
    private val STATIC_ERROR_FALLBACK = StepError(
        title = "We couldn't verify this step",
        message = "Something didn't look right. Please tap the step and try again."
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
     *
     * The cached [Rule] objects are immutable (no status setter), so live updates from
     * the upload flow are layered on as overlays rather than mutated in place:
     *  - [statusOverrides] maps a rule id to a fresher status (from the AmaniEvent
     *    `stepsResult` socket, or a failed upload) and wins over [Rule.status].
     *  - [errorOverrides] maps a rule id to the server rejection message to show inline.
     *  - [processingRuleId] is the rule currently uploading / awaiting the verdict; its
     *    row renders the [StepRowStatus.Processing] spinner.
     */
    fun toUiState(
        rules: List<Rule>,
        config: ResGetConfig?,
        statusOverrides: Map<String, String> = emptyMap(),
        errorOverrides: Map<String, String> = emptyMap(),
        processingRuleId: String? = null
    ): HomeKYCUiState {
        val g = config?.generalConfigs
        fun eff(rule: Rule): String? = effectiveStatus(rule, statusOverrides)
        // The active step is the first one still needing the user (skipping the one that
        // is currently uploading, which shows its own processing spinner instead, and any
        // step still locked behind an unmet mandatory dependency). This is the single
        // source of truth shared with the view model so the home primary button targets
        // (and enables on) exactly this step.
        val activeIndex = activeRuleIndex(rules, config, statusOverrides, processingRuleId)

        val dots = rules.mapIndexed { index, rule ->
            DotStep(
                // Per-step label is server content (the KYC rule title), not a
                // GeneralConfigs field — kept as-is, blank when the rule has none.
                label = rule.title ?: "",
                status = dotStatus(eff(rule), index == activeIndex)
            )
        }

        val steps = rules.mapIndexed { index, rule ->
            val isProcessing = rule.id != null && rule.id == processingRuleId
            val status = eff(rule)
            // Per-step server styling (v1 KYCAdapter: appConfig.getStepConfig(sortOrder)),
            // selected by the rule's status with a "processing" override while uploading.
            val stepConfig = config?.stepConfigForSortOrder(rule.sortOrder)
            val style = stepStyle(stepConfig, status, isProcessing)
            VerificationStep(
                index = index + 1,
                // Top line: the config-driven status label (StepConfig.buttonText for this
                // status, v1's textOfButton) — moved up from the old subtitle slot. Falls back
                // to the server rule title when config has no label so the row is never blank.
                title = style.label ?: rule.title ?: "",
                // Bottom line: static, informative duration hint (see STATIC_STEP_DURATION).
                // The status label used to live here; it now sits on the top line.
                subtitle = STATIC_STEP_DURATION,
                status = if (isProcessing) StepRowStatus.Processing
                else rowStatus(status, index == activeIndex),
                // Config-driven per-status colors (StepConfig). buttonColor is the saturated
                // accent (border, inner wash, number-badge fill, trailing icon); buttonTextColor
                // drives the step's text. The number-badge glyph stays white (set in StepRow).
                accentColor = style.accentColor,
                textColor = style.textColor,
                error = if (isProcessing) null else errorFor(
                    status = status,
                    // Fresh socket / upload message wins; otherwise fall back to the
                    // message baked into the cached rule (initially-rejected steps).
                    overrideMessage = errorOverrides[rule.id]
                        ?: rule.errors?.firstOrNull()?.errorMessage?.toString()
                )
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
            primaryButtonText = g?.continueText.orFallback("Continue"),
            // The primary button only advances when there's a step ready for the user. It
            // is disabled while the active step is uploading / awaiting its verdict (and the
            // next step is still locked behind it), and re-enables once that step is approved
            // and the next becomes active.
            primaryButtonEnabled = activeIndex >= 0
        )
    }

    private fun effectiveStatus(rule: Rule, statusOverrides: Map<String, String>): String? =
        statusOverrides[rule.id] ?: rule.status

    /**
     * v1's mandatory-step gate (KYCAdapter): a step whose [StepConfig] declares
     * `mandatoryStepIDs` cannot become active until every referenced step is APPROVED or
     * PENDING_REVIEW. No mandatory ids → always unlocked (steps aren't gated). This is what
     * keeps a dependent step Locked while the step it depends on is still processing /
     * uploading, and unlocks it once that prerequisite is approved.
     */
    private fun isUnlocked(
        rule: Rule,
        rules: List<Rule>,
        config: ResGetConfig?,
        statusOverrides: Map<String, String>
    ): Boolean {
        val mandatoryIds = config?.stepConfigForSortOrder(rule.sortOrder)?.mandatoryStepIDs
        if (mandatoryIds.isNullOrEmpty()) return true
        return mandatoryIds.all { mId ->
            val depStatus = rules.firstOrNull { it.id == mId }?.let { effectiveStatus(it, statusOverrides) }
            depStatus == AppConstant.STATUS_APPROVED || depStatus == AppConstant.STATUS_PENDING_REVIEW
        }
    }

    /**
     * Index of the active step: the first one still needing the user — not done and not
     * locked behind an unmet mandatory dependency. `-1` when nothing is actionable right
     * now.
     *
     * Crucially, **while any step is uploading / awaiting its verdict ([processingRuleId]
     * is set) the whole flow is gated**: this returns `-1` so no other step becomes Active
     * and the home primary button stays disabled. Otherwise a step with no mandatory
     * dependency (e.g. Selfie sitting below an ID that is still processing) would be picked
     * as the next active step and re-enable the button before the in-flight step's verdict
     * arrives. Only once the AmaniEvent verdict clears [processingRuleId] does the scan
     * resume — and an APPROVED prerequisite then unlocks its dependent step.
     */
    private fun activeRuleIndex(
        rules: List<Rule>,
        config: ResGetConfig?,
        statusOverrides: Map<String, String>,
        processingRuleId: String?
    ): Int {
        // A step is in flight: gate everything until its verdict resolves the processing lock.
        if (processingRuleId != null) return -1
        return rules.indexOfFirst {
            !isDone(effectiveStatus(it, statusOverrides)) &&
                isUnlocked(it, rules, config, statusOverrides)
        }
    }

    /**
     * The step the home primary button should start, applying the exact same overlay-,
     * processing-, and mandatory-lock-aware logic used to mark the Active row. Returns
     * `null` when no step is actionable (button disabled / no-op), so the button can never
     * re-open a just-approved step or jump a step that is still locked while another
     * processes. Shared with [toUiState] to keep navigation and the rendered state in sync.
     */
    fun resolveActiveRule(
        rules: List<Rule>,
        config: ResGetConfig?,
        statusOverrides: Map<String, String> = emptyMap(),
        processingRuleId: String? = null
    ): Rule? =
        activeRuleIndex(rules, config, statusOverrides, processingRuleId).takeIf { it >= 0 }?.let { rules[it] }

    private fun isDone(status: String?): Boolean = status in DONE_STATUSES

    private fun dotStatus(status: String?, isActive: Boolean): StepStatus = when {
        status in DONE_STATUSES -> StepStatus.Completed
        status in REJECTED_STATUSES -> StepStatus.Rejected
        isActive -> StepStatus.Current
        else -> StepStatus.Pending
    }

    private fun rowStatus(status: String?, isActive: Boolean): StepRowStatus = when {
        status in DONE_STATUSES -> StepRowStatus.Done
        status in REJECTED_STATUSES -> StepRowStatus.Rejected
        isActive -> StepRowStatus.Active
        else -> StepRowStatus.Locked
    }

    /**
     * Config-driven, per-status styling for one step.
     *  - [label]: StepConfig.buttonText for the status (the status subtitle).
     *  - [accentColor]: StepConfig.buttonColor — the saturated fill; drives the border,
     *    inner wash, badge fill, and trailing icon.
     *  - [textColor]: StepConfig.buttonTextColor — drives the step's title/subtitle text.
     *    (The number-badge glyph itself stays white, set in StepRow.)
     */
    private data class StepStyle(
        val label: String?,
        val accentColor: Color?,
        val textColor: Color?
    )

    /**
     * Resolves the [StepConfig] for [sortOrder] the same way v1's `ResGetConfig.getStepConfig`
     * does — by 1-based position in the ordered `stepConfigs` list — but null-safe instead of
     * throwing when the index is out of range.
     */
    private fun ResGetConfig.stepConfigForSortOrder(sortOrder: Int?): StepConfig? {
        val configs = stepConfigs ?: return null
        val index = (sortOrder ?: return null) - 1
        return configs.getOrNull(index)
    }

    /**
     * Picks the per-status fields from the step's [StepConfig], mirroring v1's KYCAdapter
     * `when (status)` branches: each status maps to its matching buttonText / buttonColor /
     * buttonTextColor, and a step that is currently uploading uses the `processing` fields
     * (v1's `isShowLoader` branch). Returns blanks/nulls when config is missing so the row
     * falls back to the built-in V2 design.
     */
    private fun stepStyle(stepConfig: StepConfig?, status: String?, isProcessing: Boolean): StepStyle {
        if (stepConfig == null) return StepStyle(null, null, null)
        val bc = stepConfig.buttonColor
        val bt = stepConfig.buttonText
        val btc = stepConfig.buttonTextColor
        val key = if (isProcessing) AppConstant.STATUS_PROCESSING else status
        val (labelRaw, accentHex, textHex) = when (key) {
            AppConstant.STATUS_APPROVED -> Triple(bt?.approved, bc?.approved, btc?.approved)
            AppConstant.STATUS_REJECTED -> Triple(bt?.rejected, bc?.rejected, btc?.rejected)
            AppConstant.STATUS_AUTOMATICALLY_REJECTED ->
                Triple(bt?.automaticallyRejected, bc?.automaticallyRejected, btc?.automaticallyRejected)
            AppConstant.STATUS_PENDING_REVIEW -> Triple(bt?.pendingReview, bc?.pendingReview, btc?.pendingReview)
            AppConstant.STATUS_PROCESSING -> Triple(bt?.processing, bc?.processing, btc?.processing)
            // NOT_UPLOADED and any unknown/null status fall back to the not-uploaded fields,
            // matching v1 (the default actionable step state).
            else -> Triple(bt?.notUploaded, bc?.notUploaded, btc?.notUploaded)
        }
        return StepStyle(
            label = labelRaw?.takeIf { it.isNotBlank() },
            accentColor = accentHex.toAmaniColorOrNull(),
            textColor = textHex.toAmaniColorOrNull()
        )
    }

    /**
     * Surfaces the server message on a step as an inline block, for any
     * [ERROR_BEARING_STATUSES] status (rejected, auto-rejected, or pending review — same
     * as v1's KYCAdapter). The [overrideMessage] is the AmaniEvent socket / failed-upload
     * message, falling back to the cached [Rule.errors] message at the call site. The error
     * *title* had no GeneralConfigs equivalent so it is dropped (null).
     */
    private fun errorFor(status: String?, overrideMessage: String?): StepError? {
        if (status !in ERROR_BEARING_STATUSES) return null
        // Server message wins when present.
        val message = overrideMessage?.takeIf { it.isNotBlank() }
        if (message != null) return StepError(title = null, message = message)
        // No backend message: show the static fallback (toggleable) so a *rejected* step
        // still explains itself. Pending-review without a message stays silent.
        return if (STATIC_ERROR_FALLBACK_ENABLED && status in REJECTED_STATUSES) {
            STATIC_ERROR_FALLBACK
        } else {
            null
        }
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
