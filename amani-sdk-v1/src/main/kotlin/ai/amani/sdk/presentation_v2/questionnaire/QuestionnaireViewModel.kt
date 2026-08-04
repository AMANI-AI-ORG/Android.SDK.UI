package ai.amani.sdk.presentation_v2.questionnaire

import ai.amani.sdk.Amani
import ai.amani.sdk.data.mapper.QuestionnaireMapper.asSDKSubmitAnswerModel
import ai.amani.sdk.presentation.otp.questions.SurveyResponse
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * V2 questionnaire view model — the Compose port of v1's QuestionnaireViewModel +
 * SurveyAdapter answer bookkeeping, unified in one place.
 *
 * Behaviour mirrors v1:
 *  - on init it fetches the questions via `Amani.Questionnaire().getQuestions`
 *  - it tracks each question's answer and marks the flow submittable only once *every*
 *    question is answered (v1's `checkAllQuestionsAnswered`)
 *  - submit converts the answers to the shared [SurveyResponse] + `asSDKSubmitAnswerModel`
 *    bridge and calls `Amani.Questionnaire().submitAnswers`; success emits [completed] so the
 *    host can advance the flow.
 *
 * The only additions over v1 are the [QuestionType.Number] handling (numeric keypad) and the
 * explicit [QuestionType.Text] field; both submit through the same `typedAnswer` path.
 */
class QuestionnaireViewModel : ViewModel() {

    private val _state = MutableStateFlow<QuestionnaireScreenState>(QuestionnaireScreenState.Loading)
    val state: StateFlow<QuestionnaireScreenState> = _state.asStateFlow()

    // One-shot "answers submitted" signal for the host to advance the flow (pop to Home /
    // continue to the next pre-KYC step). SharedFlow so it isn't re-delivered on recomposition.
    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    private var questions: List<QuestionUi> = emptyList()

    init {
        fetchQuestions()
    }

    private fun fetchQuestions() {
        _state.value = QuestionnaireScreenState.Loading
        Amani.sharedInstance().Questionnaire().getQuestions(
            onComplete = { result ->
                questions = result.questions.map { item ->
                    QuestionUi(
                        id = item.id,
                        title = item.title,
                        type = questionTypeOf(item.answerType),
                        options = item.answers.map { AnswerOption(it.id, it.title) }
                    )
                }
                emitReady()
            },
            onError = {
                _state.value = QuestionnaireScreenState.Error("Something went wrong!")
            }
        )
    }

    private fun emitReady(submitting: Boolean = false) {
        _state.value = QuestionnaireScreenState.Ready(
            questions = questions,
            submitting = submitting,
            // v1 parity: submit is only enabled once every question has an answer.
            submitEnabled = questions.isNotEmpty() && questions.all { it.isAnswered }
        )
    }

    private fun update(questionId: String, transform: (QuestionUi) -> QuestionUi) {
        questions = questions.map { if (it.id == questionId) transform(it) else it }
        emitReady()
    }

    fun onSingleSelect(questionId: String, answerId: String) =
        update(questionId) { it.copy(singleAnswerId = answerId) }

    fun onMultiToggle(questionId: String, answerId: String) =
        update(questionId) { q ->
            val next = q.selectedAnswerIds.toMutableSet().apply { if (!add(answerId)) remove(answerId) }
            q.copy(selectedAnswerIds = next)
        }

    fun onTextChange(questionId: String, text: String) =
        update(questionId) { it.copy(typedAnswer = text) }

    fun submit() {
        val current = questions
        if (current.isEmpty() || current.any { !it.isAnswered }) return
        emitReady(submitting = true)

        // Convert to the shared SDK bridge (same model v1 uploads through).
        val responses = current.map { q ->
            when (q.type) {
                QuestionType.SingleChoice ->
                    SurveyResponse(question = q.id, singleOptionAnswer = q.singleAnswerId)
                QuestionType.MultipleChoice ->
                    SurveyResponse(question = q.id, multipleOptionAnswer = q.selectedAnswerIds.toList())
                QuestionType.Text, QuestionType.Number ->
                    SurveyResponse(question = q.id, typedAnswer = q.typedAnswer)
            }
        }

        Amani.sharedInstance().Questionnaire().submitAnswers(
            responses.asSDKSubmitAnswerModel(),
            onComplete = { _completed.tryEmit(Unit) },
            onError = { error ->
                _state.value = QuestionnaireScreenState.Error(
                    error.message ?: "Could not submit your answers. Please try again."
                )
            }
        )
    }

    /** Re-run the fetch after an error. */
    fun retry() = fetchQuestions()
}
