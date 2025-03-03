package ai.amani.sdk.presentation.common

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils

class CustomTextView : AppCompatTextView {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    /**
     * @param view        button view of xml
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
        view: View,
        drawable: Drawable?,
        solidColor: String?,
        strokeSize: Int,
        strokeColor: String?,
        alpha: Int,
        alphaColor: String?,
        isRounded: Boolean,
        corner: Int
    ) {
        this.background = drawable
        val gradientDrawable = view.background as GradientDrawable
        if (!TextUtils.isEmpty(solidColor)) {
            gradientDrawable.color = ColorStateList.valueOf(Color.parseColor(solidColor))
        }
        if (alpha > 0) {
            gradientDrawable.color = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                    Color.parseColor(alphaColor), alpha
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
     * set text and his color
     *
     * @param text      text
     * @param textColor textcolor
     */
    fun setTextProperty(text: String?, textColor: String?) {
        if (text.isNullOrBlank()) return
        this.text = text
        this.setTextColor(Color.parseColor(textColor))
    }

    /**
     * @param drawable drawable
     * @param gravity  gravity i.e can TOP BOTTOM START END
     * @param padding  padding
     */
    fun setCompoundDrawable(drawable: Int, gravity: Int, padding: Int, visible: Boolean) {
        if (visible) {
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
        } else {
            this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
        this.compoundDrawablePadding = padding
        invalidate()
    }

    /**
     * Setting textSize of the text views.
     * @param textSize
     */
    override fun setTextSize(textSize: Float) {
        this.textSize = textSize
    }
}