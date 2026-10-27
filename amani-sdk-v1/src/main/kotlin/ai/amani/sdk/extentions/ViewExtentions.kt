package ai.amani.sdk.extentions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import datamanager.model.config.GeneralConfigs

/**
 * @Author: zekiamani
 * @Date: 20.09.2022
 */

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.show(show: Boolean) {
    if (show) this.visibility = View.VISIBLE
    else this.visibility = View.INVISIBLE
}

fun View.hide() {
    this.visibility = View.INVISIBLE
}

/**
 * Sets the visibility of the "powered by Amani" bottom logo. When the Core SDK config sets
 * `hideLogo` (or is not loaded yet) the logo is GONE, so the screen content takes its space.
 * Otherwise it follows [visible]; INVISIBLE keeps the layout steady while the keyboard is open.
 */
internal fun View.showAmaniLogo(generalConfigs: GeneralConfigs?, visible: Boolean = true) {
    visibility = when {
        generalConfigs == null || generalConfigs.hideLogo -> View.GONE
        visible -> View.VISIBLE
        else -> View.INVISIBLE
    }
}

fun View.gone() {
    this.visibility = View.INVISIBLE
}

fun View.remove() {
    this.visibility = View.GONE
}

fun View.slideLeft() {
    this.animate()
        .translationX(-this.width.toFloat())
        .alpha(0.0f)
        .setDuration(300)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                clearAnimation()
                visibility = View.GONE
            }
        })
}

fun View.slideLeft(
    onCompleted: () -> Unit
) {
    this.animate()
        .translationX(-this.width.toFloat())
        .alpha(0.0f)
        .setDuration(300)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                clearAnimation()
                onCompleted.invoke()
            }
        })
}

fun View.setCustomBackground(@DrawableRes drawable: Int, @ColorRes color: Int?  = null) {
    val buttonBackground = ContextCompat.getDrawable(this.context, drawable)?.mutate()

    if (color != null) {
        if (buttonBackground is GradientDrawable){
            buttonBackground.setStroke(2,
                ContextCompat.getColor(
                    this.context,
                    color)
            )
        }
    }

    this.background = buttonBackground
}

fun Button.setBackground(@ColorRes color: Int) {
    this.setBackgroundColor(ContextCompat.getColor(this.context, color))
}



