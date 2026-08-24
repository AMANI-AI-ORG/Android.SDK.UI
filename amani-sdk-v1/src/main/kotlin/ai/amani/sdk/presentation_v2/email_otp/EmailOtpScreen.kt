package ai.amani.sdk.presentation_v2.email_otp

import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.phone_otp.OtpPhase
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EmailOtpRoute(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailOtpViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { onCompleted() }
    }
    BackHandler {
        when {
            state.phase == OtpPhase.EnterCode && state.resendEnabled -> viewModel.backToContact()
            state.phase == OtpPhase.EnterCode -> Unit
            else -> onBack()
        }
    }
    EmailOtpScreen(
        state = state,
        onBack = { if (state.phase == OtpPhase.EnterCode) viewModel.backToContact() else onBack() },
        onInputChange = viewModel::onInputChange,
        onContinue = viewModel::onContinue,
        onResend = viewModel::backToContact,
        modifier = modifier
    )
}

@Composable
fun EmailOtpScreen(
    state: EmailOtpUiState,
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
            OutlinedTextField(
                value = state.input,
                onValueChange = { input -> onInputChange(if (isCode) input.filter { it.isDigit() } else input) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { if (state.hint.isNotBlank()) Text(state.hint, color = palette.inkLight) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isCode) KeyboardType.Number else KeyboardType.Email
                ),
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
                PrimaryButton(text = state.continueText, enabled = state.continueEnabled, onClick = onContinue)
            }
        }
    }
}

@Preview(name = "EmailOtp — enter email", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewEmailEnter() {
    AmaniV2Theme {
        EmailOtpScreen(
            state = EmailOtpUiState(
                phase = OtpPhase.EnterContact,
                headerTitle = "Email",
                description = "We'll send a verification code to your email.",
                label = "Email address",
                hint = "you@example.com",
                continueText = "Continue",
                resendText = "Resend code",
                input = "you@example.com"
            ),
            onBack = {}, onInputChange = {}, onContinue = {}, onResend = {}
        )
    }
}

@Preview(name = "EmailOtp — enter code", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewEmailCode() {
    AmaniV2Theme {
        EmailOtpScreen(
            state = EmailOtpUiState(
                phase = OtpPhase.EnterCode,
                headerTitle = "Verify",
                description = "Enter the code we sent to your email.",
                label = "Verification code",
                hint = "",
                continueText = "Continue",
                resendText = "Resend code",
                input = "123456",
                secondsRemaining = 174
            ),
            onBack = {}, onInputChange = {}, onContinue = {}, onResend = {}
        )
    }
}
