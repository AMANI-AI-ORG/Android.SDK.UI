package ai.amani.sdk.presentation_v2.speech_verify

import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.speechverifier.SpeechVerifier
import ai.amani.speechverifier.model.ContinuationPrompts
import ai.amani.speechverifier.model.IdentityQuestionSpec
import ai.amani.speechverifier.model.IdentityQuestionType
import ai.amani.speechverifier.model.SpeechVerifierUploadError
import ai.amani.speechverifier.model.SpeechVerifierUploadResult
import ai.amani.speechverifier.model.SpokenPhrase
import ai.amani.speechverifier.model.VerificationStep
import ai.amani.speechverifier.observable.OnFailureSpeechVerifier
import ai.amani.speechverifier.observable.SpeechVerifierObserver
import ai.amani.speechverifier.observable.SpeechVerifierUploadObserver
import android.content.Context
import android.graphics.Color
import androidx.fragment.app.Fragment
import datamanager.model.config.Version
import timber.log.Timber
import androidx.core.graphics.toColorInt

/**
 * The ONE place that hard-links the optional `ai.amani.speechverifier.*` API. Because the
 * artifact is a `compileOnly` dependency, this class must only ever be loaded once the module
 * is confirmed present (guard with [SpeechVerifierAvailability]); otherwise loading it throws
 * `NoClassDefFoundError`.
 *
 * It bridges the SpeechVerifier SDK to the rest of the UI SDK with NEUTRAL callback lambdas,
 * so no caller (the host screens, the view model) has to reference SpeechVerifier types —
 * keeping the "missing module" failure contained to this file and its two entry points. The
 * [datamanager.model.config.Version] it reads comes from AmaniAi (always present).
 */
internal object SpeechVerifierLauncher {

    /** Top corner radius (dp) of the bottom-sheet panel */
    private const val SPEECH_VERIFIER_CORNER_RADIUS_DP = 28f

    /**
     * Last-resort similarity threshold, used only when neither the server config
     * (`defaultMatchThresholdPercent`) nor [SpeechVerifierOptions.matchThresholdPercent] gives
     * a value in 1..100. Matches the module's own per-item default.
     */
    private const val DEFAULT_MATCH_THRESHOLD_PERCENT = 80

    /**
     * Recording kept after an identity answer matches, so the last digit/word is captured.
     * Used when the config question carries no `successCaptureDelayMs`; matches the module's
     * own default.
     */
    private const val IDENTITY_SUCCESS_CAPTURE_DELAY_MS = 1000L

    /**
     * Builds the speech-verification [Fragment] driven by the server [version] config
     * (`speechVerification` steps/thresholds, `speechVerifierUiTexts/UiColors`,
     * `speechVerifierIdentityPrompts`, `timeoutSeconds`). Session credentials come from
     * [SpeechVerifierOptions] (captured at SDK init + KYC start). The caller commits the
     * returned fragment into its own container.
     *
     * The flow shape follows the config verbatim: a `SPOKEN_TEXT` step yields ONLY spoken
     * passphrases, an `IDENTITY_QUESTION` step yields ONLY identity questions.
     */
    fun buildFragment(
        version: Version,
        onPreparing: () -> Unit = {},
        onReady: () -> Unit = {},
        onSuccess: () -> Unit,
        onFailure: (reason: String, attempt: Int) -> Unit,
        onError: (message: String) -> Unit
    ): Fragment {
        val builder = SpeechVerifier.Builder()
            .documentType(version.type)

        val serverUrl = SpeechVerifierOptions.serverUrl
        val token = SpeechVerifierOptions.token
        if (!serverUrl.isNullOrBlank() && !token.isNullOrBlank()) {
            builder.session(serverURL = serverUrl, token = token)
        } else {
            Timber.e("V2 speech: no session (serverUrl/token missing) — upload will fail")
        }

        // Voice prompts: reuse the app config the flow ALREADY fetched (CachingHomeKYC) instead of
        // letting the module make its own config request. The `ST` step is only reachable after the
        // config is cached, so the URL is available here. voiceAssistant(...) is set explicitly in
        // both branches so the behaviour never depends on the module's own default.
        val voiceUrl = CachingHomeKYC.appConfig?.generalConfigs?.ttsVoices?.takeIf { it.isNotBlank() }
        if (voiceUrl != null) {
            builder.voiceAssistant(true)
            builder.voiceUrl(voiceUrl)
        } else {
            builder.voiceAssistant(false)
            Timber.i("V2 speech: no ttsVoices in the app config — voice prompts off")
        }

        val sv = version.speechVerification

        // Flow-level similarity threshold: server config wins, else the integrator override.
        // The module builder has no global setter, so this is resolved per item in buildSteps().
        val defaultThreshold = sv?.defaultMatchThresholdPercent?.takeIf { it in 1..100 }
            ?: SpeechVerifierOptions.matchThresholdPercent.takeIf { it in 1..100 }
            ?: DEFAULT_MATCH_THRESHOLD_PERCENT

        // Ordered flow from config: each config step maps to exactly one VerificationStep of
        // the matching type (SPOKEN_TEXT → spoken only, IDENTITY_QUESTION → identity only).
        // Falls back to a single spoken passphrase when the config has no steps.
        builder.verificationSteps(buildSteps(sv, defaultThreshold))

        sv?.exemptWords?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?.let { builder.verificationExemptWords(it) }
        sv?.let { builder.autoDetectForeignWords(it.autoDetectForeignWords) }

        // Consent-integrity + Turkish-matching options from remote config.
        sv?.let { builder.detectTurkishNegation(it.detectTurkishNegation) }
        sv?.let { builder.ignoreTurkishDiacritics(it.ignoreTurkishDiacritics) }
        sv?.rejectWords?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?.let { builder.rejectWords(it) }

        // Per-step time window: config seconds → ms, else the integrator override, else module default.
        val timeoutMs = version.timeoutSeconds.takeIf { it > 0 }?.let { it * 1000L }
            ?: SpeechVerifierOptions.timeoutMs.takeIf { it > 0L }
        timeoutMs?.let { builder.timeoutMillis(it) }

        // Rounded bottom-sheet top corners, matching the Core SDK sample (28dp).
        builder.bottomSheetCornerRadius(SPEECH_VERIFIER_CORNER_RADIUS_DP)

        // Identity-question prompts (config → module's ContinuationPrompts; blanks keep defaults).
        version.speechVerifierIdentityPrompts?.let { p ->
            val d = ContinuationPrompts()
            builder.continuationPrompts(
                ContinuationPrompts(
                    instruction = d.instruction,
                    idNumber = p.idNumber.ifBlank { d.idNumber },
                    motherName = p.motherName.ifBlank { d.motherName },
                    fatherName = p.fatherName.ifBlank { d.fatherName },
                    documentNumber = p.documentNumber.ifBlank { d.documentNumber }
                )
            )
        }

        // On-camera texts (blank → module default via null). The instruction falls back to the
        // step's captureDescription so a config that only fills the generic step texts still
        // drives the on-screen line.
        val stepInstruction = version.steps?.firstOrNull()?.captureDescription?.orNull()
        version.speechVerifierUiTexts?.let { t ->
            builder.userInterfaceTexts(
                instruction = t.instruction.orNull() ?: stepInstruction,
                listening = t.listening.orNull(),
                verifying = t.verifying.orNull(),
                verified = t.verified.orNull(),
                failed = t.failed.orNull(),
                recognizerNotAvailable = t.recognizerNotAvailable.orNull(),
                retry = t.retry.orNull()
            )
        }

        // No speechVerifierUiTexts block at all: still honour the step's captureDescription.
        if (version.speechVerifierUiTexts == null && stepInstruction != null) {
            builder.userInterfaceTexts(instruction = stepInstruction)
        }

        // Colors (hex → color int; unparseable/blank → module default via null). These are raw
        // @ColorInt ARGB values parsed from the remote config's hex strings, so use the
        // colour-INT setter (userInterfaceColorInts) — NOT userInterfaceColors, which expects
        // @ColorRes resource ids and would treat a hex value as a (missing) resource id and crash.
        version.speechVerifierUiColors?.let { c ->
            builder.userInterfaceColorInts(
                overlayBackgroundColor = c.overlayBackgroundColor.toColorIntOrNull(),
                scrimColor = c.scrimColor.toColorIntOrNull(),
                speechTextColor = c.speechTextColor.toColorIntOrNull(),
                speechTextHighlightColor = c.speechTextHighlightColor.toColorIntOrNull(),
                instructionTextColor = c.instructionTextColor.toColorIntOrNull(),
                statusTextColor = c.statusTextColor.toColorIntOrNull(),
                micActiveColor = c.micActiveColor.toColorIntOrNull(),
                micIdleColor = c.micIdleColor.toColorIntOrNull(),
                resultSuccessColor = c.resultSuccessColor.toColorIntOrNull(),
                resultErrorColor = c.resultErrorColor.toColorIntOrNull(),
                failedTextColor = c.failedTextColor.toColorIntOrNull(),
                retryButtonTextColor = c.retryButtonTextColor.toColorIntOrNull(),
                retryButtonBackgroundColor = c.retryButtonBackgroundColor.toColorIntOrNull()
            )
        }

        return builder
            .observe(object : SpeechVerifierObserver {
                override fun onPreparing() = onPreparing()
                override fun onReady() = onReady()
                override fun onSuccess() = onSuccess()
                override fun onFailure(reason: OnFailureSpeechVerifier, currentAttempt: Int) =
                    onFailure(reason.name, currentAttempt)
                override fun onError(error: Error) = onError(error.message ?: "Speech verifier error")
            })
            .build()
    }

    /**
     * Maps the config steps into the module's ordered flow. A `SPOKEN_TEXT` step contributes
     * ONLY spoken passphrases; an `IDENTITY_QUESTION` step ONLY identity questions.
     *
     * Threshold resolution, most specific first: the item's own `matchThresholdPercent`, then
     * the step's, then [defaultThreshold] (config `defaultMatchThresholdPercent` → integrator
     * override → [DEFAULT_MATCH_THRESHOLD_PERCENT]). Unknown step/question types and empty
     * steps are skipped; an entirely empty result falls back to one passphrase.
     */
    private fun buildSteps(
        sv: datamanager.model.config.SpeechVerification?,
        defaultThreshold: Int
    ): List<VerificationStep> {
        val steps = sv?.steps.orEmpty().mapNotNull { step ->
            when (step.type.trim().uppercase()) {
                "SPOKEN_TEXT" -> {
                    val phrases = step.texts
                        .filter { it.text.isNotBlank() }
                        .map {
                            SpokenPhrase(
                                text = it.text,
                                thresholdPercent = it.matchThresholdPercent.orThreshold(
                                    step.matchThresholdPercent, defaultThreshold
                                ),
                                successCaptureDelayMs = it.successCaptureDelayMs  // per-phrase
                            )
                        }
                    phrases.takeIf { it.isNotEmpty() }?.let { VerificationStep.SpokenText(it) }
                }
                "IDENTITY_QUESTION" -> {
                    val questions = step.questions.mapNotNull { q ->
                        runCatching { IdentityQuestionType.valueOf(q.type.trim().uppercase()) }.getOrNull()
                            ?.let {
                                IdentityQuestionSpec(
                                    type = it,
                                    thresholdPercent = q.matchThresholdPercent.orThreshold(
                                        step.matchThresholdPercent, defaultThreshold
                                    ),
                                    // per-question; null keeps the module's own capture delay
                                    successCaptureDelayMs = q.successCaptureDelayMs
                                        ?: IDENTITY_SUCCESS_CAPTURE_DELAY_MS
                                )
                            }
                    }
                    questions.takeIf { it.isNotEmpty() }?.let { VerificationStep.IdentityQuestion(it) }
                }
                else -> null
            }
        }
        if (steps.isNotEmpty()) return steps

        // No usable config → fall back to the integrator passphrases (or one default phrase).
        val phrases = SpeechVerifierOptions.passphrases.filter { it.isNotBlank() }
            .ifEmpty { listOf("Onaylıyorum") }
            .map { SpokenPhrase(text = it, thresholdPercent = defaultThreshold) }
        return listOf(VerificationStep.SpokenText(phrases))
    }

    /** Item threshold → step threshold → flow default; out-of-range values are ignored. */
    private fun Int?.orThreshold(stepThreshold: Int?, defaultThreshold: Int): Int =
        this?.takeIf { it in 1..100 }
            ?: stepThreshold?.takeIf { it in 1..100 }
            ?: defaultThreshold

    /**
     * Uploads the recorded video of the just-completed session (call after [onSuccess]) and
     * reports the backend's step verdict through neutral callbacks.
     */
    fun upload(
        context: Context,
        onResult: (stepStatus: String, documentId: String?) -> Unit,
        onError: (code: Int, message: String) -> Unit
    ) {
        SpeechVerifier.upload(
            context = context,
            observer = object : SpeechVerifierUploadObserver {
                override fun onResult(result: SpeechVerifierUploadResult) =
                    onResult(result.stepStatus, result.documentId)

                override fun onError(error: SpeechVerifierUploadError, message: String) =
                    onError(error.code, message)
            }
        )
    }

    private fun String?.orNull(): String? = this?.takeIf { it.isNotBlank() }

    private fun String?.toColorIntOrNull(): Int? =
        this?.takeIf { it.isNotBlank() }?.let { runCatching { it.toColorInt() }.getOrNull() }
}
