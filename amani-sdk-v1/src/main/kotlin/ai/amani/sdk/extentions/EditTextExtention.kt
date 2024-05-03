package ai.amani.sdk.extentions

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.widget.EditText
import android.widget.LinearLayout
import com.hbb20.CountryCodePicker

/**
 * @Author: @zekiamani
 * @Date: 23.02.2024
 */

fun EditText.customStroke(stroke: Int, color: String, cornerRadius: Int) {
    try {
        val drawable = (this.background.mutate() as GradientDrawable)
        drawable.cornerRadius = cornerRadius.toFloat()
        drawable.setStroke(stroke, ColorStateList.valueOf(Color.parseColor(color)))
        this.background = drawable
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun LinearLayout.customStroke(stroke: Int, color: String, cornerRadius: Int) {
    try {
        val drawable = (this.background.mutate() as GradientDrawable)
        drawable.cornerRadius = cornerRadius.toFloat()
        drawable.setStroke(stroke, ColorStateList.valueOf(Color.parseColor(color)))
        this.background = drawable
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun CountryCodePicker.customStroke(stroke: Int, color: String, cornerRadius: Int) {
    try {
        val drawable = (this.background.mutate() as GradientDrawable)
        val radii = floatArrayOf(
            cornerRadius.toFloat(), cornerRadius.toFloat(),  // Top-left corner
            0f, 0f,                      // Top-right corner
            0f, 0f,                      // Bottom-right corner
            cornerRadius.toFloat(), cornerRadius.toFloat()   // Bottom-left corner
        )
        drawable.cornerRadii = radii
        drawable.color = ColorStateList.valueOf(Color.parseColor(color))
        drawable.setStroke(stroke, ColorStateList.valueOf(Color.parseColor(color)))
        this.background = drawable
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

