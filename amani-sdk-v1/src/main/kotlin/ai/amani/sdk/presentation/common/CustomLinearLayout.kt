package ai.amani.sdk.presentation.common

import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.content.res.ColorStateList
import ai.amani.sdk.presentation.common.CustomLinearLayout
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils

class CustomLinearLayout : ConstraintLayout {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    /**
     * @param drawable    custom drawable
     * @param solidColor  main background color
     * @param strokeSize  outline size
     * @param strokeColor outline color
     * @param alpha       set alpha
     * @param alphaColor  set alpha color if not null
     * @param isRounded   set rounded corner if true
     * @param corner      corner radius
     */
    fun setBackgroundDrawable(
        drawable: Drawable?,
        solidColor: String?,
        strokeSize: Int,
        strokeColor: String?,
        alpha: Float,
        alphaColor: String?,
        isRounded: Boolean,
        corner: Int
    ) {
        this.background = drawable
        val gradientDrawable = this.background as GradientDrawable
        if (!TextUtils.isEmpty(solidColor)) {
            gradientDrawable.color = ColorStateList.valueOf(Color.parseColor(solidColor))
        }
        if (alpha > 0) {
            gradientDrawable.color = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                    Color.parseColor(alphaColor), alpha.toInt()
                )
            )
        }
        if (strokeSize > 0) {
            gradientDrawable.setStroke(
                strokeSize,
                ColorStateList.valueOf(Color.parseColor(strokeColor))
            )
        }
        if (isRounded) {
            gradientDrawable.cornerRadius = corner.toFloat()
        }
        invalidate()
    }

    /**
     * @param view        button view of xml
     * @param startColor  button start color
     * @param startColor  button end color
     * @param strokeSize  outline size
     * @param strokeColor outline color
     * @param isRounded   set rounded corner if true
     * @param corner      corner radius
     */
    fun setGradientBackgroundDrawable(
        view: CustomLinearLayout,
        startColor: String?,
        endColor: String?,
        strokeSize: Int,
        strokeColor: String?,
        isRounded: Boolean,
        corner: Int
    ) {
        val colors = intArrayOf(Color.parseColor(startColor), Color.parseColor(endColor))
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors)
        if (strokeSize > 0) {
            gradientDrawable.setStroke(
                strokeSize,
                ColorStateList.valueOf(Color.parseColor(strokeColor))
            )
        }
        if (isRounded) {
            gradientDrawable.cornerRadius = corner.toFloat()
        }
        view.background = gradientDrawable
    }
}