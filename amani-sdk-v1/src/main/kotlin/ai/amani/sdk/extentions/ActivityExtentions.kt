package ai.amani.sdk.extentions

import android.R
import android.graphics.Color
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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