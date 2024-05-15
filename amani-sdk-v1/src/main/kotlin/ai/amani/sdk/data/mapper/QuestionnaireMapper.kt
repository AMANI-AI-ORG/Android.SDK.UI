package ai.amani.sdk.data.mapper

import ai.amani.sdk.model.questionnaire.SubmitAnswer
import ai.amani.sdk.presentation.otp.questions.SurveyResponse

/**
 * @Author: @zekiamani
 * @Date: 15.01.2024
 */
object QuestionnaireMapper {

    fun List<SurveyResponse>.asSDKSubmitAnswerModel(): List<SubmitAnswer> {
        val listOfSubmitAnswers = arrayListOf<SubmitAnswer>()

        this.forEach {
            listOfSubmitAnswers.add(
                SubmitAnswer(
                    question = it.question,
                    multipleOptionAnswer = it.multipleOptionAnswer,
                    singleOptionAnswer = it.singleOptionAnswer,
                    typedAnswer = it.typedAnswer
                )
            )
        }

        return listOfSubmitAnswers
    }
}