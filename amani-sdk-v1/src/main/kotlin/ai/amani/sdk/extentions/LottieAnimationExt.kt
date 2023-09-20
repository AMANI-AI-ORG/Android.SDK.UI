package ai.amani.sdk.extentions

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath

/**
 * @Author: zekiamani
 * @Date: 29.09.2022
 */

fun LottieAnimationView.setColor(
    color: String?
) {
    if (color == null) return
    this.addValueCallback(
        KeyPath("**"),
        LottieProperty.COLOR_FILTER
    ) { PorterDuffColorFilter(
        Color.parseColor(color),
        PorterDuff.Mode.SRC_ATOP
    )}
}