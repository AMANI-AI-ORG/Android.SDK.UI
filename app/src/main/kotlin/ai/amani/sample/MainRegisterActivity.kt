package ai.amani.sample

import AmaniSDKUI
import ai.amani.base.utility.AmaniVersion
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

        AmaniSDKUI.init(
            activity = this,
            serverURL = TestCredentials.SERVER_URL,
            amaniVersion = AmaniVersion.V2,
            sharedSecret = null
        )

        registerButton.setOnClickListener {

            AmaniSDKUI.setHologramDetection(false)
            AmaniSDKUI.setIdCaptureVideoRecord(false)
            AmaniSDKUI.setSelfieCaptureVideoRecord(false)

            hideKeyboard()

            if (inputIdLabel.text.isNullOrEmpty() && inputIdLabel.text.isNullOrBlank()) {
                Toast.makeText(this, "Please enter your ID", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            AmaniSDKUI.goToKycActivity(
                activity = this,
                resultLauncher = resultLauncher,
                idNumber = inputIdLabel.text.toString(),
                authToken = TestCredentials.TOKEN,
                language = "tr",
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

