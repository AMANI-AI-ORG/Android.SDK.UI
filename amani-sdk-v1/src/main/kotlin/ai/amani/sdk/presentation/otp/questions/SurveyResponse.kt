package ai.amani.sdk.presentation.otp.questions

data class SurveyResponse(
    var question: String,
    var singleOptionAnswer: String? = null,
    var multipleOptionAnswer: List<String>? = null,
    var typedAnswer : String? = null
)