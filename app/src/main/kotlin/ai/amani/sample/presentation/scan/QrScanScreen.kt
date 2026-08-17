package ai.amani.sample.presentation.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.DecoratedBarcodeView

private val ScanBg = Color(0xFFEEF4FA)
private val Black20 = Color(0xFF20202F)
private val rubik300 = FontFamily(Font(ai.amani.amani_sdk.R.font.rubik_300, FontWeight.Light))
private val rubik500 = FontFamily(Font(ai.amani.amani_sdk.R.font.rubik_500, FontWeight.Medium))

/**
 * Compose reproduction of the verify app's QR scan screen (fragment_qr_code): a top toolbar
 * (back + centered title), the ZXing [DecoratedBarcodeView] hosted via [AndroidView], and a
 * bottom instruction + logo. The activity locks orientation to portrait.
 */
@Composable
fun QrScanScreen(barcodeView: DecoratedBarcodeView, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScanBg)
    ) {
        // Toolbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
        ) {
            Image(
                painter = painterResource(ai.amani.amani_sdk.R.drawable.ic_back_press),
                contentDescription = "Back",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(50.dp)
                    .clickable { onBack() }
                    .padding(15.dp)
            )
            Text(
                text = "Scan QR Code",
                color = Color.Black,
                fontSize = 18.sp,
                fontFamily = rubik500,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        HorizontalDivider(color = Color.White, thickness = 1.dp)

        // Live camera / scanner
        AndroidView(
            factory = { barcodeView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 20.dp)
        )

        Text(
            text = "Please scan the QR code provided for you to start verification",
            color = Black20,
            fontSize = 16.sp,
            fontFamily = rubik300,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        Image(
            painter = painterResource(ai.amani.amani_sdk.R.drawable.ic_bottom_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        )
    }
}
