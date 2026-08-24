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
import datamanager.model.config.GeneralConfigs
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

    /** Config-driven step duration, used when the version carries no `v2EstimatedTime`. */
    private fun defaultStepDuration(g: GeneralConfigs?): String =
        g?.v2StepDefaultDuration.orFallback(STATIC_STEP_DURATION)

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
        // Step whose dot reads as "current": the first one still needing the user and not
        // locked behind an unmet mandatory dependency.
        val activeIndex = activeRuleIndex(rules, config, statusOverrides)

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
            // Per-step server styling, selected by the rule's status with a "processing"
            // override while uploading.
            val stepConfig = config?.stepConfigForRule(rule)
            val style = stepStyle(stepConfig, status, isProcessing)
            // Gating is per step (v1 KYCAdapter parity): a step is selectable when its OWN
            // `mandatoryStepIDs` are satisfied. An upload elsewhere in the flow does not lock
            // it — only the uploading step itself is busy.
            val isActive = isUnlocked(rule, rules, config, statusOverrides)
            val rowStatus = if (isProcessing) StepRowStatus.Processing
            else rowStatus(status, isActive)
            // The step's version config carries the v2 per-document strings (estimated
            // time, rejection fallback texts).
            val version = config?.firstVisibleVersionFor(rule)
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
                        "$actionLabel · ${version?.v2EstimatedTime.orFallback(defaultStepDuration(g))}"
                    else -> style.label
                },
                status = rowStatus,
                // Config pair for the status: buttonColor + buttonTextColor (see StepRow).
                fillColor = style.fillColor,
                textColor = style.textColor,
                error = if (isProcessing) null else errorFor(
                    status = status,
                    // Fresh socket / upload message wins; otherwise fall back to the
                    // message carried by the step itself (initially-rejected steps) —
                    // first non-blank across the rule's errors.
                    overrideMessage = errorOverrides[rule.id]
                        ?: rule.errors?.firstNotNullOfOrNull {
                            it?.errorMessage?.toString()?.takeIf(String::isNotBlank)
                        },
                    version = version,
                    general = g
                )
            )
        }

        // Home heading follows the flow state (design v2.6): rejected wins, then progress
        // (something already done), then the fresh-start heading. A config subtitle carrying
        // "{count}" is a full sentence and is used verbatim with the number substituted —
        // the only form that survives translation. Legacy configs stay sentence *suffixes*,
        // with the English step-count prefix composed here.
        val remaining = rules.size - doneCount
        val (title, subtitle) = when {
            rejectedCount > 0 -> Pair(
                g?.v2HomeRejectedTitle.orFallback("Verification incomplete"),
                subtitleFor(
                    template = g?.v2HomeRejectedSubtitle,
                    count = rejectedCount,
                    legacyPrefix = if (rejectedCount == 1) " step needs " else " steps need ",
                    legacyFallback = "your attention before we can continue."
                )
            )
            doneCount > 0 -> Pair(
                g?.v2HomeProgressTitle.orFallback("You're making progress"),
                subtitleFor(
                    template = g?.v2HomeProgressSubtitle,
                    count = remaining,
                    legacyPrefix = if (remaining == 1) " more step " else " more steps ",
                    legacyFallback = "to finish verification."
                )
            )
            else -> Pair(
                g?.v2HomeInitialTitle.orFallback("Let's get you verified"),
                subtitleFor(
                    template = g?.v2HomeInitialSubtitle,
                    count = rules.size,
                    legacyPrefix = " ",
                    legacyFallback = "quick steps. Should take about 2 minutes."
                )
            )
        }

        return HomeKYCUiState(
            // Config-driven: GeneralConfigs.mainTitleText (v1 toolbar title), fallback when blank/null.
            headerTitle = g?.mainTitleText.orFallback("Verification"),
            dots = dots,
            title = title,
            subtitle = subtitle,
            steps = steps
        )
    }

    /**
     * Home subtitle for [count] steps. A [template] containing `{count}` is a complete,
     * translatable sentence: the placeholder becomes the numeral and nothing else is added.
     * Otherwise the legacy shape is kept — spelled-out count + [legacyPrefix] + the config
     * suffix (or [legacyFallback] when the config has none).
     */
    private fun subtitleFor(
        template: String?,
        count: Int,
        legacyPrefix: String,
        legacyFallback: String
    ): String {
        val text = template?.takeIf { it.isNotBlank() }
        if (text != null && text.contains(COUNT_PLACEHOLDER)) {
            return text.replace(COUNT_PLACEHOLDER, count.toString())
        }
        return countWord(count) + legacyPrefix + (text ?: legacyFallback)
    }

    /** Placeholder a config subtitle uses to position the step count. */
    private const val COUNT_PLACEHOLDER = "{count}"

    /**
     * Spelled-out step count for the LEGACY home subtitles ("Four quick steps…", "One step
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
    private fun isUnlocked(
        rule: Rule,
        rules: List<Rule>,
        config: ResGetConfig?,
        statusOverrides: Map<String, String>
    ): Boolean {
        val mandatoryIds = config?.stepConfigForRule(rule)?.mandatoryStepIDs
        if (mandatoryIds.isNullOrEmpty()) return true
        return mandatoryIds.all { mId ->
            val depStatus = rules.firstOrNull { it.id == mId }?.let { effectiveStatus(it, statusOverrides) }
            depStatus == AppConstant.STATUS_APPROVED || depStatus == AppConstant.STATUS_PENDING_REVIEW
        }
    }

    /**
     * Index of the active step: the first one still needing the user — not done and not
     * locked behind an unmet mandatory dependency. `-1` when nothing is actionable.
     */
    private fun activeRuleIndex(
        rules: List<Rule>,
        config: ResGetConfig?,
        statusOverrides: Map<String, String>
    ): Int {
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
     * Resolves the [StepConfig] backing [rule] by id. Deliberately NOT by `sortOrder` position
     * (v1's `ResGetConfig.getStepConfig`): a profile whose rules start at sortOrder 0 — every
     * profile with a before-KYC step does — shifted every row onto the previous step's config,
     * so the first KYC row rendered the profile_info step's texts and colors.
     */
    private fun ResGetConfig.stepConfigForRule(rule: Rule): StepConfig? =
        stepConfigs?.firstOrNull { it.id != null && it.id == rule.id }

    /**
     * First selectable (non-hidden) [Version] of [rule]'s step — the carrier of the per-document
     * v2 strings (`v2EstimatedTime`, `v2StepRejection*`). Read-only: unlike
     * CaptureFlow.prepareVersions it stamps nothing onto the version objects.
     */
    private fun ResGetConfig.firstVisibleVersionFor(rule: Rule): Version? {
        val documents = stepConfigForRule(rule)?.mDocuments ?: return null
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
    private fun errorFor(
        status: String?,
        overrideMessage: String?,
        version: Version?,
        general: GeneralConfigs?
    ): StepError? {
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
            title = configTitle
                ?: general?.v2StepRejectionFallbackTitle?.takeIf { it.isNotBlank() }
                ?: STATIC_ERROR_FALLBACK.title,
            message = version?.v2StepRejectionDescription?.takeIf { it.isNotBlank() }
                ?: general?.v2StepRejectionFallbackDescription
                    .orFallback(STATIC_ERROR_FALLBACK.message)
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
