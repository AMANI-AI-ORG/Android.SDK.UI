import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.Amani
import ai.amani.sdk.UploadSource
import ai.amani.sdk.model.RegisterConfig
import ai.amani.sdk.presentation.MainActivity
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

object AmaniSDKUI {

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
    }

    fun goToKycActivity(
        activity: ComponentActivity,
        idNumber: String,
        resultLauncher: ActivityResultLauncher<Intent>,
        authToken: String,
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
            }
        )
    }
}