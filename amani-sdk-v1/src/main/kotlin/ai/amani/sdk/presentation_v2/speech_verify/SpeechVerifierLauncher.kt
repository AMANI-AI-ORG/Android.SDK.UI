package ai.amani.sdk.presentation_v2.speech_verify

import ai.amani.speechverifier.SpeechVerifier
import ai.amani.speechverifier.model.IdentityQuestionType
import ai.amani.speechverifier.model.SpeechVerifierUploadError
import ai.amani.speechverifier.model.SpeechVerifierUploadResult
import ai.amani.speechverifier.model.VerificationStep
import ai.amani.speechverifier.observable.OnFailureSpeechVerifier
import ai.amani.speechverifier.observable.SpeechVerifierObserver
import ai.amani.speechverifier.observable.SpeechVerifierUploadObserver
import android.content.Context
import androidx.fragment.app.Fragment
import timber.log.Timber

/**
 * The ONE place that hard-links the optional `ai.amani.speechverifier.*` API. Because the
 * artifact is a `compileOnly` dependency, this class must only ever be loaded once the module
 * is confirmed present (guard with [SpeechVerifierAvailability]); otherwise loading it throws
 * `NoClassDefFoundError`.
 *
 * It bridges the SpeechVerifier SDK to the rest of the V2 UI with NEUTRAL callback lambdas, so
 * no caller (the host screen, the view model) has to reference SpeechVerifier types — keeping
 * the "missing module" failure contained to this file and its two entry points.
 */
internal object SpeechVerifierLauncher {

    /**
     * Builds the speech-verification [Fragment] for [docType], wired to plain callbacks.
     * Session credentials + passphrases come from [SpeechVerifierOptions] (populated at SDK
     * init and KYC start). The caller commits the returned fragment into its own container.
     *
     * @param onPreparing a blocking preparation step started (fires only when the flow needs a
     *   backend fetch, e.g. identity questions) — show a loader.
     * @param onReady the screen is live (pairs with [onPreparing]) — hide the loader.
     * @param onSuccess all steps passed; the session video is recorded + secured. Follow with
     *   [upload].
     * @param onFailure a single attempt failed (the module shows its own retry UI); carries the
     *   failure reason name + attempt count.
     * @param onError a non-recoverable error (recognizer/recording).
     */
    fun buildFragment(
        docType: String,
        onPreparing: () -> Unit = {},
        onReady: () -> Unit = {},
        onSuccess: () -> Unit,
        onFailure: (reason: String, attempt: Int) -> Unit,
        onError: (message: String) -> Unit
    ): Fragment {
        val builder = SpeechVerifier.Builder()
            .documentType(docType)

        val serverUrl = SpeechVerifierOptions.serverUrl
        val token = SpeechVerifierOptions.token
        if (!serverUrl.isNullOrBlank() && !token.isNullOrBlank()) {
            builder.session(serverURL = serverUrl, token = token)
        } else {
            Timber.e("V2 speech: no session (serverUrl/token missing) — upload will fail")
        }

        // Unified ordered flow (replaces expectedRandomTexts/speechText): one spoken-passphrase
        // step whose pool is the configured phrases — the module picks one at random per attempt.
        val phrases = SpeechVerifierOptions.passphrases
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Onaylıyorum") }
        builder.verificationSteps(
            listOf(VerificationStep.SpokenText(phrases),
                VerificationStep.IdentityQuestion(
                    listOf(
                        IdentityQuestionType.DOCUMENT_NUMBER,
                        IdentityQuestionType.FATHER_NAME,
                        IdentityQuestionType.MOTHER_NAME,
                        IdentityQuestionType.ID_NUMBER
                    )
                )
            )
        )

        SpeechVerifierOptions.matchThresholdPercent
            .takeIf { it in 1..100 }
            ?.let { builder.matchThresholdPercent(it) }

        if (SpeechVerifierOptions.timeoutMs > 0L) {
            builder.timeoutMillis(SpeechVerifierOptions.timeoutMs)
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
     * Uploads the recorded video of the just-completed session (call after [onSuccess]) and
     * reports the backend's step verdict through neutral callbacks. Uses the session
     * credentials the fragment was built with.
     *
     * @param onResult backend step status (e.g. APPROVED/PENDING_REVIEW/REJECTED) + optional doc id.
     * @param onError upload/stream failure: error code + message.
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
}
