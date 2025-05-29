package ai.amani.sdk.presentation.otp.questions

import ai.amani.amani_sdk.R
import ai.amani.sdk.extentions.gone
import ai.amani.sdk.extentions.remove
import ai.amani.sdk.extentions.show
import ai.amani.sdk.presentation.common.CustomTextView
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
import ai.amani.sdk.presentation.otp.profile_info.ProfileInfoFragmentArgs
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDivider


class SurveyAdapter(
    private var questionData: ArrayList<SurveyQuestion>,
    private val surveyCallback: SurveyCallback,
    private val args: ProfileInfoFragmentArgs
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface SurveyCallback {
        fun onAllQuestionsAnswered(answeredQuestions: List<SurveyResponse>)

        fun onMissingQuestion()
    }

    private val answeredQuestions: MutableList<SurveyResponse> = mutableListOf()

    fun updateQuestions(questions: List<SurveyQuestion>) {
        questions.forEachIndexed {index, item ->
            questionData.add(item)
            notifyItemInserted(index + 1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TEXT_QUESTION -> TextQuestionViewHolder(
                inflater.inflate(R.layout.item_questionnaire_text, parent, false)
            )
            MULTIPLE_CHOICE_QUESTION -> MultipleChoiceQuestionViewHolder(
                inflater.inflate(R.layout.item_questionnaire_multiple_choice, parent, false)
            )
            SINGLE_CHOICE_QUESTION -> SingleChoiceQuestionViewHolder(
                inflater.inflate(R.layout.item_questionnaire_single_question, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val question = questionData[position]

        when (holder.itemViewType) {
            TEXT_QUESTION -> {
                val textViewHolder = holder as TextQuestionViewHolder
                textViewHolder.bind(question, position)
            }
            MULTIPLE_CHOICE_QUESTION -> {
                val multipleChoiceViewHolder = holder as MultipleChoiceQuestionViewHolder
                multipleChoiceViewHolder.bind(question, position)
            }
            SINGLE_CHOICE_QUESTION -> {
                val singleChoiceViewHolder = holder as SingleChoiceQuestionViewHolder
                singleChoiceViewHolder.bind(question, position)
            }
        }
    }

    override fun getItemCount(): Int = questionData.size

    override fun getItemViewType(position: Int): Int {
        val question = questionData[position]

        return when (question.answerType) {
            "text" -> TEXT_QUESTION
            "multiple_choice" -> MULTIPLE_CHOICE_QUESTION
            "single_choice" -> SINGLE_CHOICE_QUESTION
            else -> throw IllegalArgumentException("Invalid answer type")
        }
    }

    inner class TextQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTitle: CustomTextView = itemView.findViewById(R.id.questionTitle)
        private val answerEditText: EditText = itemView.findViewById(R.id.answerEditText)
        private val headerSurvey: LinearLayout = itemView.findViewById(R.id.survey_header)
        private val firstDivider: MaterialDivider = itemView.findViewById(R.id.first_divider_text)
        private val lastDivider: MaterialDivider = itemView.findViewById(R.id.last_divider_text)
        private val descriptionText: CustomTextView = itemView.findViewById(R.id.text_question_desc)

        fun bind(question: SurveyQuestion, position: Int) {
            if (position == 0) {
                headerSurvey.show()
                firstDivider.show()
            } else {
                headerSurvey.remove()
                firstDivider.remove()
            }

            questionTitle.setTextProperty(question.title, args.data.config.appFontColor)
            args.data.steps.first().mDocuments?.first()?.versions
                ?.first()?.steps?.first()?.captureDescription?.let {
                    descriptionText.setTextProperty(it, args.data.config.appFontColor)
                }
            answerEditText.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(p0: Editable?) {
                    updateTypedQuestions(questionId = question.id, answer =  p0.toString())
                }
            })

            if (position == itemCount - 1) {
                lastDivider.show()
            } else lastDivider.remove()
        }
    }

    inner class MultipleChoiceQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTitle: CustomTextView = itemView.findViewById(R.id.questionTitle)
        private val answersContainer: RadioGroup = itemView.findViewById(R.id.answersContainer)
        private val headerSurvey: LinearLayout = itemView.findViewById(R.id.survey_header)
        private val firstDivider: MaterialDivider = itemView.findViewById(R.id.first_divider_multiple)
        private val lastDivider: MaterialDivider = itemView.findViewById(R.id.last_divider_multiple)
        private val descriptionText: CustomTextView = itemView.findViewById(R.id.multiple_desc)

        fun bind(question: SurveyQuestion, position: Int) {
            if (position == 0) {
                headerSurvey.show()
                firstDivider.show()
            } else {
                headerSurvey.remove()
                firstDivider.remove()
            }

            questionTitle.setTextProperty(question.title, args.data.config.appFontColor)
            args.data.steps.first().mDocuments?.first()?.versions
                ?.first()?.steps?.first()?.captureDescription?.let {
                    descriptionText.setTextProperty( it, args.data.config.appFontColor)
                }
            answersContainer.removeAllViews()

            for ((index, answer) in question.answers.withIndex()) {
                val paramCheckBox = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val checkBox = AppCompatCheckBox(itemView.context)
                checkBox.layoutParams = paramCheckBox
                checkBox.buttonTintList = ColorStateList.valueOf(Color.BLACK)
                checkBox.text = answer.title
                checkBox.id = answer.id.hashCode() // Use a unique ID for each CheckBox
                answersContainer.addView(checkBox)

                if (index < question.answers.size - 1) {
                    val divider = View(itemView.context)
                    val params = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        5
                    )
                    params.setMargins(0, 8, 0, 0)
                    divider.layoutParams = params
                    divider.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.background))
                    answersContainer.addView(divider)
                }

                checkBox.setOnCheckedChangeListener { _, _ ->
                    val selectedAnswers = question.answers.filter { itemView.findViewById<CheckBox>(it.id.hashCode()).isChecked }
                        .map { it.id }
                    updateAnsweredQuestions(question.id, selectedAnswers)
                }
            }

            if (position == itemCount - 1) {
                lastDivider.show()
            } else lastDivider.gone()
        }
    }

    inner class SingleChoiceQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTitle: CustomTextView = itemView.findViewById(R.id.questionTitle)
        private val answersContainer: RadioGroup = itemView.findViewById(R.id.answersContainer)
        private val headerSurvey: LinearLayout = itemView.findViewById(R.id.survey_header)
        private val firstDivider: MaterialDivider = itemView.findViewById(R.id.first_divider_single)
        private val lastDivider: MaterialDivider = itemView.findViewById(R.id.last_divider_text_single)
        private val descriptionText: CustomTextView = itemView.findViewById(R.id.single_question_desc)


        fun bind(question: SurveyQuestion, position: Int) {
            //Add header only for first element of the screen
            if (position == 0) {
                headerSurvey.show()
                firstDivider.show()
            } else {
                headerSurvey.remove()
                firstDivider.gone()
            }

            questionTitle.setTextProperty(question.title, args.data.config.appFontColor)
            args.data.steps.first().mDocuments?.first()?.versions
                ?.first()?.steps?.first()?.captureDescription?.let {
                    descriptionText.setTextProperty(it, args.data.config.appFontColor)
                }
            answersContainer.removeAllViews()

            for ((index, answer) in question.answers.withIndex()) {
                val paramRadioButton = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val radioButton = RadioButton(itemView.context)
                radioButton.layoutParams = paramRadioButton
                radioButton.buttonTintList = ColorStateList.valueOf(Color.BLACK)
                radioButton.text = answer.title
                radioButton.id = answer.id.hashCode() // Use a unique ID for each RadioButton
                answersContainer.addView(radioButton)

                if (index < question.answers.size - 1) {
                    val divider = View(itemView.context)
                    val params = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        5
                    )
                    params.setMargins(0, 8, 0, 0)
                    divider.layoutParams = params
                    divider.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.background))
                    answersContainer.addView(divider)
                }

                radioButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        updateSingleAnswers(question.id, answerId = answer.id)
                    }
                }
            }

            if (position == itemCount - 1) {
                lastDivider.show()
            } else lastDivider.gone()
        }
    }

    private fun updateSingleAnswers(questionId: String, answerId : String){
        val existingResponse = answeredQuestions.find { it.question == questionId }
        if (existingResponse != null) {
            // Update existing response
            answeredQuestions.remove(existingResponse)
            answeredQuestions.add(existingResponse.copy(singleOptionAnswer = answerId))
        } else {
            // Add new response
            answeredQuestions.add(SurveyResponse(questionId, singleOptionAnswer = answerId))
        }

        checkAllQuestionsAnswered()
    }
    private fun updateAnsweredQuestions(questionId: String, answerIds: List<String>) {
        val existingResponse = answeredQuestions.find { it.question == questionId }
        if (existingResponse != null) {
            // Update existing response
            answeredQuestions.remove(existingResponse)
            answeredQuestions.add(existingResponse.copy(multipleOptionAnswer = answerIds))
        } else {
            // Add new response
            answeredQuestions.add(SurveyResponse(questionId, multipleOptionAnswer = answerIds))
        }

        checkAllQuestionsAnswered()
    }

    private fun updateTypedQuestions(questionId: String, answer: String) {
        val existingResponse = answeredQuestions.find { it.question == questionId }
        if (existingResponse != null) {
            //Update existing response
            answeredQuestions.remove(existingResponse)
            answeredQuestions.add(existingResponse.copy(typedAnswer = answer))
        } else {
            answeredQuestions.add(SurveyResponse(questionId, typedAnswer = answer))
        }

        checkAllQuestionsAnswered()
    }

    private fun checkAllQuestionsAnswered() {
        if (answeredQuestions.size == questionData.size) {
            surveyCallback.onAllQuestionsAnswered(answeredQuestions)
        } else surveyCallback.onMissingQuestion()
    }

    companion object {
        private const val TEXT_QUESTION = 1
        private const val MULTIPLE_CHOICE_QUESTION = 2
        private const val SINGLE_CHOICE_QUESTION = 3
    }
}
