import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.Amani
import ai.amani.sdk.DynamicFeature
import ai.amani.sdk.UploadSource
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.model.exception.AmaniException
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.utils.AppConstant
import ai.amani.voice_assistant.AmaniVoiceAssistant
import ai.amani.voice_assistant.callback.AmaniVAInitCallBack
import ai.amani.voice_assistant.model.TTSVoice
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RawRes
import java.io.InputStream
import kotlin.jvm.Throws

object AmaniSDKUI {

    private var featureConfig = FeatureConfig()

    /**
     *  First init of the Amani SDK, best practice to call it in Application class while app is
     *  running up.
     *
     *  @param activity: Current activity.
     *
     *  @param serverURL: Base URL of the server you have. (format example: https://www.server_url.com)
     *
     *  @param amaniVersion: Version of the Amani Service. The default is AmaniVersion.V2.
     *
     *  @param sharedSecret: Extra security layer. Non mandatory.
     */
    fun init(
        applicationContext: Context,
        serverURL: String,
        amaniVersion: AmaniVersion = AmaniVersion.V2,
        sharedSecret: String? = null,
        enabledFeatures: List<DynamicFeature> = DynamicFeature.allFeatures
    ){
        Amani.init(
            context = applicationContext,
            server = serverURL,
            sharedSecret = sharedSecret,
            version = amaniVersion,
            uploadSource = UploadSource.KYC,
            enabledFeatures = enabledFeatures
        )

    }

    /**
     * Starts the KYC activity directly.
     *
     * @param activity: Current activity.
     *
     * @param idNumber: A unique ID of the created profile.
     *
     * @param resultLauncher: To launch tke KYC activity and get the result of the KYC.
     *
     * @param authToken: Auth Token for relevant profile.
     *
     * @param language: KYC config language for screen texts default is "tr", non mandatory.
     *
     * @param birthDate: birthDate data of the user to scan NFC of the user, it is required in case
     * to scan NFC with manual setting those NVI data group(birthDate, expireDate, documentNumber),
     * non mandatory.
     *
     * @param expireDate: expireDate data of the ID Card to scan NFC of the user, it is required in
     * case to scan NFC with manual setting those NVI data group(birthDate, expireDate, documentNumber),
     * non mandatory.
     *
     * @param documentNumber: documentNumber data of the ID Card to scan NFC of the user, it is
     * required in case to scan NFC with manual setting those NVI data group(birthDate, expireDate,
     * documentNumber), non mandatory.
     *
     * @param userEmail: Extra information data of the current user to upload Amani studio, non
     * mandatory
     *
     * @param userPhoneNumber: Extra information data of the current user to upload Amani studio,
     * non mandatory,
     *
     * @param userFullName: Extra information data of the current user to upload Amani studio,
     * non mandatory,
     */
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

    /**
     * Enables hologram check while scanning ID Card for extra security.
     * @param hologramDetection: Whether enable/disable Check hologram.
     */
    fun setHologramDetection(hologramDetection: Boolean?) {
        hologramDetection?.let {
            featureConfig.idCaptureHologramDetection = it
        }
    }

    /**
     * Enables video recording in Selfie Capture session.
     * @param selfieVideoRecord: Whether enable/disable VideoRecord.
     */
    fun setSelfieCaptureVideoRecord(selfieVideoRecord: Boolean?) {
        selfieVideoRecord?.let {
            featureConfig.selfieCaptureVideoRecord = it
        }
    }

    /**
     * Enables video recording in ID Capture session.
     * @param idCaptureVideoRecord: Whether enable/disable VideoRecord.
     */
    fun setIdCaptureVideoRecord(idCaptureVideoRecord: Boolean? = true) {
        idCaptureVideoRecord?.let {
            featureConfig.idCaptureVideoRecord = it
        }
    }

    /** Set for Manual Capture Time Out.
     * @param timeOut: Wait seconds to activate Manual Auto Capture in case Auto Capture failure.
     */
    fun setIdCaptureManualTimeOut(timeOut: Int) {
        AppConstant.ID_CAPTURE_TIME_OUT = timeOut
    }

    /**
     * WARNING: It should be called before the first init() function!
     *
     * Enables the SSL Pinning with the certificate that taken from raw/ssl.cert folder of your
     * application.
     *
     * @param context: Current activity pointer.
     *
     * @param certificate: Raw res id of the SSL pinning certificate.
     *
     * @throws AmaniException if invalid certificate or file is given.
     *
     */
    @Throws(AmaniException::class)
    fun setSSLPinning(
        context: Context,
        @RawRes certificate: Int
        ){
        Amani.setSSLPinning(
            context,
            certificate
        )
    }

    /**
     * WARNING: It should be called before the first init() function!
     *
     * Enables the SSL Pinning with the certificate that taken from raw/ssl.cert folder of your
     * application.
     *
     * @param certificate: SSL Pinning certificate as InputStream.
     * @throws AmaniException if invalid certificate or file is given.
     */
    @Throws(AmaniException::class)
    fun setSSLPinning(
        certificate: InputStream
    ){
        Amani.setSSLPinning(
            certificate
        )
    }
}