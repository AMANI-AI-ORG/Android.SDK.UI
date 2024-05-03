package ai.amani.sdk.presentation.otp.questions

data class SurveyQuestion(
    val id: String,
    val title: String,
    val answerType: String,
    val answers: List<SurveyAnswer>
)

data class SurveyAnswer(
    val id: String,
    val title: String
)