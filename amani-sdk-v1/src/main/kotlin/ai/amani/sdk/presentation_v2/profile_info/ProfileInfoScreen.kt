package ai.amani.sdk.presentation_v2.profile_info

import ai.amani.sdk.presentation.otp.profile_info.DatePickerHandler
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Stateful entry point: hosts [ProfileInfoViewModel], streams state into [ProfileInfoScreen]
 * and forwards intents. Emits [onCompleted] when the profile step's verdict comes back
 * approved so the host can advance the pre/post-KYC chain.
 */
@Composable
fun ProfileInfoRoute(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileInfoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { onCompleted() }
    }
    ProfileInfoScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onSurnameChange = viewModel::onSurnameChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onSubmit = viewModel::submit,
        modifier = modifier
    )
}

/**
 * Stateless profile-info form — the V2 redesign of v1's fragment_profile_info.xml. Name and
 * surname are free text; the birth date opens the shared [DatePickerHandler] dialog (v1 reused).
 * All labels/hints are config-driven via [ProfileInfoUiState].
 */
@Composable
fun ProfileInfoScreen(
    state: ProfileInfoUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val contentMaxWidth = amaniV2ContentMaxWidth()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
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
            LabeledTextField(
                title = state.nameTitle,
                value = state.name,
                hint = state.nameHint,
                onValueChange = onNameChange
            )
            Spacer(Modifier.height(16.dp))
            LabeledTextField(
                title = state.surnameTitle,
                value = state.surname,
                hint = state.surnameHint,
                onValueChange = onSurnameChange
            )
            Spacer(Modifier.height(16.dp))
            // Birth date is picker-only (no keyboard), matching v1's read-only field + dialog.
            DateField(
                title = state.birthDateTitle,
                value = state.birthDate,
                hint = state.birthDateHint,
                onClick = {
                    DatePickerHandler(context) { onBirthDateChange(it) }.showDatePickerDialog()
                }
            )
            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(error, style = AmaniV2Type.bodySmall.scaled(), color = palette.danger)
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
                    enabled = state.submitEnabled,
                    onClick = onSubmit
                )
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    title: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val shape = RoundedCornerShape(palette.buttonRadius.dp.scaled())
    Column(modifier.fillMaxWidth()) {
        if (title.isNotBlank()) {
            Text(
                title,
                style = AmaniV2Type.label.copy(fontWeight = FontWeight.Medium).scaled(),
                color = palette.ink
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { if (hint.isNotBlank()) Text(hint, style = AmaniV2Type.body.scaled(), color = palette.inkLight) },
            shape = shape,
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
    }
}

/** A read-only, tappable field that opens the date picker (no keyboard). */
@Composable
private fun DateField(
    title: String,
    value: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val shape = RoundedCornerShape(palette.buttonRadius.dp.scaled())
    Column(modifier.fillMaxWidth()) {
        if (title.isNotBlank()) {
            Text(
                title,
                style = AmaniV2Type.label.copy(fontWeight = FontWeight.Medium).scaled(),
                color = palette.ink
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp.scaled())
                .background(palette.surface, shape)
                .border(1.dp, palette.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp.scaled()),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                value.ifBlank { hint },
                style = AmaniV2Type.body.scaled(),
                color = if (value.isBlank()) palette.inkLight else palette.ink
            )
        }
    }
}

@Preview(name = "ProfileInfo", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewProfileInfo() {
    AmaniV2Theme {
        ProfileInfoScreen(
            state = ProfileInfoUiState(
                headerTitle = "Profile",
                description = "Enter your details exactly as they appear on your ID.",
                nameTitle = "Name",
                nameHint = "Your name",
                surnameTitle = "Surname",
                surnameHint = "Your surname",
                birthDateTitle = "Date of birth",
                birthDateHint = "yyyy-mm-dd",
                continueText = "Continue",
                name = "Jane",
                surname = "Doe",
                birthDate = "1990-05-12"
            ),
            onBack = {}, onNameChange = {}, onSurnameChange = {}, onBirthDateChange = {}, onSubmit = {}
        )
    }
}
