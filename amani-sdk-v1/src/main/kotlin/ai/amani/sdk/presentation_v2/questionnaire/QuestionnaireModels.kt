package ai.amani.sdk.presentation_v2.questionnaire

/**
 * V2 (Compose) questionnaire models — the port of the v1 SurveyAdapter's view-type logic
 * into plain, stateless data. The SDK bridge is reused as-is: answers are converted to the
 * shared [ai.amani.sdk.presentation.otp.questions.SurveyResponse] +
 * [ai.amani.sdk.data.mapper.QuestionnaireMapper] before upload, so only the UI is rewritten.
 */

/**
 * How a question's answer is captured — drives which composable renders it. Ported from the
 * v1 `answerType` string switch (`single_choice` / `multiple_choice` / `text`), extended with
 * the previously-missing [Number] type. [Text] shows a plain text field; [Number] shows the
 * same field but with a numeric keypad and digit-only input.
 */
enum class QuestionType { SingleChoice, MultipleChoice, Text, Number }

/** Maps the server `answerType` string to a [QuestionType]; unknown types fall back to [Text]. */
fun questionTypeOf(answerType: String?): QuestionType = when (answerType?.trim()?.lowercase()) {
    "single_choice" -> QuestionType.SingleChoice
    "multiple_choice" -> QuestionType.MultipleChoice
    "number" -> QuestionType.Number
    "text" -> QuestionType.Text
    else -> QuestionType.Text
}

/** A selectable option for a single/multiple choice question. */
data class AnswerOption(val id: String, val title: String)

/**
 * A question plus its current answer. Immutable: the view model rebuilds the list on each
 * change so Compose recomposes. [isAnswered] mirrors v1's per-type "answered" rule, which the
 * screen uses to gate the submit button (all questions must be answered).
 */
data class QuestionUi(
    val id: String,
    val title: String,
    val type: QuestionType,
    val options: List<AnswerOption> = emptyList(),
    val singleAnswerId: String? = null,
    val selectedAnswerIds: Set<String> = emptySet(),
    val typedAnswer: String = ""
) {
    val isAnswered: Boolean
        get() = when (type) {
            QuestionType.SingleChoice -> singleAnswerId != null
            QuestionType.MultipleChoice -> selectedAnswerIds.isNotEmpty()
            QuestionType.Text, QuestionType.Number -> typedAnswer.isNotBlank()
        }
}

/** Rendering state for the questionnaire screen. */
sealed interface QuestionnaireScreenState {
    /** Fetching the questions from the SDK. */
    data object Loading : QuestionnaireScreenState

    /** Questions loaded; [submitEnabled] is true once every question is answered. */
    data class Ready(
        val questions: List<QuestionUi>,
        val submitting: Boolean = false,
        val submitEnabled: Boolean = false
    ) : QuestionnaireScreenState

    /** Fetch or submit failed; [message] is surfaced to the user. */
    data class Error(val message: String) : QuestionnaireScreenState
}
