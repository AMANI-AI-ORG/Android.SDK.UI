package ai.amani.sdk.presentation_v2.speech_verify

/**
 * Neutral, primitives-only holder for the speech-verification session.
 *
 * Deliberately references NO `ai.amani.speechverifier.*` type, so this object (and anything
 * that touches it — [AmaniSDKUI], the view model) loads fine even when the optional
 * `AmaniSpeechVerifier` artifact is absent from the app. The actual SpeechVerifier types are
 * only referenced inside [SpeechVerifierLauncher] / the host screen, which run exclusively
 * once an `ST` step is entered (and are guarded by [SpeechVerifierAvailability]).
 *
 * Populated from two places:
 *  - [serverUrl] is captured at SDK init (`AmaniSDKUI.init/configure`).
 *  - [token] is set when a KYC session starts (the profile-scoped JWT from RegisterConfig).
 *
 * Both back `SpeechVerifier.session(serverURL, token)` — required for the upload + its SSE
 * result stream. [passphrases] / [timeoutMs] are optional integrator overrides.
 */
internal object SpeechVerifierOptions {

    /** Amani API base URL — captured from `AmaniSDKUI.init/configure`. */
    @Volatile var serverUrl: String? = null

    /** Profile-scoped JWT for the current KYC session — set on `HomeKYCViewModel.start`. */
    @Volatile var token: String? = null

    /**
     * Passphrase(s) the user must read aloud. A single item is a fixed phrase; several items
     * let the module pick one at random. Overridable via
     * `AmaniSDKUI.setSpeechVerificationPassphrases(...)`; defaults to one Turkish confirmation
     * phrase since the server config carries no speech text field yet.
     */
    @Volatile var passphrases: List<String> = listOf("Onaylıyorum")

    /** Per-step time window in ms; 0 = use the module default (60s). */
    @Volatile var timeoutMs: Long = 0L

    /**
     * Minimum phonetic similarity (1..100) the transcription must reach to pass. The module
     * default is 100 (exact match); the UI SDK relaxes it to tolerate small recognition drifts.
     * Set to a value outside 1..100 to fall back to the module default.
     */
    @Volatile var matchThresholdPercent: Int = 80
}
