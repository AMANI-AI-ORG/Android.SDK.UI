package ai.amani.sdk.data.manager

import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAInitCallBack
import ai.amani.voice_assistant.callback.AmaniVAPlayerCallBack
import ai.amani.voice_assistant.model.AmaniVAVoiceKeys
import android.content.Context

internal object VoiceAssistantSDKManager {

    /**
     * Enable/Disable Voice Assistant SDK accordingly remote config value
     */
    private var voiceAssistantEnabled = false

    fun init(
        url: String?,
        callBack: AmaniVAInitCallBack
    ) {
        url?.let {
            voiceAssistantEnabled = true
            AmaniVoiceAssistant.init(
                url = url,
                callBack = callBack
            )
        }
    }

    fun play(
        context: Context,
        key: AmaniVAVoiceKeys,
        callBack: AmaniVAPlayerCallBack
    ) {
        if (!voiceAssistantEnabled) return

        AmaniVoiceAssistant.play(
            context = context,
            key = key,
            callBack = callBack
        )
    }

    fun stop() {
        if (!voiceAssistantEnabled) return

        AmaniVoiceAssistant.stop()
    }
}