package ai.amani.sdk.presentation_v2.phone_otp

import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hbb20.CountryCodePicker

@Composable
fun PhoneOtpRoute(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhoneOtpViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { onCompleted() }
    }
    // On the code screen back returns to the phone screen; blocked until resend is available
    // (v1 countdown). On the phone screen back exits the SDK (pre-KYC).
    BackHandler {
        when {
            state.phase == OtpPhase.EnterCode && state.resendEnabled -> viewModel.backToContact()
            state.phase == OtpPhase.EnterCode -> Unit
            else -> onBack()
        }
    }
    PhoneOtpScreen(
        state = state,
        onBack = { if (state.phase == OtpPhase.EnterCode) viewModel.backToContact() else onBack() },
        onInputChange = viewModel::onInputChange,
        onContinue = viewModel::onContinue,
        onResend = viewModel::backToContact,
        modifier = modifier
    )
}

@Composable
fun PhoneOtpScreen(
    state: PhoneOtpUiState,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onContinue: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val contentMaxWidth = amaniV2ContentMaxWidth()
    val isCode = state.phase == OtpPhase.EnterCode

    Column(
        modifier = modifier.fillMaxSize().background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = state.headerTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            Spacer(Modifier.height(16.dp))
            if (state.description.isNotBlank()) {
                Text(state.description, style = AmaniV2Type.body.scaled(), color = palette.inkMuted)
                Spacer(Modifier.height(20.dp))
            }
            if (state.label.isNotBlank()) {
                Text(
                    state.label,
                    style = AmaniV2Type.label.copy(fontWeight = FontWeight.Medium).scaled(),
                    color = palette.ink
                )
                Spacer(Modifier.height(8.dp))
            }
            if (isCode) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = { input -> onInputChange(input.filter { it.isDigit() }) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { if (state.hint.isNotBlank()) Text(state.hint, color = palette.inkLight) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(palette.buttonRadius.dp.scaled()),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedIndicatorColor = palette.accent,
                        unfocusedIndicatorColor = palette.border,
                        cursorColor = palette.accent,
                        focusedTextColor = palette.ink,
                        unfocusedTextColor = palette.ink
                    )
                )
            } else {
                // Country-code selector (flags + dial code) on the left of the number field,
                // reusing the same CCP library v1 uses (com.hbb20) via AndroidView. The entered
                // phone = dialCode + local number (v1 PhoneVerify composition).
                var dialCode by rememberSaveable(state.phase) { mutableStateOf("") }
                var number by rememberSaveable(state.phase) { mutableStateOf("") }
                PhoneNumberField(
                    number = number,
                    // No placeholder: the CCP already shows the country/dial code, so the config
                    // phoneHint (e.g. "+90 …") would be redundant next to it.
                    hint = "",
                    onDialCode = { dialCode = it; onInputChange(it + number) },
                    onNumberChange = { number = it; onInputChange(dialCode + it) }
                )
            }
            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, style = AmaniV2Type.bodySmall.scaled(), color = palette.danger)
            }
            if (isCode && state.resendText.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                val resendLabel = if (state.resendEnabled) state.resendText
                else "${state.resendText} (${state.secondsRemaining}s)"
                Text(
                    resendLabel,
                    style = AmaniV2Type.bodySmall.copy(fontWeight = FontWeight.Medium).scaled(),
                    color = if (state.resendEnabled) palette.accent else palette.inkLight,
                    modifier = if (state.resendEnabled) Modifier.clickable(onClick = onResend) else Modifier
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(bottom = 20.dp)
        ) {
            if (state.submitting) {
                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 3.dp)
                }
            } else {
                PrimaryButton(
                    text = state.continueText,
                    enabled = state.continueEnabled,
                    onClick = onContinue
                )
            }
        }
    }
}

/** Country-code picker (com.hbb20 CCP via AndroidView) + number field, styled as one field. */
@Composable
private fun PhoneNumberField(
    number: String,
    hint: String,
    onDialCode: (String) -> Unit,
    onNumberChange: (String) -> Unit
) {
    val palette = AmaniV2Theme.palette
    val shape = RoundedCornerShape(palette.buttonRadius.dp.scaled())
    val currentOnDial by rememberUpdatedState(onDialCode)
    // CCP colors from the config palette.
    val contentArgb = palette.ink.toArgb()
    val surfaceArgb = palette.surface.toArgb()
    val accentArgb = palette.accent.toArgb()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp.scaled())
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (LocalInspectionMode.current) {
            Text("🇹🇷 +90", style = AmaniV2Type.body.scaled(), color = palette.ink, modifier = Modifier.padding(start = 12.dp))
        } else {
            AndroidView(
                modifier = Modifier.padding(start = 8.dp),
                factory = { ctx ->
                    CountryCodePicker(ctx).apply {
                        showFlag(true)
                        showNameCode(false)
                        showFullName(false)
                        setCountryForNameCode("tr")
                        setContentColor(contentArgb)
                        setDialogBackgroundColor(surfaceArgb)
                        setDialogTextColor(contentArgb)
                        setDialogSearchEditTextTintColor(accentArgb)
                        setOnCountryChangeListener { currentOnDial(selectedCountryCode) }
                    }.also { ccp -> ccp.post { currentOnDial(ccp.selectedCountryCode) } }
                }
            )
        }
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .width(1.dp)
                .height(24.dp)
                .background(palette.border)
        )
        BasicTextField(
            value = number,
            onValueChange = { onNumberChange(it.filter { c -> c.isDigit() }) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = AmaniV2Type.body.scaled().copy(color = palette.ink),
            cursorBrush = SolidColor(palette.accent),
            decorationBox = { inner ->
                if (number.isEmpty() && hint.isNotBlank()) {
                    Text(hint, style = AmaniV2Type.body.scaled(), color = palette.inkLight)
                }
                inner()
            }
        )
    }
}

@Preview(name = "PhoneOtp — enter phone", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewPhoneEnter() {
    AmaniV2Theme {
        PhoneOtpScreen(
            state = PhoneOtpUiState(
                phase = OtpPhase.EnterContact,
                headerTitle = "Phone",
                description = "We'll send a verification code to your phone.",
                label = "Phone number",
                hint = "+90 5xx xxx xx xx",
                continueText = "Continue",
                resendText = "Resend code",
                input = "+905551234567"
            ),
            onBack = {}, onInputChange = {}, onContinue = {}, onResend = {}
        )
    }
}

@Preview(name = "PhoneOtp — enter code", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewPhoneCode() {
    AmaniV2Theme {
        PhoneOtpScreen(
            state = PhoneOtpUiState(
                phase = OtpPhase.EnterCode,
                headerTitle = "Verify",
                description = "Enter the code we sent to your phone.",
                label = "Verification code",
                hint = "",
                continueText = "Continue",
                resendText = "Resend code",
                input = "123456",
                secondsRemaining = 172
            ),
            onBack = {}, onInputChange = {}, onContinue = {}, onResend = {}
        )
    }
}

/**
 * Preview-only mock of the country picker in its OPEN state. The real picker is the native
 * com.hbb20 dialog (can't render in @Preview), so this reproduces its look with the config
 * palette — including the search field on a [surface][AmaniV2Palette.surface] background.
 */
@Preview(name = "PhoneOtp — country picker open", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewPhoneCountryPickerOpen() {
    AmaniV2Theme {
        val palette = AmaniV2Theme.palette
        Box(Modifier.fillMaxSize().background(palette.background)) {
            PhoneOtpScreen(
                state = PhoneOtpUiState(
                    phase = OtpPhase.EnterContact,
                    headerTitle = "Phone",
                    description = "We'll send a verification code to your phone.",
                    label = "Phone number",
                    hint = "",
                    continueText = "Continue",
                    resendText = "Resend code"
                ),
                onBack = {}, onInputChange = {}, onContinue = {}, onResend = {}
            )
            // Open-picker overlay
            Box(
                Modifier.fillMaxSize().background(palette.ink.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(palette.surface, RoundedCornerShape(16.dp))
                        .border(0.5.dp, palette.border, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("Select country", style = AmaniV2Type.rowTitle.scaled(), color = palette.ink)
                    Spacer(Modifier.height(12.dp))
                    // Search field — background = surface (per request), delimited by a border.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .background(palette.surface, RoundedCornerShape(palette.buttonRadius.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(palette.buttonRadius.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Search", style = AmaniV2Type.body.scaled(), color = palette.inkLight)
                    }
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        Triple("🇹🇷", "Türkiye", "+90"),
                        Triple("🇬🇧", "United Kingdom", "+44"),
                        Triple("🇩🇪", "Germany", "+49"),
                        Triple("🇺🇸", "United States", "+1"),
                        Triple("🇫🇷", "France", "+33")
                    ).forEach { (flag, name, code) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(flag, style = AmaniV2Type.body.scaled())
                            Spacer(Modifier.width(12.dp))
                            Text(name, style = AmaniV2Type.body.scaled(), color = palette.ink, modifier = Modifier.weight(1f))
                            Text(code, style = AmaniV2Type.body.scaled(), color = palette.inkMuted)
                        }
                    }
                }
            }
        }
    }
}
