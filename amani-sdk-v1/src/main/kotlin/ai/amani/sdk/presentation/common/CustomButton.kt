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
     * @param view        button view of xml
     * @param startColor  button start color
     * @param startColor  button end color
     * @param strokeSize  outline size
     * @param strokeColor outline color
     * @param isRounded   set rounded corner if true
     * @param corner      corner radius
     */
    fun setGradientBackgroundDrawable(
        view: AmaniButton,
        startColor: String,
        endColor: String,
        strokeSize: Int,
        strokeColor: String?,
        isAlpha: Boolean,
        isRounded: Boolean,
        corner: Int
    ) {
        val colors = IntArray(2)
        if (isAlpha) {
            colors[0] = Color.parseColor("#80" + startColor.substring(1))
            colors[1] = Color.parseColor("#80" + endColor.substring(1))
        } else {
            colors[0] = Color.parseColor(startColor)
            colors[1] = Color.parseColor(endColor)
        }
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

    /**
     * set text and his color
     *
     * @param text      text
     * @param textColor opaque textcolor
     */
    fun setOpaqueTextProperty(text: String?, textColor: String) {
        val color = "#80" + textColor.substring(1)
        this.text = text
        this.setTextColor(Color.parseColor(color))
    }

    /**
     * @param drawable drawable
     * @param gravity  gravity i.e can TOP BOTTOM START END
     * @param padding  padding
     */
    fun setCompoundDrawable(drawable: Int, gravity: Int, padding: Int) {
        when (gravity) {
            Gravity.TOP -> this.setCompoundDrawablesWithIntrinsicBounds(
                null, ResourcesCompat.getDrawable(
                    resources, drawable, null
                ), null, null
            )
            Gravity.BOTTOM -> this.setCompoundDrawablesWithIntrinsicBounds(
                null, null, null, ResourcesCompat.getDrawable(
                    resources, drawable, null
                )
            )
            Gravity.END -> this.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ResourcesCompat.getDrawable(
                    resources, drawable, null
                ), null
            )
            Gravity.START -> this.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(
                    resources, drawable, null
                ), null, null, null
            )
        }
        this.compoundDrawablePadding = padding
        invalidate()
    }

    /**
     * set app default button property
     *
     */
    fun setDefaultLinearParam() {
        this.isAllCaps = false
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        this.gravity = Gravity.START or Gravity.CENTER
        setPadding(40, 0, 0, 0)
        layoutParams.setMargins(60, 60, 60, 0)
    }

    fun setWeight(weight: Int?) {
        var layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            weight!!.toFloat()
        )
        layoutParams = layoutParams
    }
}