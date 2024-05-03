package ai.amani.sdk.presentation.nfc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class LoadingDotView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private companion object {
        const val DOT_COUNT = 5
        const val ANIMATION_DELAY = 1000L
        const val COLOR_GRAY = "#D9D9D9"
        const val COLOR_GREEN = "#5BC792"
    }

    private val dotColors = MutableList(DOT_COUNT) { COLOR_GRAY }
    private var currentDot = 0
    private var allDotsAreGreen = false
    private var animationJob: Job? = null
    private var paint = Paint()

    fun startAnimation() {
        if (animationJob != null) if(animationJob!!.isActive || animationJob!!.isCancelled) return
        this.visibility = VISIBLE
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            while (animationJob!!.isActive && !animationJob!!.isCancelled) {
                updateDots()
                invalidate()
                delay(ANIMATION_DELAY)
            }
        }
    }

    fun stopAnimation() {
        dotColors.fill(COLOR_GRAY)
        this.visibility = INVISIBLE
        animationJob?.cancel()
        animationJob = null
    }

    private fun updateDots() {
        dotColors[currentDot] = COLOR_GREEN
        currentDot = (currentDot + 1) % DOT_COUNT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (animationJob == null) return
        if (animationJob!!.isCancelled) return

        val dotRadius = 25f // You can adjust the radius as needed
        val spacing = 12f // You can adjust the spacing between dots

        val totalWidth = DOT_COUNT * (2 * dotRadius + spacing) - spacing
        val startX = (width - totalWidth) / 2f

        dotColors.indices.forEach {
            paint.color = Color.parseColor(dotColors[it])
            val cx = startX + it * (2 * dotRadius + spacing) + dotRadius
            val cy = height / 2f
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }

        resetColorIfAllGreen()

    }

    /**
     * Checks the if all the dots is green reset the colors
     */
    private fun resetColorIfAllGreen() {
        if (allDotsAreGreen) {
            paint = Paint()
            dotColors.fill(COLOR_GRAY)
            currentDot = 0
            invalidate()
        }

        allDotsAreGreen = currentDot == DOT_COUNT - 1
    }
}
