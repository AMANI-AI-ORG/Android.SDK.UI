package ai.amani.sample

import AmaniSDKUI
import ai.amani.BuildConfig
import ai.amani.base.utility.AmaniVersion
import ai.amani.sdk.DynamicFeature
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.junit.Test
import java.util.Properties

/**
 *  Basic Sample Activity to launch amani-sdk module
 */
class MainRegisterActivity : AppCompatActivity() {

    private val registerButton: Button by lazy { findViewById(R.id.btn) }
    private val inputIdLabel: EditText by lazy { findViewById(R.id.input_id_number) }
    private val inputBirthDateLabel: EditText by lazy { findViewById(R.id.input_birth_date) }
    private val inputExpiryDateLabel: EditText by lazy { findViewById(R.id.input_expiry_date) }
    private val inputDocumentNumberLabel: EditText by lazy { findViewById(R.id.input_doc_number) }


    private val resultLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            data?.let {
                val kycResult: KYCResult? = it.parcelable(AppConstant.KYC_RESULT)
                Toast.makeText(this, "Kyc result: ${kycResult!!.profileStatus}" , Toast.LENGTH_LONG).show()
            }?: let {
                Toast.makeText(this, "Data is null", Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_main)

        //To activate SSL pinning with raw file
        //AmaniSDKUI.setSSLPinning(context = this, certificate = R.raw.certifica)

        //To activate SSL pinning with InputStream
        //val inputStream = this.resources.openRawResource(R.raw.certifica)
        //AmaniSDKUI.setSSLPinning(inputStream)

        /* Dynamic Feature usages samples, select one configuration accordingly your needs. :
        //Enables Auto Selfie Capture && ID Capture
        val enabledFeatures1 = listOf(
            DynamicFeature.SELFIE_AUTO,
            DynamicFeature.ID_CAPTURE
        )

        //Enables Pose Estimation && ID Capture && NFC Scan
        val enabledFeatures2 = listOf(
            DynamicFeature.SELFIE_POSE_ESTIMATION,
            DynamicFeature.ID_CAPTURE,
            DynamicFeature.NFC_SCAN
        )

        //Enables all features
        val enabledFeatures3 = DynamicFeature.allFeatures
        */

        registerButton.setOnClickListener {

            AmaniSDKUI.setHologramDetection(false)
            AmaniSDKUI.setIdCaptureVideoRecord(false)
            AmaniSDKUI.setSelfieCaptureVideoRecord(false)

            hideKeyboard()

            AmaniSDKUI.goToKycActivity(
                activity = this,
                resultLauncher = resultLauncher,
                idNumber = inputIdLabel.text.toString(),
                authToken = "TestCredentials.TOKEN",
                language = "en",
                geoLocation = true,
                birthDate = inputBirthDateLabel.text.toString(),
                expireDate = inputExpiryDateLabel.text.toString(),
                documentNumber = inputDocumentNumberLabel.text.toString(),
                userEmail = "",
                userPhoneNumber = ""
            )
        }
    }
}

