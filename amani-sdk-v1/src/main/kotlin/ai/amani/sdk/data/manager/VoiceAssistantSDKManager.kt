package ai.amani.sdk.data.manager

import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAInitCallBack
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import ai.amani.voice_assistant.model.AmaniVAVoiceKeys
import ai.amani.voice_assistant.model.TTSVoice
import android.content.Context
import timber.log.Timber

/**
 * Voice prompt keys used by the UI SDK screens. Deliberately dependency-free so call sites never
 * touch `ai.amani.voice_assistant.*` types — see [VoiceAssistantSDKManager]. Names must match the
 * SDK's own `AmaniVAVoiceKeys` entries.
 */
internal enum class UiVoiceKey {
    VOICE_ID0,
    VOICE_ID1,
    VOICE_SE0,
    VOICE_SE1,
    VOICE_NFC,
    VOICE_SUCCESS
}

/**
 * The UI SDK's single entry point to the optional Amani Voice Assistant (TTS) SDK.
 *
 * The artifact is a **`compileOnly`** dependency (see `amani-sdk-v1/build.gradle`): the UI SDK
 * compiles against it but does not bundle it, so apps that never want voice prompts carry none of
 * its weight. Consequently the classes may be **absent at runtime**, so:
 *
 *  - this is the ONLY file allowed to reference `ai.amani.voice_assistant.*` — the public API here
 *    speaks in [UiVoiceKey] / primitives, so call sites never resolve a missing class;
 *  - every call is guarded by [isSdkPresent] and wrapped in `runCatching` (which also swallows
 *    `NoClassDefFoundError`), so a missing SDK degrades to a **silent no-op** instead of crashing
 *    the KYC flow.
 */
internal object VoiceAssistantSDKManager {

    /**
     * Enable/Disable Voice Assistant SDK accordingly remote config value — set by [init] once a
     * voice URL arrives from the app config.
     */
    private var voiceAssistantEnabled = false

    fun init(url: String?) {
        if (url.isNullOrBlank()) return
        if (!isSdkPresent()) {
            Timber.w(
                "Voice Assistant SDK is not on the classpath — voice prompts disabled. Add " +
                        "'ai.amani:Android.SDK.AmaniVoiceAssistant' to the app to enable them."
            )
            return
        }
        runCatching {
            AmaniVoiceAssistant.init(
                url = url,
                callBack = object : AmaniVAInitCallBack {
                    override fun onSuccess(voices: List<TTSVoice>) {
                        voiceAssistantEnabled = true
                        Timber.i("Voice Assistant ready (${voices.size} voices)")
                    }

                    override fun onFailure(exception: Exception) {
                        Timber.w("Voice Assistant init failed: ${exception.message}")
                    }
                }
            )
        }.onFailure { Timber.w("Voice Assistant init skipped: ${it.javaClass.simpleName}") }
    }

    fun play(context: Context, key: UiVoiceKey) {
        if (!voiceAssistantEnabled) return
        runCatching {
            AmaniVoiceAssistant.play(
                context = context,
                key = AmaniVAVoiceKeys.valueOf(key.name),
                callBack = object : AmaniVAPlayerCallBack {
                    override fun onPlay() {}

                    override fun onStop() {}

                    override fun onFailure(exception: Exception) {
                        Timber.w("Voice Assistant play failed for ${key.name}: ${exception.message}")
                    }
                }
            )
        }.onFailure { Timber.w("Voice Assistant play skipped: ${it.javaClass.simpleName}") }
    }

    fun stop() {
        if (!voiceAssistantEnabled) return
        runCatching { AmaniVoiceAssistant.stop() }
    }

    private const val ENTRY_POINT = "ai.amani.voice_assistant.AmaniVoiceAssistant"

    /**
     * Whether the optional Voice Assistant artifact is on the RUNTIME classpath. Resolved by name
     * so this check itself never hard-links the class (safe to call when it is absent).
     */
    private val sdkPresent: Boolean by lazy {
        runCatching {
            Class.forName(ENTRY_POINT, false, this::class.java.classLoader)
        }.isSuccess
    }

    private fun isSdkPresent(): Boolean = sdkPresent
}
