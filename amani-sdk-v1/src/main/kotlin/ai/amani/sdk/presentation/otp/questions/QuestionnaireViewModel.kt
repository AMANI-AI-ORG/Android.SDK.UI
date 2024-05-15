package ai.amani.sdk.presentation.otp.questions

import ai.amani.sdk.Amani
import ai.amani.sdk.data.mapper.QuestionnaireMapper.asSDKSubmitAnswerModel
import ai.amani.sdk.model.OTPScreenArgModel
import ai.amani.sdk.presentation.common.BaseViewModel
import ai.amani.sdk.presentation.otp.profile_info.ProfileInfoFragmentArgs
import ai.amani.sdk.presentation.otp.profile_info.ProfileInfoFragmentDirections
import ai.amani.sdk.utils.AppConstant
import androidx.navigation.NavDirections
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.StepConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.NullPointerException

/**
 * @Author: @zekiamani
 * @Date: 14.01.2024
 */
class QuestionnaireViewModel : BaseViewModel() {

    private var _uiState: MutableStateFlow<QuestionnaireState> =
        MutableStateFlow(QuestionnaireState.Empty)
    var uiState: StateFlow<QuestionnaireState> = _uiState

    private var surveyResponse = listOf<SurveyResponse>()
    private var step: StepConfig? = null
    private var config: GeneralConfigs? = null
    private var steps: ArrayList<StepConfig> = arrayListOf()

    val surveyAdapter = SurveyAdapter(arrayListOf(), object : SurveyAdapter.SurveyCallback {
        override fun onAllQuestionsAnswered(answeredQuestions: List<SurveyResponse>) {
            _uiState.value = QuestionnaireState.SubmitButtonClickable(clickable = true)
            surveyResponse = answeredQuestions
        }

        override fun onMissingQuestion() {
            _uiState.value = QuestionnaireState.SubmitButtonClickable(clickable = false)
        }
    })

    fun setArgs(args: ProfileInfoFragmentArgs) {
        steps = args.data.steps
        args.data.steps.forEach {
            if (it.identifier == AppConstant.IDENTIFIER_QUESTIONNAIRE) {
                step = it
            }
        }

        config = args.data.config
    }
    init {
        getQuestions()
    }

    fun onClickSubmit() {
        submitAnswers(surveyResponse = surveyResponse)
    }

    private fun getQuestions() {
        _uiState.value = QuestionnaireState.Loading
        Amani.sharedInstance().Questionnaire().getQuestions(
            onComplete = {
                val surveyList = arrayListOf<SurveyQuestion>()
                it.questions.forEachIndexed { index, questionnaireItem ->
                    val answers = arrayListOf<SurveyAnswer>()
                    questionnaireItem.answers.forEach { answer ->
                        answers.add(
                            SurveyAnswer(
                                id = answer.id,
                                title = answer.title
                            )
                        )
                    }
                    surveyList.add(
                        SurveyQuestion(
                            id = questionnaireItem.id,
                            title = questionnaireItem.title,
                            answerType = questionnaireItem.answerType,
                            answers = answers
                        )
                    )
                }

                _uiState.value = QuestionnaireState.Success

                CoroutineScope(Dispatchers.Main).launch {
                    surveyAdapter.updateQuestions(surveyList)
                }

            }, onError = {
                _uiState.value = QuestionnaireState.Error("Something gone wrong!")
            })
    }

    private fun submitAnswers(surveyResponse: List<SurveyResponse>) {
        _uiState.value = QuestionnaireState.Loading
        Amani.sharedInstance().Questionnaire().submitAnswers(
            surveyResponse.asSDKSubmitAnswerModel(),
            onComplete = {
                _uiState.value = QuestionnaireState.Success
                getNavDirection(
                    returnToHomeKYC = {
                        navigateToHomeScreen()
                    },

                    navDirection = {
                        navigateTo(it)
                    }
                )
            },
            onError = {
                _uiState.value = QuestionnaireState.Error(it.message.toString())
            }
        )
    }

    /**
     * Navigation direction will be decided accordingly sorted step list
     */
    private fun getNavDirection(
        returnToHomeKYC: () -> Unit,
        navDirection: (NavDirections) -> Unit
    ) {
        val steps: ArrayList<StepConfig> = this.steps
        steps.remove(step)
        if (steps.isEmpty()) {
            //If steps is empty returning to HomeKYC
            returnToHomeKYC.invoke()
            return
        }
        val direction: NavDirections? = when(steps.first().identifier) {
            AppConstant.IDENTIFIER_PHONE_OTP -> {
                QuestionnaireFragmentDirections.actionQuestionnaireFragmentToVerifyPhoneFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_EMAIL_OTP -> {
                QuestionnaireFragmentDirections.actionQuestionnaireFragmentToVerifyEmailFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            AppConstant.IDENTIFIER_PROFILE_INFO -> {
                QuestionnaireFragmentDirections.actionQuestionnaireFragmentToProfileInfoFragment(
                    OTPScreenArgModel(
                        steps = steps,
                        config = config!!
                    )
                )
            }

            else -> {
                null
            }
        }

        if (direction == null) throw Exception(NullPointerException("Nav direction is null"))
        else navDirection.invoke(direction)
    }
}