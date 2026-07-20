package ai.amani.sdk.presentation_v2.speech_verify

/**
 * Presence check for the optional `AmaniSpeechVerifier` artifact.
 *
 * The UI SDK depends on the speech verifier as `compileOnly`, so it is NOT bundled: an
 * integrator who wants the speech-verification (`ST`) step must add
 * `implementation 'ai.amani.android:AmaniSpeechVerifier:<version>'` themselves. If they don't,
 * the classes are missing at runtime.
 *
 * This object references the module ONLY by string name (via [Class.forName]), so it never
 * hard-links any `ai.amani.speechverifier.*` type — it can be safely loaded and called even
 * when the artifact is absent. It is the gate the `ST` flow checks before touching
 * [SpeechVerifierLauncher] / the host screen (which DO reference those types).
 */
internal object SpeechVerifierAvailability {

    private const val ENTRY_POINT = "ai.amani.speechverifier.SpeechVerifier"

    /** True when the speech-verifier artifact is on the runtime classpath. */
    fun isPresent(): Boolean = runCatching {
        Class.forName(ENTRY_POINT, false, this::class.java.classLoader)
    }.isSuccess

    /**
     * Throws a descriptive [IllegalStateException] when the artifact is missing — the
     * deliberate, actionable failure for an app that configured an `ST` step but did not add
     * the optional dependency (instead of an opaque `NoClassDefFoundError` deep in the flow).
     */
    fun requirePresent() {
        if (!isPresent()) {
            throw IllegalStateException(
                "This KYC flow contains a speech-verification (ST) step, but the optional " +
                    "AmaniSpeechVerifier module is not on the classpath. Add it to your app:\n" +
                    "    implementation 'ai.amani.android:AmaniSpeechVerifier:<version>'\n" +
                    "The Amani UI SDK depends on it as compileOnly, so it is not bundled " +
                    "automatically."
            )
        }
    }
}
