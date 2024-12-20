import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.Amani
import ai.amani.sdk.UploadSource
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.utils.AppConstant
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAInitCallBack
import ai.amani.voice_assistant.model.TTSVoice
import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

object AmaniSDKUI {

    private var featureConfig = FeatureConfig()

    fun init(
        activity: Activity,
        serverURL: String,
        amaniVersion: AmaniVersion = AmaniVersion.V2,
        sharedSecret: String? = null
    ){
        Amani.init(
            activity,
            serverURL,
            sharedSecret,
            amaniVersion,
            UploadSource.KYC
        )

        //TODO change static URL with dynamic 
        AmaniVoiceAssistant.init(
            url = "https://gist.githubusercontent.com/munir-amani/70bbb480b1ea8b761169397004a37a4d/raw/9ad200113d016db661905de94dd822f9609c9623/ttsVoices.json",
            callBack = object : AmaniVAInitCallBack {
                override fun onSuccess(voices: List<TTSVoice>) {

                }

                override fun onFailure(exception: Exception) {

                }
            }
        )
    }

    fun goToKycActivity(
        activity: Activity,
        idNumber: String,
        resultLauncher: ActivityResultLauncher<Intent>,
        authToken: String? = null,
        language: String = "tr",
        geoLocation: Boolean = true,
        birthDate: String? = null,
        expireDate: String? = null,
        documentNumber: String? = null,
        userEmail: String? = null,
        userPhoneNumber: String? = null,
        userFullName: String? = null
    ){
        val config = RegisterConfig(
            tcNumber = idNumber,
            token = authToken,
            language = language,
            location = geoLocation,
            userEmail = userEmail,
            userFullName = userFullName,
            userPhoneNumber = userPhoneNumber,
            birthDate = birthDate,
            expireDate = expireDate,
            documentNumber = documentNumber,
        )

        resultLauncher.launch(
            Intent(activity, MainActivity::class.java).apply {
                this.putExtra(AppConstant.REGISTER_CONFIG, config)
                this.putExtra(AppConstant.FEATURE_CONFIG, featureConfig)
            }
        )
    }

    fun setHologramDetection(hologramDetection: Boolean?) {
        hologramDetection?.let {
            featureConfig.idCaptureHologramDetection = it
        }
    }

    fun setSelfieCaptureVideoRecord(selfieVideoRecord: Boolean?) {
        selfieVideoRecord?.let {
            featureConfig.selfieCaptureVideoRecord = it
        }
    }

    fun setIdCaptureVideoRecord(idCaptureVideoRecord: Boolean?) {
        idCaptureVideoRecord?.let {
            featureConfig.idCaptureVideoRecord = it
        }
    }
}