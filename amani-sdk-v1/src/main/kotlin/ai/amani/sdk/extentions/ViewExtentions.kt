package ai.amani.sdk.extentions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

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

fun View.gone() {
    this.visibility = View.INVISIBLE
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

