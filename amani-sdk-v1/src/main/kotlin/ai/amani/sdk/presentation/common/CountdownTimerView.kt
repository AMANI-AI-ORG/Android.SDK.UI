package ai.amani.sdk.presentation.common

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat


class CountdownTimerView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var tvCountdown: TextView
    private lateinit var countDownTimer: CountDownTimer
    var state: CountDownState? = null

    companion object {
        private var COUNT_DOWN_TIME = 180L
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER

        val typeface = ResourcesCompat.getFont(context, ai.amani.amani_sdk.R.font.rubik_400)
        tvCountdown = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            textSize = 24f
            this.typeface = typeface
            this.setTextColor(ContextCompat.getColor(context, ai.amani.amani_sdk.R.color.black_20))
        }
        addView(tvCountdown)
    }

    /**
     * @param countDownTimeSeconds Count down time as seconds
     */
    fun countDownTime(countDownTimeSeconds: Long)= apply {
        tvCountdown.text = countDownTimeSeconds.toAnalogMinute()
        COUNT_DOWN_TIME = countDownTimeSeconds
    }

    /**
     * Starts the count down timer
     * @param countDownState state callback
     */
    fun startCountdown(countDownState: CountDownState) = apply {
        state = countDownState
        state?.started()
        countDownTimer = object : CountDownTimer(COUNT_DOWN_TIME * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountdownText(millisUntilFinished)
            }

            override fun onFinish() {
                updateCountdownText(0)
                state?.finished()
            }
        }

        countDownTimer.start()
    }

    private fun updateCountdownText(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 60000
        val seconds = (millisUntilFinished % 60000) / 1000
        val timeLeft = String.format("%02d:%02d", minutes, seconds)
        tvCountdown.text = timeLeft
    }

    interface CountDownState {
        fun started()

        fun finished()
    }

    private fun Long.toAnalogMinute(): String {
        val min = this/60
        val seconds = this - (min * 60)
        return "$min:$seconds"
    }
}
