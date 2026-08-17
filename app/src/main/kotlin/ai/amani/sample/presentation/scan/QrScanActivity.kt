package ai.amani.sample.presentation.scan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.Size

/**
 * Portrait QR scan screen (Compose UI, ZXing engine) — a straight port of the verify app's
 * ScanQRCodeFragment. Returns the scanned QR text via [EXTRA_QR_TEXT] to the caller.
 */
class QrScanActivity : ComponentActivity() {

    companion object {
        const val EXTRA_QR_TEXT = "qr_text"
    }

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var capture: CaptureManager

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_QR_TEXT, result.result.text))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        barcodeView = DecoratedBarcodeView(this).apply {
            barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            // ~300dp square framing rect, matching the verify app's custom scanner layout.
            val px = (300 * resources.displayMetrics.density).toInt()
            barcodeView.framingRectSize = Size(px, px)
            setStatusText("")
        }

        capture = CaptureManager(this, barcodeView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.setShowMissingCameraPermissionDialog(true)

        setContent { QrScanScreen(barcodeView = barcodeView, onBack = { finish() }) }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
        barcodeView.decodeSingle(callback)
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
