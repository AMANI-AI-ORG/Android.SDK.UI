package ai.amani.sdk.presentation.common

import androidx.appcompat.widget.AppCompatButton
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.content.res.ColorStateList
import ai.amani.base.widget.AmaniButton
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import android.widget.LinearLayout
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils

class CustomButton : AppCompatButton {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {}

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
        solidColor: String,
        strokeSize: Int,
        strokeColor: String?,
        alpha: Float?,
        alphaColor: String?,
        isRounded: Boolean?,
        corner: Int
    ) {
        this.background = drawable
        val gradientDrawable = this.background as GradientDrawable
        if (!TextUtils.isEmpty(solidColor)) {
            gradientDrawable.color = ColorStateList.valueOf(Color.parseColor(solidColor))
        }
        if (alpha != null) {
            if (alpha > 0F) {
                gradientDrawable.color = ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(
                        Color.parseColor(alphaColor), alpha.toInt()
                    )
                )
            }
        }
        if (strokeSize > 0) {
            gradientDrawable.setStroke(
                strokeSize,
                ColorStateList.valueOf(Color.parseColor(strokeColor))
            )
        }
        if (alpha != null) {
            if (solidColor == alphaColor && alpha > 0) {
                gradientDrawable.setStroke(
                    strokeSize, ColorStateList.valueOf(
                        ColorUtils.setAlphaComponent(
                            Color.parseColor(alphaColor), alpha.toInt()
                        )
                    )
                )
            }
        }
        if (isRounded == true) {
            gradientDrawable.cornerRadius = corner.toFloat()
        }
        invalidate()
    }

    /**
     * set text and his color
     *
     * @param text      text
     * @param textColor textcolor
     */
    fun setTextProperty(text: String?, textColor: String?) {
        this.text = text
        this.setTextColor(Color.parseColor(textColor))
    }

}