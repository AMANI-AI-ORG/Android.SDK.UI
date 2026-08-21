package ai.amani.sample.presentation.entry

import AmaniSDKUI
import ai.amani.base.utility.AmaniVersion
import ai.amani.sample.R
import ai.amani.sample.domain.model.ProfileUrlInfo
import ai.amani.sample.parcelable
import ai.amani.sample.presentation.scan.QrScanActivity
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.model.UIStyle
import ai.amani.sdk.utils.AppConstant
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/**
 * Entry point (English only): a Compose reproduction of the verify app's register screen. All
 * QR-resolution logic lives in [QrEntryViewModel]; this Activity keeps only the platform bits —
 * launching the scanner and starting the Amani UI SDK.
 */
class QrEntryActivity : AppCompatActivity() {

    private val viewModel: QrEntryViewModel by viewModels { QrEntryViewModel.Factory() }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val text = result.data?.getStringExtra(QrScanActivity.EXTRA_QR_TEXT)
                if (text.isNullOrBlank()) {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.onQrScanned(text)
                }
            }
        }

    private val kycLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val kycResult: KYCResult? = result.data?.parcelable(AppConstant.KYC_RESULT)
                Toast.makeText(this, "Kyc result: ${kycResult?.profileStatus}", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is QrEntryEvent.StartKyc ->
                            startKyc(event.info, event.language, event.uiStyle)
                        is QrEntryEvent.Error ->
                            Toast.makeText(this@QrEntryActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RegisterScreen(
                countries = state.countries,
                selected = state.selected,
                uiStyle = state.uiStyle,
                onCountrySelected = viewModel::onCountrySelected,
                onUiStyleSelected = viewModel::onUiStyleSelected,
                onScan = { scanLauncher.launch(Intent(this, QrScanActivity::class.java)) }
            )
        }
    }

    private fun startKyc(info: ProfileUrlInfo, language: String, uiStyle: UIStyle) {
        // Always set it: the SDK keeps the style in a process-wide FeatureConfig that is never
        // reset, so passing the current selection on every launch is what makes the picker stick.
        AmaniSDKUI.setUIStyle(uiStyle)
        AmaniSDKUI.setSelfiePoseEstimationV2PreparationVideo(R.raw.pose_video)

        // Re-init against the server the QR pointed at (it may differ from the App default).
        AmaniSDKUI.init(
            applicationContext = applicationContext,
            serverURL = info.serverUrl,
            amaniVersion = AmaniVersion.V2
        )

        AmaniSDKUI.goToKycActivity(
            activity = this,
            resultLauncher = kycLauncher,
            idNumber = "",
            authToken = info.token,
            language = language,   // from the country selection
            geoLocation = true
        )
    }
}
