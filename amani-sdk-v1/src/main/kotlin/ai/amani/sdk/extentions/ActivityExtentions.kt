package ai.amani.sdk.extentions

import android.graphics.Color
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentContainerView
import timber.log.Timber


/**
 * @Author: @zekiamani
 * @Date: 23.02.2024
 */

fun AppCompatActivity.setActionBarColor(color: String) {
    try {
        val window: Window = this.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor(color)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

internal fun AppCompatActivity.setScreenEdgePaddings(container: FragmentContainerView) {
    //If edge to edge will be using by app && and give padding for system bars
    try {
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )
            insets
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}