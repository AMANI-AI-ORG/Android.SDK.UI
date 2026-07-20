import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.Amani
import ai.amani.sdk.DynamicFeature
import ai.amani.sdk.UploadSource
import ai.amani.sdk.model.FeatureConfig
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.model.UIStyle
import ai.amani.sdk.model.exception.AmaniException
import ai.amani.sdk.presentation.AmaniMainActivity
import ai.amani.sdk.presentation_v2.AmaniComposeActivity
import ai.amani.sdk.presentation_v2.speech_verify.SpeechVerifierOptions
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
     *
     *  @param enabledFeatures: Dynamic Features of SDK to enable
     */
    fun init(
        applicationContext: Context,
        serverURL: String,
        amaniVersion: AmaniVersion = AmaniVersion.V2,
        sharedSecret: String? = null,
        enabledFeatures: List<DynamicFeature> = DynamicFeature.allFeatures
    ){
        SpeechVerifierOptions.serverUrl = serverURL
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
     * ✅ Recommended Latest Configuration method
     *
     * Use this method to configure the SDK with all available options.
     * It replaces the will be deprecated [init] method and should be preferred
     * for all new integrations.
     *
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
     *
     *  @param uploadSource: is used to distinguish uploads from different sources.
     *  (Optional) This feature allows you to list and group uploaded data in Amani Studio based
     *  on source, or generate different statistics accordingly. Default upload source is KYC.
     *
     *  @param enabledFeatures: Dynamic Features of SDK to enable
     */
    fun configure(
        applicationContext: Context,
        serverURL: String,
        amaniVersion: AmaniVersion = AmaniVersion.V2,
        uploadSource: UploadSource = UploadSource.KYC,
        sharedSecret: String? = null,
        enabledFeatures: List<DynamicFeature> = DynamicFeature.allFeatures
    ){
        SpeechVerifierOptions.serverUrl = serverURL
        Amani.configure(
            context = applicationContext,
            server = serverURL,
            sharedSecret = sharedSecret,
            version = amaniVersion,
            uploadSource = uploadSource,
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

        val targetActivity = when (featureConfig.uiStyle) {
            UIStyle.V2 -> AmaniComposeActivity::class.java
            UIStyle.V1 -> AmaniMainActivity::class.java
        }
        val intent = Intent(activity, targetActivity)

        val bundle = Bundle().apply {
            putParcelable(AppConstant.REGISTER_CONFIG, config)
            putParcelable(AppConstant.FEATURE_CONFIG, featureConfig)
        }
        bundle.classLoader = RegisterConfig::class.java.classLoader
        intent.putExtras(bundle)
        resultLauncher.launch(intent)
    }

    /**
     * Selects the UI style used to run the KYC flow.
     *
     * Defaults to [UIStyle.V1] (the existing XML/Fragment UI) for backward
     * compatibility. Pass [UIStyle.V2] to opt into the Jetpack Compose redesign.
     * Must be called before [goToKycActivity].
     *
     * @param style: The UI style to use for the next KYC session.
     */
    fun setUIStyle(style: UIStyle) {
        featureConfig.uiStyle = style
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

    /**
     * Sets the raw video resource used in the Pose Estimation v2 preparation screen.
     * @param videoRes: Raw resource id of an MP4 video shown in the preparation screen.
     */
    fun setSelfiePoseEstimationV2PreparationVideo(@RawRes videoRes: Int) {
        featureConfig.selfiePoseEstimationV2PreparationVideo = videoRes
    }

    /** Set for Manual Capture Time Out.
     * @param timeOut: Wait seconds to activate Manual Auto Capture in case Auto Capture failure.
     */
    fun setIdCaptureManualTimeOut(timeOut: Int) {
        AppConstant.ID_CAPTURE_TIME_OUT = timeOut
    }

    /**
     * Passphrase(s) the user must read aloud in the speech-verification (`ST`) step. A single
     * item is a fixed phrase; several items let the module pick one at random per attempt.
     *
     * The speech-verification step relies on the OPTIONAL `AmaniSpeechVerifier` module, which
     * is a `compileOnly` dependency of the UI SDK — it is NOT bundled. To use `ST` steps, add
     * it to your app: `implementation 'ai.amani.android:AmaniSpeechVerifier:<version>'`.
     * Without it, entering an `ST` step throws a descriptive runtime error.
     *
     * Optional — defaults to a single Turkish confirmation phrase (the server config carries
     * no speech-text field yet). Call before [goToKycActivity].
     *
     * @param passphrases the phrase(s); blank entries are ignored.
     */
    fun setSpeechVerificationPassphrases(passphrases: List<String>) {
        passphrases.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let {
            SpeechVerifierOptions.passphrases = it
        }
    }

    /**
     * Per-step time window (milliseconds) for the speech-verification (`ST`) step. If the
     * passphrase is not matched within this window the module fails the attempt and shows its
     * retry UI. Optional — non-positive values keep the module default (60s).
     */
    fun setSpeechVerificationTimeoutMillis(millis: Long) {
        if (millis > 0L) SpeechVerifierOptions.timeoutMs = millis
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