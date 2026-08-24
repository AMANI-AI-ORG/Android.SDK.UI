package ai.amani.sdk.presentation_v2.questionnaire

import ai.amani.sdk.presentation_v2.components.AmaniV2Loader
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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

/**
 * Stateful entry point: hosts [QuestionnaireViewModel], streams its state into the stateless
 * [QuestionnaireScreen] and forwards intents. Emits [onCompleted] once the answers are
 * accepted by the SDK so the host can advance the flow (pop to Home / next pre-KYC step).
 */
@Composable
fun QuestionnaireRoute(
    headerTitle: String,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    submitButtonText: String = "Continue",
    // v1 parity: the error-state retry button reads GeneralConfigs.tryAgainText.
    tryAgainText: String = CachingHomeKYC.appConfig?.generalConfigs?.tryAgainText
        ?.takeIf { it.isNotBlank() } ?: "Try again",
    viewModel: QuestionnaireViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { onCompleted() }
    }
    QuestionnaireScreen(
        state = state,
        headerTitle = headerTitle,
        submitButtonText = submitButtonText,
        tryAgainText = tryAgainText,
        onBack = onBack,
        onSingleSelect = viewModel::onSingleSelect,
        onMultiToggle = viewModel::onMultiToggle,
        onTextChange = viewModel::onTextChange,
        onRetry = viewModel::retry,
        onSubmit = viewModel::submit,
        modifier = modifier
    )
}

/**
 * Stateless questionnaire screen — the V2 redesign of v1's QuestionnaireFragment +
 * item_questionnaire_* layouts. Renders each question by its [QuestionType] and gates the
 * bottom submit button on [QuestionnaireScreenState.Ready.submitEnabled] (all answered).
 */
@Composable
fun QuestionnaireScreen(
    state: QuestionnaireScreenState,
    headerTitle: String,
    onBack: () -> Unit,
    onSingleSelect: (questionId: String, answerId: String) -> Unit,
    onMultiToggle: (questionId: String, answerId: String) -> Unit,
    onTextChange: (questionId: String, text: String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    submitButtonText: String = "Continue",
    tryAgainText: String = "Try again",
    onRetry: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val contentMaxWidth = amaniV2ContentMaxWidth()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = headerTitle, onBack = onBack)

        when (state) {
            QuestionnaireScreenState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                AmaniV2Loader()
            }

            is QuestionnaireScreenState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(AmaniV2Dimens.screenPadding),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, style = AmaniV2Type.body.scaled(), color = palette.inkMuted)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(text = tryAgainText, onClick = onRetry)
                }
            }

            is QuestionnaireScreenState.Ready -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = contentMaxWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AmaniV2Dimens.screenPadding)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.questions.forEach { question ->
                            QuestionCard(
                                question = question,
                                onSingleSelect = onSingleSelect,
                                onMultiToggle = onMultiToggle,
                                onTextChange = onTextChange
                            )
                        }
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
                            text = submitButtonText,
                            enabled = state.submitEnabled,
                            onClick = onSubmit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionUi,
    onSingleSelect: (String, String) -> Unit,
    onMultiToggle: (String, String) -> Unit,
    onTextChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val cardShape = RoundedCornerShape(AmaniV2Dimens.cardRadius.scaled())
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface, cardShape)
            .border(0.5.dp, palette.border, cardShape)
            .padding(16.dp.scaled())
    ) {
        Text(
            question.title,
            style = AmaniV2Type.rowTitle.scaled(),
            color = palette.ink
        )
        Spacer(Modifier.height(12.dp))
        when (question.type) {
            QuestionType.SingleChoice -> question.options.forEach { option ->
                ChoiceRow(
                    text = option.title,
                    selected = question.singleAnswerId == option.id,
                    multi = false,
                    onClick = { onSingleSelect(question.id, option.id) }
                )
            }

            QuestionType.MultipleChoice -> question.options.forEach { option ->
                ChoiceRow(
                    text = option.title,
                    selected = option.id in question.selectedAnswerIds,
                    multi = true,
                    onClick = { onMultiToggle(question.id, option.id) }
                )
            }

            QuestionType.Text -> AnswerField(
                value = question.typedAnswer,
                keyboardType = KeyboardType.Text,
                onValueChange = { onTextChange(question.id, it) }
            )

            QuestionType.Number -> AnswerField(
                value = question.typedAnswer,
                keyboardType = KeyboardType.Number,
                // Number field: keep only digits so the answer matches the numeric keypad.
                onValueChange = { input -> onTextChange(question.id, input.filter { it.isDigit() }) }
            )
        }
    }
}

/** A single radio (single choice) or checkbox (multiple choice) option row. */
@Composable
private fun ChoiceRow(
    text: String,
    selected: Boolean,
    multi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multi) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.inkLight
                )
            )
        } else {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = palette.accent,
                    unselectedColor = palette.inkLight
                )
            )
        }
        Spacer(Modifier.size(8.dp.scaled()))
        Text(text, style = AmaniV2Type.body.scaled(), color = palette.ink)
    }
}

/** Free-text / numeric answer field. [keyboardType] decides which keyboard opens. */
@Composable
private fun AnswerField(
    value: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
}

@Preview(name = "Questionnaire", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewQuestionnaire() {
    AmaniV2Theme {
        QuestionnaireScreen(
            state = QuestionnaireScreenState.Ready(
                questions = listOf(
                    QuestionUi(
                        id = "1", title = "What is your employment status?",
                        type = QuestionType.SingleChoice,
                        options = listOf(AnswerOption("a", "Employed"), AnswerOption("b", "Self-employed")),
                        singleAnswerId = "a"
                    ),
                    QuestionUi(
                        id = "2", title = "Which services do you use?",
                        type = QuestionType.MultipleChoice,
                        options = listOf(AnswerOption("x", "Payments"), AnswerOption("y", "Savings")),
                        selectedAnswerIds = setOf("x")
                    ),
                    QuestionUi(id = "3", title = "Your monthly income", type = QuestionType.Number, typedAnswer = "5000"),
                    QuestionUi(id = "4", title = "Any notes?", type = QuestionType.Text, typedAnswer = "None")
                ),
                submitEnabled = true
            ),
            headerTitle = "Questionnaire",
            onBack = {}, onSingleSelect = { _, _ -> }, onMultiToggle = { _, _ -> },
            onTextChange = { _, _ -> }, onSubmit = {}
        )
    }
}
