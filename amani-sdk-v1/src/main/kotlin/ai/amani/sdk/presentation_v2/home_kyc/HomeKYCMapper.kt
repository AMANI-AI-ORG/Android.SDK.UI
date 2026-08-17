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
import datamanager.model.config.Version

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
    /**
     * Master switch for the static error fallback. When `true`, a rejected step that the
     * server returned *no* message for still shows the inline error block, using
     * [STATIC_ERROR_FALLBACK] so the card explains itself. `false` renders the inline
     * error only from the message carried by the step itself ([Rule.errors] / the socket
     * verdict) — v1 behaviour, and the current product decision.
     */
    private const val STATIC_ERROR_FALLBACK_ENABLED = true

    /** Fallback when the step's version carries no `v2EstimatedTime`. */
    private const val STATIC_STEP_DURATION = "~30 sec"

    /**
     * Generic rejection message shown when [STATIC_ERROR_FALLBACK_ENABLED] is on and
     * neither the server nor the step's version config (`v2StepRejectionTitle` /
     * `v2StepRejectionDescription`) provided a per-step message. Pending-review steps are
     * *not* given this fallback (they carry no rejection text); only true rejections fall
     * back to it.
     */
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
        // Every GeneralConfigs color field with a V2 counterpart, mapped 1:1; anything
        // null/blank/malformed falls back inside amaniV2PaletteFromHex.
        return amaniV2PaletteFromHex(
            accent = g.primaryButtonBackgroundColor,
            ink = g.appFontColor,
            background = g.appBackground,
            success = g.successIconColor,
            danger = g.errorIconColor,
            topBar = g.topBarBackground,
            topBarFont = g.topBarFontColor,
            primaryButtonText = g.primaryButtonTextColor,
            primaryButtonBorder = g.primaryButtonBorderColor,
            secondaryButtonBackground = g.secondaryButtonBackgroundColor,
            secondaryButtonText = g.secondaryButtonTextColor,
            secondaryButtonBorder = g.secondaryButtonBorderColor,
            loader = g.loaderColor,
            buttonRadius = g.buttonRadiusAndroid
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

        // Aggregate step counts that drive the home heading + CTA wording. "Done" here is
        // deliberately narrower than [DONE_STATUSES]: a step that is merely uploading /
        // server-processing hasn't been completed by the user yet, so it must not flip the
        // heading into the progress state (the design keeps the initial heading while the
        // first step processes).
        val doneCount = rules.count {
            eff(it) == AppConstant.STATUS_APPROVED || eff(it) == AppConstant.STATUS_PENDING_REVIEW
        }
        val rejectedCount = rules.count { eff(it) in REJECTED_STATUSES }

        val dots = rules.mapIndexed { index, rule ->
            DotStep(
                // Per-step label is server content (the KYC rule title), not a
                // GeneralConfigs field — kept as-is, blank when the rule has none.
                label = rule.title ?: "",
                status = dotStatus(eff(rule), index == activeIndex)
            )
        }

        // "Start here · ~30 sec" on a fresh flow; once something is done the next
        // actionable step reads "Up next · ~30 sec" instead (design v2.6).
        val actionLabel = if (doneCount > 0) {
            g?.v2StepUpNextLabel.orFallback("Up next")
        } else {
            g?.v2StepStartHereLabel.orFallback("Start here")
        }

        val steps = rules.mapIndexed { index, rule ->
            val isProcessing = rule.id != null && rule.id == processingRuleId
            val status = eff(rule)
            // Per-step server styling (v1 KYCAdapter: appConfig.getStepConfig(sortOrder)),
            // selected by the rule's status with a "processing" override while uploading.
            val stepConfig = config?.stepConfigForSortOrder(rule.sortOrder)
            val style = stepStyle(stepConfig, status, isProcessing)
            // Gating is per step (v1 KYCAdapter parity): a step is selectable when its OWN
            // `mandatoryStepIDs` are satisfied. An upload in flight gates the whole flow.
            val isActive = processingRuleId == null &&
                isUnlocked(rule, rules, config, statusOverrides)
            val rowStatus = if (isProcessing) StepRowStatus.Processing
            else rowStatus(status, isActive)
            // Primary-button label to show while this step is selected — same prefix logic
            // as the default CTA below, per step. Only actionable rows carry one.
            val ctaLabel = if (rowStatus == StepRowStatus.Active || rowStatus == StepRowStatus.Rejected) {
                "${ctaPrefix(status, doneCount, g)} ${stepConfig?.buttonText?.notUploaded.orEmpty()}".trim()
            } else null
            // The step's version config carries the v2 per-document strings (estimated
            // time, rejection fallback texts).
            val version = config?.firstVisibleVersionFor(rule.sortOrder)
            VerificationStep(
                index = index + 1,
                ruleId = rule.id,
                // Top line: the step's action name — stable across statuses (design v2.6:
                // "Upload ID" stays the title while the status moves to the subtitle).
                // buttonText.notUploaded is that action label; the server rule title is the
                // fallback so the row is never blank.
                title = (stepConfig?.buttonText?.notUploaded?.takeIf { it.isNotBlank() })
                    ?: rule.title ?: "",
                // Bottom line: actionable steps show "<Start here|Up next> · <estimated
                // time>" (config-driven); every other status shows its config status label
                // (StepConfig.buttonText — "Document is currently processing", "Your ID
                // rejected. Please try again", …).
                subtitle = when (rowStatus) {
                    StepRowStatus.Active, StepRowStatus.Locked ->
                        "$actionLabel · ${version?.v2EstimatedTime.orFallback(STATIC_STEP_DURATION)}"
                    else -> style.label
                },
                status = rowStatus,
                // Config pair for the status: buttonColor + buttonTextColor (see StepRow).
                fillColor = style.fillColor,
                textColor = style.textColor,
                ctaLabel = ctaLabel,
                error = if (isProcessing) null else errorFor(
                    status = status,
                    // Fresh socket / upload message wins; otherwise fall back to the
                    // message carried by the step itself (initially-rejected steps) —
                    // first non-blank across the rule's errors.
                    overrideMessage = errorOverrides[rule.id]
                        ?: rule.errors?.firstNotNullOfOrNull {
                            it?.errorMessage?.toString()?.takeIf(String::isNotBlank)
                        },
                    version = version
                )
            )
        }

        // Home heading follows the flow state (design v2.6): rejected wins, then progress
        // (something already done), then the fresh-start heading. The config strings are
        // sentence *suffixes* — the step-count prefix is composed here.
        val remaining = rules.size - doneCount
        val (title, subtitle) = when {
            rejectedCount > 0 -> Pair(
                g?.v2HomeRejectedTitle.orFallback("Verification incomplete"),
                countWord(rejectedCount) +
                    (if (rejectedCount == 1) " step needs " else " steps need ") +
                    g?.v2HomeRejectedSubtitle.orFallback("your attention before we can continue.")
            )
            doneCount > 0 -> Pair(
                g?.v2HomeProgressTitle.orFallback("You're making progress"),
                countWord(remaining) +
                    (if (remaining == 1) " more step " else " more steps ") +
                    g?.v2HomeProgressSubtitle.orFallback("to finish verification.")
            )
            else -> Pair(
                g?.v2HomeInitialTitle.orFallback("Let's get you verified"),
                countWord(rules.size) + " " +
                    g?.v2HomeInitialSubtitle.orFallback("quick steps. Should take about 2 minutes.")
            )
        }

        // CTA names the step it will start: "Start with Identification" on a fresh flow,
        // "Continue with Selfie" mid-flow, "Retake Identification" after a rejection. While
        // a step is uploading (nothing actionable) the label still names the next step but
        // the button is disabled. No step at all → plain config continue text.
        val ctaRule = activeIndex.takeIf { it >= 0 }?.let { rules[it] }
            ?: rules.firstOrNull { !isDone(eff(it)) && it.id != processingRuleId }
        val primaryButtonText = if (ctaRule != null) {
            "${ctaPrefix(eff(ctaRule), doneCount, g)} ${ctaRule.title.orEmpty()}".trim()
        } else {
            g?.continueText.orFallback("Continue")
        }

        return HomeKYCUiState(
            // Config-driven: GeneralConfigs.mainTitleText (v1 toolbar title), fallback when blank/null.
            headerTitle = g?.mainTitleText.orFallback("Verification"),
            dots = dots,
            title = title,
            subtitle = subtitle,
            steps = steps,
            primaryButtonText = primaryButtonText,
            // The primary button only advances when there's a step ready for the user. It
            // is disabled while the active step is uploading / awaiting its verdict (and the
            // next step is still locked behind it), and re-enables once that step is approved
            // and the next becomes active.
            primaryButtonEnabled = activeIndex >= 0
        )
    }

    /**
     * Spelled-out step count for the home subtitles ("Four quick steps…", "One step
     * needs…"). Past ten (unrealistic for a KYC flow) the numeral is used as-is.
     */
    private fun countWord(count: Int): String = when (count) {
        1 -> "One"; 2 -> "Two"; 3 -> "Three"; 4 -> "Four"; 5 -> "Five"
        6 -> "Six"; 7 -> "Seven"; 8 -> "Eight"; 9 -> "Nine"; 10 -> "Ten"
        else -> count.toString()
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
    /**
     * CTA verb prefix for a step given its status and the overall progress: "Retake" for a
     * rejected step, "Continue with" once something is done, otherwise "Start with" (all
     * config-driven with fallbacks). Shared by the default primary label and each step's
     * per-selection [VerificationStep.ctaLabel].
     */
    private fun ctaPrefix(status: String?, doneCount: Int, g: datamanager.model.config.GeneralConfigs?): String =
        when {
            status in REJECTED_STATUSES -> g?.v2HomeCtaRetake.orFallback("Retake")
            doneCount > 0 -> g?.v2HomeCtaContinue.orFallback("Continue with")
            else -> g?.v2HomeCtaStart.orFallback("Start with")
        }

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
     *  - [fillColor]: StepConfig.buttonColor — the status color (card wash, border, badge).
     *  - [textColor]: StepConfig.buttonTextColor — the color drawn on that fill.
     */
    private data class StepStyle(
        val label: String?,
        val fillColor: Color?,
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
     * First selectable (non-hidden) [Version] of the step at [sortOrder] — the carrier of
     * the per-document v2 strings (`v2EstimatedTime`, `v2StepRejection*`). Read-only: unlike
     * CaptureFlow.prepareVersions it stamps nothing onto the version objects.
     */
    private fun ResGetConfig.firstVisibleVersionFor(sortOrder: Int?): Version? {
        val documents = stepConfigForSortOrder(sortOrder)?.mDocuments ?: return null
        return documents.firstNotNullOfOrNull { document ->
            document?.versions?.firstOrNull { it.isHidden != true }
        }
    }

    /**
     * Picks the per-status fields from the step's [StepConfig], mirroring v1's KYCAdapter
     * `when (status)` branches: each status maps to its matching buttonText / buttonColor /
     * buttonTextColor, and a step that is currently uploading uses the `processing` fields
     * (v1's `isShowLoader` branch). Returns blanks/nulls when config is missing so the row
     * falls back to the built-in V2 design. Nothing is substituted across statuses.
     */
    private fun stepStyle(stepConfig: StepConfig?, status: String?, isProcessing: Boolean): StepStyle {
        if (stepConfig == null) return StepStyle(null, null, null)
        val bc = stepConfig.buttonColor
        val bt = stepConfig.buttonText
        val btc = stepConfig.buttonTextColor
        val key = if (isProcessing) AppConstant.STATUS_PROCESSING else status
        val (labelRaw, fillHex, textHex) = when (key) {
            AppConstant.STATUS_APPROVED -> Triple(bt?.approved, bc?.approved, btc?.approved)
            AppConstant.STATUS_REJECTED -> Triple(bt?.rejected, bc?.rejected, btc?.rejected)
            AppConstant.STATUS_AUTOMATICALLY_REJECTED ->
                Triple(bt?.automaticallyRejected, bc?.automaticallyRejected, btc?.automaticallyRejected)
            AppConstant.STATUS_PENDING_REVIEW -> Triple(bt?.pendingReview, bc?.pendingReview, btc?.pendingReview)
            AppConstant.STATUS_PROCESSING -> Triple(bt?.processing, bc?.processing, btc?.processing)
            // NOT_UPLOADED and any unknown/null status use the not-uploaded fields, matching v1
            // (the default actionable step state).
            else -> Triple(bt?.notUploaded, bc?.notUploaded, btc?.notUploaded)
        }
        return StepStyle(
            label = labelRaw?.takeIf { it.isNotBlank() },
            fillColor = fillHex.toAmaniColorOrNull(),
            textColor = textHex.toAmaniColorOrNull()
        )
    }

    /**
     * Surfaces the server message on a step as an inline block. Any step that carries a
     * message shows it, *except* approved ones (nothing left to explain) — deliberately
     * wider than gating on the rejected-family statuses alone: the first-login rule list
     * can arrive with [Rule.errors] attached to a step whose status was already reset for
     * retry (e.g. NOT_UPLOADED after a rejection), and those must still be shown. The
     * [overrideMessage] is the AmaniEvent socket / failed-upload message, falling back to
     * the cached [Rule.errors] message at the call site. The error *title* had no
     * GeneralConfigs equivalent so it is dropped (null).
     */
    private fun errorFor(status: String?, overrideMessage: String?, version: Version?): StepError? {
        // Approved: resolved, nothing to explain. Processing: the server is re-evaluating;
        // a leftover rejection message would be stale/confusing while it runs.
        if (status == AppConstant.STATUS_APPROVED || status == AppConstant.STATUS_PROCESSING) return null
        // The block's title is the version config's rejection headline ("ID could not be
        // verified") — shown above whichever message wins below.
        val configTitle = version?.v2StepRejectionTitle?.takeIf { it.isNotBlank() }
        // Server message wins when present.
        val message = overrideMessage?.takeIf { it.isNotBlank() }
        if (message != null) return StepError(title = configTitle, message = message)
        // No backend message: a *rejected* step still explains itself with the version
        // config texts (v2StepRejectionTitle/Description), then the static fallback
        // (toggleable). Everything else without a message stays silent.
        if (!STATIC_ERROR_FALLBACK_ENABLED || status !in REJECTED_STATUSES) return null
        return StepError(
            title = configTitle ?: STATIC_ERROR_FALLBACK.title,
            message = version?.v2StepRejectionDescription?.takeIf { it.isNotBlank() }
                ?: STATIC_ERROR_FALLBACK.message
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
